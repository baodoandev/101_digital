# Coffee Shop Pre-Order Platform

A multi-tenant coffee shop pre-order and queue management API built with Spring Boot, PostgreSQL + PostGIS, and JWT authentication.

## Quick Start

```bash
git clone <repo-url> && cd 101_digital
docker compose up --build
# Wait ~30s for startup
open http://localhost:8080/swagger-ui.html
```

The platform starts with demo seed data pre-loaded. Health check: `curl http://localhost:8080/actuator/health`

## Seeded Demo Credentials

| Role | Login | Credentials |
|------|-------|-------------|
| Owner | `POST /api/v1/auth/login` | `owner@chain.com` / `password` |
| Operator | `POST /api/v1/auth/login` | `operator@chain.com` / `password` |
| Customer (Alex) | `POST /api/v1/auth/customer/login` | `+6591110001` / OTP `000000` |
| Customer (Bao) | `POST /api/v1/auth/customer/login` | `+447700900001` / OTP `000000` |

Seeded shops: "Chain Coffee - Raffles" (Singapore, SGD) and "Chain Coffee - Bank" (London, GBP), each with 6 menu items and queues.

## Running the Demo

The demo script exercises the full happy path plus error cases:

```bash
./test/demo.sh                          # default: http://localhost:8080
./test/demo.sh http://localhost:8080    # explicit URL
```

Requires `curl` and `jq`.

## Running Tests

```bash
./mvnw test
```

Requires Docker running locally (Testcontainers spins up PostgreSQL + PostGIS automatically).

## API Reference

All endpoints are under `/api/v1`. Swagger UI available at `/swagger-ui.html`.

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/owner/register` | - | Register shop owner |
| POST | `/api/v1/auth/operator/register` | - | Register operator |
| POST | `/api/v1/auth/login` | - | Login (owner/operator) |
| POST | `/api/v1/auth/customer/register` | - | Register customer (with optional address) |
| POST | `/api/v1/auth/customer/login` | - | Customer login via OTP |
| GET | `/api/v1/auth/me` | Bearer | Get current identity |

### Shops

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/shops` | Owner | Create a shop |
| GET | `/api/v1/shops` | Bearer | List all shops |
| GET | `/api/v1/shops/{id}` | Bearer | Get shop details |
| GET | `/api/v1/shops/nearest` | Bearer | Find shops by location |
| POST | `/api/v1/shops/{id}/menu-items` | Owner | Add menu item |
| GET | `/api/v1/shops/{id}/menu-items` | Bearer | List menu items |
| POST | `/api/v1/shops/{id}/queues` | Owner | Create queue |
| GET | `/api/v1/shops/{id}/queues` | Bearer | List queues |
| POST | `/api/v1/shops/{id}/operators` | Owner | Assign operator |

### Orders

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/orders` | Customer | Place order |
| GET | `/api/v1/orders/{id}` | Bearer | Get order details |
| GET | `/api/v1/orders/{id}/queue-position` | Customer | Check queue position + ETA |
| DELETE | `/api/v1/orders/{id}` | Customer | Cancel order |
| GET | `/api/v1/customers/me` | Customer | Get profile |
| PATCH | `/api/v1/customers/me` | Customer | Update profile (name, address) |
| GET | `/api/v1/customers/me/orders` | Customer | Order history |

### Queue Operations

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/queues/{id}` | Operator/Owner | Queue snapshot |
| GET | `/api/v1/queues/{id}/entries` | Operator/Owner | List queue entries |
| POST | `/api/v1/queues/{id}/serve-next` | Operator/Owner | Serve next in queue |
| POST | `/api/v1/queues/{id}/entries/{eid}/serve` | Operator/Owner | Serve specific entry |

### System

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | - | Health check |
| GET | `/swagger-ui.html` | - | API documentation |

## Design Documentation

Full Part 1 design documents are in the [`docs/`](docs/) directory:

