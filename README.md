# Step-by-Step Setup & Run Instructions

## Prerequisites

- Java 21+
- Docker and Docker Compose
- Gradle (or use the Gradle Wrapper)

---

## 1. Clone & Build

```bash
git clone <repo-url>
cd <repo-folder>
./gradlew build
```

---

## 2. Start Infrastructure (Postgres, Redis, RabbitMQ)

```bash
docker-compose up -d
```

- **PostgreSQL:** localhost:5432 (user: payment_user, pass: payment_pass, db: payment_db)
- **Redis:** localhost:6379
- **RabbitMQ:** localhost:5672 (UI at http://localhost:15672, guest/guest)

---

## 3. Run the Spring Boot Application

```bash
./gradlew bootRun
```
or
```bash
java -jar build/libs/payments-0.0.1-SNAPSHOT.jar
```

---

## 4. Test API Endpoints

### Create Payment (`POST /payments`)

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

### Get Payment Status (`GET /payments/{orderId}`)

```bash
curl http://localhost:8080/payments/order-001
```

---

## 5. Simulate External Payment Provider

- Run your existing simulated provider at `http://localhost:8081/api/payments`.
- You may change the provider URL via `application.properties` (`payments.provider.url`).

---

## 6. Scaling & Events

- **RabbitMQ queue**: Durable, supports multiple worker instances.
- **Events Exchange**: All terminal state changes (COMPLETED/FAILED) are published here.
- Add more Spring Boot app instances to scale worker/payment processing.

---

## 7. Rate Limit

- The Redis-backed bucket ensures a max of 2 payment requests per second **across the whole system**.

---

## 8. Customize

- Add more payment providers by implementing `ProviderAdapter`.
- Extend outbox payloads/events as needed.

---

**You now have a robust, scalable, event-driven payment processing Proof-of-Concept!**# payment-api
