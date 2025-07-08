# Payment API

The Payment API application is designed to address the requirements and architectural challenges outlined in the Kifiya — Payment-Processing Challenge document. It leverages various Spring Boot features, message queues, and a distributed rate limiter to create a resilient and extensible payment processing gateway. The External Payment Simulator API serves as the mock external payment provider that the Payment API interacts with, simulating its throughput limits, latency, and transient failures.

Here's a detailed explanation of how the Payment API application meets the challenge's criteria:

## 1. Core Functional Requirements

### Ingest Payment Orders
- The service exposes a REST API endpoint for accepting payment orders. The `PaymentController` handles incoming POST requests to `/payments`.
- It expects a `PaymentRequest` in the request body, which includes details like `orderId`, `amount`, `paymentMethod`, `customerId`, `description`, and `callbackUrl`.
- It also requires an `Idempotency-Key` header for duplicate detection.
- Upon successful ingestion, it returns an HTTP 202 Accepted response with a status of "PENDING" to indicate that the payment has been queued for processing.

### Guarantee At-Least-Once Delivery
- **Durability and Persistence:** Payment orders are immediately persisted to a PostgreSQL database via `PaymentOrderRepository` after ingestion, ensuring pending payments are not lost if the service restarts.
- **Asynchronous Processing with Message Queue:** Instead of direct processing, the payment order's ID is enqueued into a RabbitMQ queue (`payments-queue`). This decouples the ingestion from the actual processing, adding durability and allowing for retries and asynchronous handling.
- **PaymentOrderWorker:** A `PaymentOrderWorker` listens to this queue and submits the processing task to a `paymentTaskExecutor` thread pool, ensuring that even if the worker or processor fails, the message remains in the queue for later redelivery.
- **Intelligent Retries:** The `PaymentOrderProcessor` implements exponential backoff retries for transient errors and timeouts returned by the external provider. This ensures that an accepted payment order is eventually delivered and processed.

### Adhere to Global Rate Limits
- The challenge specifies a strict global maximum throughput of 2 transactions per second (2 TPS).
- The Payment API uses a `TokenBucketRateLimiter`, which is configured to operate globally.
- This global rate limiter is implemented using **Redisson**, a Redis client. Redisson provides distributed rate limiting capabilities, crucial for maintaining the global limit even with multiple instances of the Payment API running.
- The `TokenBucketRateLimiter` is initialized with a rate of 2 permits per second.
- Before calling the external payment provider, the `PaymentOrderProcessor` calls `rateLimiter.tryAcquire()`. If a permit is not immediately available, it pauses (sleeps for 100ms) and retries acquiring the permit, ensuring the global limit is respected.

### Detect and Reject Duplicates
- **Idempotent Design:** The Payment API implements idempotency using an `IdempotencyKey` entity stored in the database.
- When a `createPayment` request is received, it expects an `Idempotency-Key` header.
- The `PaymentController` first checks the `IdempotencyKeyRepository` if an entry with this key already exists.
- If an existing key is found, it returns an HTTP 409 Conflict response with a "Duplicate idempotency key" message, preventing reprocessing of the same order.
- If the key is new, it's saved along with the `paymentId` immediately after saving the `PaymentOrder`, ensuring durability across restarts.

### Implement Intelligent Retries
- The `PaymentOrderProcessor` contains the core retry logic.
- When the `ExternalPaymentProviderAdapter` receives an "ERROR" or "TIMEOUT" status from the external provider, `ProviderStatus.isRetryable()` returns true.
- The `retryExponential` method is then invoked, attempting to process the payment up to `MAX_RETRIES` (5 times).
- It uses an exponential backoff strategy with a `BASE_DELAY_MS` of 1 second, meaning the delay between retries increases exponentially (1s, 2s, 4s, 8s, 16s).
- Crucially, each retry attempt still respects the global rate limit by calling `rateLimiter.tryAcquire()` before invoking the provider.

### Expose Payment Status
- A `GET /payments/{orderId}` API endpoint is provided by the `PaymentController`.
- Client systems can query the status of a payment by its `orderId`.
- The endpoint retrieves the `PaymentOrder` from the database and returns a `PaymentResponse` containing the `orderId`, current status (`PENDING`, `COMPLETED`, `FAILED`), and a `providerMessage`.

### Facilitate Provider Extensibility
- The architecture incorporates the Adapter design pattern to abstract the external payment provider interaction.
- A `ProviderAdapter` interface defines the contract for processing payments (`processPayment` method).
- The `ExternalPaymentProviderAdapter` is the concrete implementation that interacts with the specific "External Payment Simulator API" via `RestTemplate`.
- To add a new payment provider, a new class implementing the `ProviderAdapter` interface would be created, without requiring changes to the core `PaymentOrderProcessor` logic.

### Emit Domain Events
- The Payment API uses the Transactional Outbox Pattern to reliably emit domain events.
- An `OutboxEvent` entity is used to store events (`PAYMENT_COMPLETED`, `PAYMENT_FAILED`) in the same database transaction as the `PaymentOrder` status update.
- The `PaymentOutboxHelper` ensures that the `PaymentOrder` status update and the `OutboxEvent` creation are part of a `REQUIRES_NEW` transaction, ensuring atomicity.
- An `OutboxEventPublisher` then periodically queries for unsent events (`findBySentFalse()`) and publishes them to a RabbitMQ exchange (`payments-events`) using `rabbitTemplate.convertAndSend()`.
- Once an event is successfully sent, its `sent` flag is updated to true. This guarantees that events are emitted reliably ("at-least-once") and are not lost even if the application crashes between updating the payment status and publishing the event.