- [Solution Design](docs/01-solution-design.md) — use cases, domain model, sequence diagrams, ER diagram, data flows
- [Standards](docs/02-standards.md) — coding, naming, security, and technology standards
- [API Specification](docs/03-api-specification.md) — detailed endpoint reference with request/response schemas
- [Security Solution](docs/04-security.md) — authentication architecture, authorization model, production hardening
- [Test Strategy](docs/05-test-strategy.md) — how to test without a frontend, requirements traceability

## Tech Stack

- **Java 17** + **Spring Boot 3.3.5**
- **PostgreSQL 16** with **PostGIS** for geospatial queries
- **Liquibase** for database migrations
- **JWT** (jjwt) for stateless authentication
- **Testcontainers** for integration tests
- **Docker** for packaging and deployment

## Architecture

### Package Structure

```
com.coffee.platform
├── auth/           # JWT auth, owner/customer registration, login
├── common/         # Shared exceptions, error handling, trace ID filter
├── config/         # Security, OpenAPI, application config
├── customer/       # Customer entity and order history endpoint
├── order/          # Order placement, cancellation, queue selection
├── queue/          # Queue operations (serve-next, serve-specific)
└── shop/           # Shop, menu item, queue, operator management
```

### Key Design Patterns

- **SKIP LOCKED queue selection**: Orders are routed to the least-loaded queue using `SELECT ... FOR UPDATE SKIP LOCKED`, preventing double-serve under concurrent load.
- **Price snapshot on order**: Item prices are captured at order time into `order_item`, so menu price changes don't affect existing orders.
- **Idempotency keys**: `POST /orders` accepts an `Idempotency-Key` header to prevent duplicate orders from retries.
- **Opening hours with timezone**: Each shop stores its timezone; the API checks whether the shop is currently open before accepting orders, with midnight-wrap support.
- **PostGIS nearest-shop**: `GET /shops/nearest` uses `ST_DWithin` and `ST_Distance` for efficient geospatial lookups with a radius filter.

## Testing

46+ tests covering:

- **Unit tests**: JWT service, queue selector, opening hours logic
- **Integration tests**: Full auth flow, shop data CRUD, order lifecycle, serve flow
- **Concurrency tests**: Proves SKIP LOCKED prevents double-serve with parallel threads

All integration tests use Testcontainers with PostGIS, so they run against a real PostgreSQL instance.

## AWS Deployment Notes

### Option A: EC2 + Docker Compose (Free-Tier Friendly)

1. Launch a `t2.micro` (or `t3.micro`) EC2 instance with Amazon Linux 2023
2. Install Docker and Docker Compose:
   ```bash
   sudo yum install -y docker
   sudo systemctl start docker
   sudo usermod -aG docker ec2-user
   # Install compose plugin
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   ```
3. Clone the repo and create `.env` from `.env.example` with production values
4. `docker compose up -d`
5. Open port 8080 in the EC2 security group (or put behind an ALB)

### Option B: ECS Fargate + RDS PostgreSQL

1. Create an RDS PostgreSQL 16 instance with the PostGIS extension enabled
2. Build and push the Docker image to ECR
3. Create an ECS Fargate task definition using the image, passing `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` as environment variables (use AWS Secrets Manager for sensitive values)
4. Create an ECS service behind an ALB with health check on `/actuator/health`
5. Set `LIQUIBASE_CONTEXTS=demo` for demo data, or omit for a clean database

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | Yes | JDBC PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Min 32 chars, change in production |
| `LIQUIBASE_CONTEXTS` | No | `demo` to load seed data |

## Postman Collection

Import `test/coffee-platform.postman_collection.json` into Postman. Run the requests in order — test scripts auto-extract tokens and IDs into collection variables.

## Configuration

Copy `.env.example` to `.env` and adjust values. Key settings:

- `POSTGRES_PASSWORD`: Change from default in production
- `JWT_SECRET`: Must be at least 32 characters
- `LIQUIBASE_CONTEXTS`: Set to `demo` for seed data, omit for clean DB
- `API_PORT`: Defaults to 8080
- `DB_PORT`: Defaults to 5433 (host-side)
