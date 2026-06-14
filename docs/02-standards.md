# Coding, Naming, Security & Technology Standards

## 1. Technology Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| Language | Java | 17 (LTS) | Assessment requirement; strong typing, mature ecosystem |
| Framework | Spring Boot | 3.3.5 | Convention-over-configuration, production-ready defaults |
| Database | PostgreSQL | 16 | ACID compliance, JSON support, mature tooling |
| Geospatial | PostGIS | 3.4 | Industry-standard spatial queries (ST_DWithin, geography types) |
| Migrations | Liquibase | 4.x (Spring Boot managed) | Versioned, repeatable schema changes with rollback support |
| Auth | JWT (jjwt) | 0.12.6 | Stateless authentication suited for mobile + API consumers |
| Passwords | BCrypt | Spring Security | Adaptive hashing with configurable cost factor |
| Containers | Docker | Multi-stage build | Consistent builds, AWS-ready deployment |
| Testing | JUnit 5 + Testcontainers | Spring Boot managed | Real database testing with PostGIS container |
| API Docs | springdoc-openapi | 2.6.0 | Auto-generated OpenAPI 3 spec from controller annotations |

## 2. Coding Standards

### Architecture

- **Package-by-domain**: Code is organized by business domain (`auth`, `shop`, `order`, `queue`, `customer`, `common`, `config`), not by technical layer
- **Layered within each domain**: Controller → Service → Repository
- **No cross-domain JPA relationships**: Domains reference each other by ID (`Long`), not by JPA `@ManyToOne` annotations. This keeps domains decoupled and migration-ready for future service extraction

### Dependency Injection

- **Constructor injection only** — no `@Autowired` on fields
- Dependencies are `private final` fields
- Spring resolves constructors automatically (single-constructor rule)

### DTOs

- **Java records** for all request/response DTOs — immutable, concise, no boilerplate
- Request DTOs carry Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Min`, `@Email`)
- Response DTOs use `@JsonInclude(NON_NULL)` to omit null fields from JSON output

### Entity Design

- JPA entities use `@GeneratedValue(strategy = IDENTITY)` for database-assigned IDs
- Timestamps (`createdAt`, `updatedAt`) managed via `@PrePersist` / `@PreUpdate` callbacks
- No Lombok — explicit constructors, getters, and setters for clarity

### Error Handling

- Custom exception hierarchy rooted at `ApiException` (carries HTTP status + domain error code)
- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to structured `ErrorResponse`
- Bean Validation errors mapped to field-level error lists with code `VALIDATION_ERROR`
- Per-request trace IDs (`X-Trace-Id` header) for log correlation

### Transactions

- `@Transactional` on service methods that write data
- `@Transactional(readOnly = true)` on read-only service methods
- Pessimistic locking (`FOR UPDATE SKIP LOCKED`) for concurrent queue operations

## 3. Naming Conventions

| Element | Convention | Examples |
|---------|-----------|----------|
| Packages | lowercase, singular domain nouns | `com.coffee.platform.order`, `com.coffee.platform.auth` |
| Classes | PascalCase | `OrderService`, `QueueEntry`, `PlaceOrderRequest` |
| Interfaces / Records | PascalCase | `AuthPrincipal`, `ErrorResponse` |
| Methods | camelCase, verb-first | `placeOrder()`, `serveNext()`, `loadShopForOwner()` |
| Constants / Enums | UPPER_SNAKE_CASE | `WAITING`, `SHOP_CLOSED`, `MAX_QUEUES_REACHED` |
| Database tables | snake_case, singular | `cust_order`, `queue_entry`, `menu_item` |
| Database columns | snake_case | `customer_id`, `price_minor`, `joined_at` |
| API paths | kebab-case, versioned | `/api/v1/orders`, `/api/v1/queue-position` |
| API error codes | UPPER_SNAKE_CASE | `QUEUE_FULL`, `EMAIL_TAKEN`, `INVALID_STATE` |
| Environment variables | UPPER_SNAKE_CASE | `JWT_SECRET`, `SPRING_DATASOURCE_URL` |

### API Versioning

- Path-based versioning: `/api/v1/...`
- All endpoints under a single version prefix
- Version increment only on breaking changes

## 4. Security Standards

### Authentication

- Stateless JWT tokens (no server-side sessions)
- HMAC-SHA signing with a configurable secret (`JWT_SECRET` environment variable)
- Token expiration configurable via `JWT_EXPIRES_SECONDS` (default: 1 hour)
- Separate auth flows for staff (email + password) and customers (mobile + OTP)

### Password Storage

- BCrypt hashing via Spring Security's `BCryptPasswordEncoder`
- Passwords never stored or logged in plaintext
- Minimum password length enforced via Bean Validation (`@Size(min = 6)`)

### Authorization

- Three principal types: `OWNER`, `OPERATOR`, `CUSTOMER`
- Role checked at the service layer via utility methods (`requireOwner()`, `requireCustomer()`)
- Shop-scoped access: owners access their own shops; operators access shops they are assigned to
- Customer-scoped access: customers access only their own orders

### API Security

- CSRF disabled (appropriate for stateless JWT APIs)
- CORS configured (restrict origins in production)
- Session management set to STATELESS
- Swagger UI and health endpoint are public; all other endpoints require authentication
- Custom `AuthenticationEntryPoint` returns JSON error (not Spring's default HTML page)

### Secrets Management

- All secrets externalized via environment variables
- `.env.example` provided as a template; `.env` is gitignored
- Production deployments should use AWS Secrets Manager or equivalent

## 5. Database Standards

### Schema Management

- Liquibase is the sole owner of schema; Hibernate is set to `ddl-auto: validate`
- Migrations are sequentially numbered (`001-extensions`, `002-users`, etc.)
- Each migration includes rollback SQL
- Seed data is context-gated (`LIQUIBASE_CONTEXTS=demo`) to avoid polluting production

### Data Integrity

- Foreign key relationships enforced at the database level
- `CHECK` constraints on enum-like columns (e.g., `role IN ('OWNER','OPERATOR')`)
- Unique constraints where business rules demand it (email, mobile number, menu item name per shop)
- Partial unique index on `idempotency_key` (only non-null values)

### Performance Indexing

- PostGIS GIST index on `shop.location` for geospatial queries
- Composite index on `queue_entry(queue_id, status, joined_at)` for efficient FIFO ordering
- Compound index on `shop(country, city)` for regional filtering

## 6. API Design Standards

### Request/Response Format

- All responses are JSON with `Content-Type: application/json`
- Successful responses return the resource directly (no envelope wrapper)
- Error responses use a consistent `ErrorResponse` structure:
  ```json
  {
    "code": "QUEUE_FULL",
    "message": "All queues are full",
    "traceId": "a1b2c3d4e5f6g7h8"
  }
  ```
- Validation errors include field-level details:
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "traceId": "...",
    "fieldErrors": [
      {"field": "shopId", "message": "must not be null"}
    ]
  }
  ```

### HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Successful retrieval or update |
| 201 | Resource created (e.g., new order) |
| 400 | Validation failure or bad input |
| 401 | Missing or invalid authentication |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Business rule conflict (shop closed, queue full, duplicate) |

### Idempotency

- `POST /api/v1/orders` supports an `Idempotency-Key` header
- Duplicate requests with the same key return the existing order without creating a new one
- Prevents double-charges from network retries
