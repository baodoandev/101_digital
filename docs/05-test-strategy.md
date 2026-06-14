# Test Strategy

## 1. Overview

The platform is tested at three levels without any frontend application:

1. **Automated tests** (JUnit 5 + Testcontainers) — unit and integration tests run via Maven
2. **Demo script** (`test/demo.sh`) — bash script exercising the full API against a running instance
3. **Postman collection** (`test/coffee-platform.postman_collection.json`) — importable into Postman for manual or automated testing

## 2. Automated Tests

### Test Infrastructure

| Component | Technology |
|-----------|-----------|
| Framework | JUnit 5 (Spring Boot Test) |
| HTTP client | REST Assured |
| Database | Testcontainers with PostGIS (`postgis/postgis:16-3.4`) |
| Clock | Fixed clock (`2025-01-15T04:00:00Z`) for deterministic time-dependent tests |

All integration tests extend `AbstractIT`, which starts a single PostGIS container shared across the entire test suite (singleton pattern). Tests run against a real PostgreSQL database with the same Liquibase migrations used in production.

### Running

```bash
./mvnw test
```

### Test Inventory

| Test Class | Type | Tests | What It Covers |
|-----------|------|-------|----------------|
| `ApplicationBootIT` | Integration | 1 | Schema creation (9 tables verified), application context loads |
| `AuthFlowIT` | Integration | 6 | Owner register/login/me, customer register/login/me, duplicate email, invalid credentials, validation errors, unauthenticated access |
| `JwtServiceTest` | Unit | 4 | Token generation (USER + CUSTOMER), expired token rejection, invalid token rejection |
| `ShopDataIT` | Integration | 7 | Shop CRUD, menu items, duplicate menu item, queue creation, max-3 queue limit, nearest-shop geospatial query, operator assignment, forbidden access |
| `OpeningHoursTest` | Unit | 7 | Normal hours, boundary checks, midnight-wrapping hours, same open/close = always open |
| `OrderFlowIT` | Integration | 8 | Place order, get order, queue position + ETA, cancel order, cancel-already-cancelled, idempotency key, shop-closed, queue-full, cross-customer forbidden, order history |
| `QueueSelectorTest` | Unit | 5 | Single queue, least-loaded selection, all queues full, no queues, inactive queue skipped |
| `ServeFlowIT` | Integration | 5 | Serve-next FIFO, serve-specific, loyalty score increment, serve empty queue, queue snapshot |
| `ServeConcurrencyIT` | Integration | 1 | Concurrent serve-next with 2 threads × 3 serves — no double-serving |
| **Total** | | **44+** | |

### Test Categories

- **Unit tests** (3 classes): No Spring context, no database. Test pure logic in isolation (JWT, opening hours, queue selection).
- **Integration tests** (7 classes): Full Spring Boot context with real PostGIS database via Testcontainers. Tests exercise the entire stack: HTTP request → controller → service → repository → database.

## 3. Demo Script

### Running

```bash
# Against local Docker Compose
./test/demo.sh

# Against a custom URL (e.g., AWS deployment)
./test/demo.sh https://your-server.example.com
```

### What It Tests

The script runs 18 sequential steps with color-coded pass/fail output:

**Happy Path (Steps 0–10):**

| Step | Action | Validates |
|------|--------|-----------|
| 0 | Health check | `/actuator/health` returns UP |
| 1 | Register owner | Token returned |
| 2 | Create shop | Shop created with always-open hours |
| 3 | Add menu items | 2 items (Flat White + Croissant) |
| 4 | Create queue | Queue with maxSize=5 |
| 5 | Register customer | Customer token returned |
| 6 | Place order | Status = WAITING, total calculated correctly |
| 7 | Queue position | Position and ETA returned |
| 8 | Serve next | Entry status = SERVED |
| 9 | Verify fulfilled | Order status = FULFILLED |
| 10 | Order history | Customer's orders listed |

**Error Handling (Steps 11–18):**

| Step | Scenario | Expected |
|------|----------|----------|
| 11 | Order when shop closed | 409 / SHOP_CLOSED |
| 12 | Order when queue full | 409 / QUEUE_FULL |
| 13 | Cancel fulfilled order | 409 / INVALID_STATE |
| 14 | Duplicate email registration | 409 / EMAIL_TAKEN |
| 15 | Invalid login credentials | 401 |
| 16 | Access without token | 401 |
| 17 | Access another customer's order | 403 |
| 18 | Validation error (empty body) | 400 / VALIDATION_ERROR |

### Output

The script exits with code 0 if all steps pass, or 1 if any fail. Example output:

```
[PASS] Step  0: Health check (200)
[PASS] Step  1: Register owner (201)
...
[PASS] Step 18: Validation error (400)

Results: 19 passed, 0 failed
```

## 4. Postman Collection

### Setup

1. Import `test/coffee-platform.postman_collection.json` into Postman
2. Set the `baseUrl` collection variable (default: `http://localhost:8080`)
3. Run requests sequentially — test scripts auto-extract tokens and IDs into variables

### Folders

| Folder | Requests | Coverage |
|--------|----------|----------|
| Auth | 5 | Register owner/customer, login owner/customer, get me |
| Shop | 10 | Create shop, list/get shops, nearest shops, menu CRUD, queue CRUD, assign operator |
| Order | 5 | Place order, get order, queue position, cancel order, order history |
| Queue Ops | 4 | Queue snapshot, queue entries, serve next, serve specific |

### Automated Running

```bash
# With Newman (Postman CLI)
npx newman run test/coffee-platform.postman_collection.json \
  --env-var "baseUrl=http://localhost:8080"
```

## 5. Requirements Traceability

| Customer App Requirement | Automated Test | Demo Script Step | Postman |
|--------------------------|---------------|------------------|---------|
| Register with mobile + name | AuthFlowIT | Step 5 | Auth/Register Customer |
| Find nearest shops | ShopDataIT | — | Shop/Nearest Shops |
| Place order from menu | OrderFlowIT | Step 6 | Order/Place Order |
| Queue position + wait time | OrderFlowIT | Step 7 | Order/Queue Position |
| Exit queue / cancel | OrderFlowIT | Step 13 (error) | Order/Cancel Order |
| View order history | OrderFlowIT | Step 10 | Order/My Orders |
| Shop closed rejection | OrderFlowIT | Step 11 | — |
| Queue full rejection | OrderFlowIT | Step 12 | — |
| Concurrent serve safety | ServeConcurrencyIT | — | — |
| Loyalty score increment | ServeFlowIT | — | — |

## 6. Testing Without a Frontend — Summary

| Method | Best For | Prerequisites |
|--------|----------|---------------|
| `./mvnw test` | CI/CD, regression testing | Docker (for Testcontainers) |
| `./test/demo.sh` | Quick validation of a live deployment | `curl`, `jq`, running server |
| Postman collection | Interactive exploration, API documentation | Postman (or Newman for CLI) |
| Swagger UI (`/swagger-ui.html`) | Ad-hoc manual testing, API discovery | Running server |
