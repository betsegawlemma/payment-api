# Payment API

This project is a robust, event-driven payment processing API built with Java (Spring Boot 3, Java 21+), leveraging PostgreSQL 17, RabbitMQ, and Redis. It follows best practices for idempotency, transactional outbox/eventing, global rate limiting, and extensibility for payment provider integration.

---

## Architecture Overview

- **Spring Boot 3 (Java 21+)**: Main application framework.
- **PostgreSQL 17**: Persistent storage for payments, idempotency keys, and outbox events.
- **RabbitMQ**: Durable queue for payment order processing and a fanout exchange for domain events.
- **Redis**: Implements a distributed token-bucket algorithm for global 2 TPS rate limiting.
- **Outbox Pattern**: Ensures reliable event publication after payment terminal state.
- **Extensible Provider Adapter**: Easily integrates new payment providers.

---

## Main Components

- **`/src/main/java/org/kifiya/paymentapi`**: Core application code.
  - **Controllers**: Expose REST endpoints for payment creation and status fetch.
  - **Workers**: Listen on RabbitMQ queue, process payments with retry and rate limiting.
  - **Repositories/Entities**: Models for payments, idempotency, outbox.
  - **Adapters**: Pluggable integration with external payment providers.
  - **Rate Limiter**: Redis-backed, guarantees max 2 TPS globally.
  - **Outbox Publisher**: Publishes payment events to RabbitMQ exchange.
- **Configuration**:
  - **`application.properties`**: DB, Redis, RabbitMQ, and provider endpoint config.
  - **`docker-compose.yml`**: Spins up PostgreSQL, Redis, and RabbitMQ for local development.
  - **`build.gradle.kts`**: Gradle Kotlin DSL for dependency management.
- **API Endpoints**:
  - `POST /payments` — Create a payment (with idempotency enforcement).
  - `GET /payments/{orderId}` — Check payment status.

---

## Step-by-Step Setup & Run Instructions

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle (or use provided Gradle Wrapper)

---

### 1. Clone & Build

```bash
git clone https://github.com/betsegawlemma/payment-api.git
cd payment-api
./gradlew build
```

---

### 2. Start the Application and Dependencies

The easiest way to run everything is with Docker Compose:

```bash
docker compose up --build
```

- This will launch the application alongside PostgreSQL, Redis, and RabbitMQ.
- **PostgreSQL:** `localhost:5432` (`payment_user` / `payment_pass` / `payment_db`)
- **Redis:** `localhost:6379`
- **RabbitMQ:** `localhost:5672` (UI: [http://localhost:15672](http://localhost:15672), user: guest/guest)
- **API:** `localhost:8080`

Alternatively, if you want to start only the dependencies and run the application locally:

```bash
docker compose up -d   # Start dependencies only
./gradlew build        # Build the app
./gradlew bootRun      # Run the app
```

---

### 3. Test the API

#### Create Payment

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-idem-key-1" \
  -d '{
    "orderId": "order-001",
    "amount": { "value": "100.00", "currency": "USD" },
    "paymentMethod": {
      "type": "CREDIT_CARD",
      "cardNumber": "4242424242424242",
      "expirationMonth": "12",
      "expirationYear": "25",
      "cvv": "123",
      "cardHolderName": "John Doe"
    },
    "customerId": "customer-xyz",
    "description": "Test purchase",
    "callbackUrl": "https://client.com/payment-status-webhook",
    "idempotencyKey": "unique-idem-key-1"
  }'
```

#### Check Payment Status

```bash
curl http://localhost:8080/payments/order-001
```

---

### 4. Simulate External Payment Provider

- Start your payment provider simulator at `http://localhost:8081/api/payments` (or update `payments.provider.url` in `application.properties`).

---

### 5. Scaling & Events

- Run more app instances for horizontal scaling; rate limit is enforced globally via Redis.
- Events for terminal payment states are published to RabbitMQ's event exchange, enabling reliable downstream processing.

---

### 6. Notes

- **Idempotency**: All payment requests must include a unique `Idempotency-Key` header.
- **Rate Limiting**: System-wide, never exceeds 2 payment requests/sec to the provider.
- **Extensibility**: Add new payment provider adapters by implementing the `ProviderAdapter` interface.
- **Transactional Outbox**: Terminal status changes and event emission are atomic.

---

## License

MIT License