## 2. Key Architectural Challenges to Address

### Concurrency and Rate Limiting
- **Global Rate Limiting:** As detailed above, the `TokenBucketRateLimiter` backed by Redisson (Redis) provides a distributed and global 2 TPS limit, effectively handling scenarios with multiple running instances of the service.
- **Asynchronous Processing:** Payment processing is moved off the request thread using RabbitMQ for queuing and a `ThreadPoolTaskExecutor` (`paymentTaskExecutor`) for worker processing. This prevents the API from being blocked by long-running or retrying payment operations, improving responsiveness and concurrency.
- **Race Conditions:** The idempotency key mechanism prevents race conditions stemming from duplicate requests. The synchronization within the `TokenBucketRateLimiter` (though it's a distributed lock in Redisson) helps manage concurrent access to rate limiting logic. Transactional boundaries around payment processing (`@Transactional` on `processOrder` and `updateAndOutbox`) help maintain data consistency.

### State Management and Durability
- **Persistent Storage:** The PostgreSQL database is the durable heart of the system for state management.
- **PaymentOrder Entity:** The `PaymentOrder` entity stores the entire lifecycle state of a payment (`PENDING`, `COMPLETED`, `FAILED`, `providerMessage`, `createdAt`, `updatedAt`).
- **Idempotency Key Management:** `IdempotencyKey` entities are stored in the database, mapping the `idemKey` to the `paymentId`. This key is saved in the same transaction as the initial `PaymentOrder`, ensuring that the idempotency check survives service restarts and remains consistent.
- **Outbox Pattern for Events:** The `OutboxEvent` table acts as a reliable temporary store for domain events, ensuring they are not lost before being published.

### Decoupling and Extensibility
- **Provider Extensibility:** The `ProviderAdapter` interface precisely addresses the requirement for easy addition of new payment providers, embodying the Adapter pattern. This keeps the core payment processing logic decoupled from specific provider implementations.
- **Asynchronous Messaging:** The use of RabbitMQ for queueing payment orders and emitting domain events significantly decouples the ingestion API from the processing workers and downstream event consumers. This allows independent scaling and evolution of different parts of the system.
- **Separation of Concerns:** Spring's dependency injection and component scanning (`@Service`, `@Component`, `@RestController`) facilitate a clear separation of concerns (e.g., `PaymentController` for API, `PaymentOrderProcessor` for business logic, `ExternalPaymentProviderAdapter` for external interaction, `OutboxEventPublisher` for event emission).

### Reliability and Failure Modes
- **Database Failure:** If the database is unavailable, new payment orders cannot be saved, and the service would respond with errors. Existing queued payments would remain in RabbitMQ, and the processing workers (`PaymentOrderProcessor`) would retry database operations (though this is implicitly handled by Spring Data JPA transaction management rather than explicit retry code in the processor itself). The Transactional Outbox Pattern ensures events are only persisted if the payment state is successfully updated.
- **Message Queue Failure (RabbitMQ):** If RabbitMQ is down, new payment orders cannot be enqueued, and the `createPayment` endpoint would fail. The `OutboxEventPublisher` would also fail to publish events. However, already enqueued messages are durable, and processing would resume once RabbitMQ recovers. The Outbox pattern ensures that events waiting to be published are persisted in the database and will be picked up by the `OutboxEventPublisher` once RabbitMQ is available again.
- **External Provider Failure:**
    - **Transient Errors/Timeouts:** Handled by the intelligent retry mechanism with exponential backoff, as well as the continuous `tryAcquire` loop for rate limiting. The External Payment Simulator is designed to specifically simulate these scenarios.
    - **Consistent Failures:** While a full Circuit Breaker pattern (an "Area for Exploration") is not explicitly implemented in the provided Payment API code, the retry logic limits repeated attempts.

### At-Least-Once Semantics
- **Provider Delivery:** Achieved through persistent queues (RabbitMQ) and the intelligent retry logic within `PaymentOrderProcessor`. A payment order's processing will be retried until it succeeds or reaches max retries, ensuring it's delivered to the provider at least once.
- **Event Publication:** Achieved by the Transactional Outbox Pattern. Events are first written to the database (in the same transaction as the payment status update), and then a separate publisher sends them to RabbitMQ. If publishing fails, the event remains `sent=false` and will be retried later, guaranteeing at-least-once event publication.

## 3. Areas for Exploration

The Payment API application goes beyond the core requirements by implementing several "Areas for Exploration":

- **Advanced Rate Limiting:** The `TokenBucketRateLimiter` utilizing Redisson for distributed rate limiting is an advanced implementation compared to a simple in-memory counter.
- **Database Integration:** A containerized PostgreSQL database is integrated for all state management, including `PaymentOrder`, `IdempotencyKey`, and `OutboxEvent`, satisfying the durability and persistence requirements.
- **Persistent Queues:** RabbitMQ is integrated as a production-grade message broker to manage the payment processing queue and the events exchange, enabling asynchronous and resilient processing.
- **Transactional Outbox Pattern:** The system explicitly implements the Transactional Outbox Pattern with `OutboxEvent`, `OutboxEventRepository`, `OutboxEventPublisher`, and `PaymentOutboxHelper` to ensure reliable domain event emission.