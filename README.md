# spend-management-api

Backend API for corporate spend management. Models real-world patterns from payment processing systems including idempotent transaction handling, real-time spend aggregation, configurable pricing rules, and feature flag-controlled rollouts.

## Architecture

```
Client
  │
  ▼
Spring Boot API (port 8080)
  ├── POST /transactions ──► SpendLimitService ──► Redis  (atomic spend counters per card/period)
  │                      └─► PricingRuleService ──► Redis  (cached pricing rules, 10-min TTL)
  │                      └─► PostgreSQL          (transaction persistence, idempotency)
  │                      └─► Kafka "transactions" topic
  │
  ├── GET/PUT /cards/{id}/limits
  ├── POST/GET /rules
  ├── POST/GET /flags
  └── GET /teams/{id}/billing
          │
          ▼
    Kafka Consumer (BillingEventConsumer)
          │
          ▼
    BillingRecord (PostgreSQL)
    grouped by team + billing period + merchant category
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 17, Spring Boot 3.2 |
| Database | PostgreSQL 16 + Flyway migrations |
| Cache | Redis 7 (spend aggregation + rule cache) |
| Messaging | Apache Kafka (transaction events, billing consumer) |
| Testing | JUnit 5 + Testcontainers |
| Build | Maven |

## Running Locally

### Prerequisites
- Docker + Docker Compose
- Java 17 + Maven (for building)

### Start all infrastructure + API

```bash
# Build the JAR first
mvn clean package -DskipTests

# Start everything
docker-compose up -d

# Check health
curl http://localhost:8080/actuator/health
```

### Run tests

```bash
# Requires Docker (Testcontainers spins up Postgres, Redis, Kafka)
mvn test
```

## Example API Flows

### 1. Create a card with spending limits

```bash
# Create card
curl -X POST http://localhost:8080/cards \
  -H "Content-Type: application/json" \
  -d '{
    "teamId": "11111111-0000-0000-0000-000000000000",
    "holderId": "22222222-0000-0000-0000-000000000000",
    "status": "ACTIVE",
    "currency": "EUR"
  }'

# Set limits: €200 per transaction, €1000 daily
curl -X PUT http://localhost:8080/cards/{cardId}/limits \
  -H "Content-Type: application/json" \
  -d '[
    {"limitType": "PER_TRANSACTION", "amount": 200.00, "currency": "EUR"},
    {"limitType": "DAILY", "amount": 1000.00, "currency": "EUR"}
  ]'
```

### 2. Approve a transaction

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "cardId": "{cardId}",
    "amount": 150.00,
    "currency": "EUR",
    "merchantCategory": "TRAVEL",
    "idempotencyKey": "order-abc-123"
  }'

# Response: {"status": "APPROVED", "id": "...", "appliedMarkup": null}
```

### 3. Transaction declined — daily limit exceeded

After cumulative approved spend reaches €1000, the next transaction is declined:

```bash
# Returns 201 with status DECLINED
# {"status": "DECLINED", "declineReason": "LIMIT_EXCEEDED"}
```

### 4. Update spending limit and immediately retry

```bash
# Raise daily limit to €2000
curl -X PUT http://localhost:8080/cards/{cardId}/limits \
  -H "Content-Type: application/json" \
  -d '[{"limitType": "DAILY", "amount": 2000.00, "currency": "EUR"}]'

# Retry — limit check reads from Redis immediately, no cache TTL delay
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"cardId": "...", "amount": 150.00, "currency": "EUR", "idempotencyKey": "order-abc-124"}'
# Response: {"status": "APPROVED"}
```

### 5. Enable pricing markup for travel merchants

```bash
# Enable the pricing-v2 feature flag globally
curl -X POST http://localhost:8080/flags \
  -H "Content-Type: application/json" \
  -d '{"flagKey": "pricing-v2", "enabled": true, "rolloutPercentage": 100}'

# Create a 1.5% markup rule for TRAVEL category
curl -X POST http://localhost:8080/rules \
  -H "Content-Type: application/json" \
  -d '{
    "merchantCategory": "TRAVEL",
    "markupPercentage": 0.0150,
    "effectiveFrom": "2026-01-01T00:00:00Z"
  }'

# Next TRAVEL transaction will include appliedMarkup: 0.0150
```

### 6. Check billing summary

```bash
curl http://localhost:8080/teams/{teamId}/billing?period=2026-04
# Returns total spend + line items per merchant category
```

## Design Decisions

### Redis for real-time spend aggregation
Spend limits are checked on every transaction request. Using PostgreSQL for this would require a `SUM` query on the transactions table per card per period under write contention — unacceptably slow at scale. Redis `INCRBY`/`DECRBY` operations are atomic and O(1), enabling sub-millisecond limit checks. Amounts are stored as integer micros (4 decimal places) to avoid floating-point precision issues.

### Idempotency key pattern
Payment systems require exactly-once semantics. A client network timeout leaves the client uncertain whether a transaction was processed. Sending the same `idempotencyKey` again returns the original response without re-executing the transaction. This is stored as a unique constraint on the `transactions` table — the database enforces uniqueness, not application logic.

### Feature flags for pricing rollout
Pricing engine changes carry financial risk. Wrapping the new engine behind `pricing-v2` flag allows:
- **Percentage rollout**: gradually expose the new pricing to a fraction of cards
- **Targeted rollout**: test on specific `entityIds` before full rollout
- **Instant kill switch**: set `enabled: false` to revert to zero markup in one API call
The hash-based bucketing (`hash(flagKey + entityId) % 100`) ensures stable, consistent assignment per entity.

### Kafka for billing aggregation
Billing consumers are decoupled from the transaction request path. If the billing consumer is slow, backs up, or crashes, transaction processing is unaffected. The consumer uses manual acknowledgment (`MANUAL_IMMEDIATE`) so a billing failure doesn't drop the event — it retries on restart.
