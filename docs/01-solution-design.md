# Solution Design — Coffee Shop Pre-Order Platform

## 1. Overview

A global coffee shop chain needs a platform enabling customers to pre-order coffee for pickup via mobile apps. The platform serves two audiences:

- **Shop owners/operators** — configure shops, manage menus, and process queue orders
- **Customers** — find nearby shops, place orders, track queue position, and cancel if needed

This document covers the Customer App design. The platform exposes a RESTful API consumed by native mobile apps (iOS + Android) and potentially third-party integrators.

## 2. Use Cases

### UC-1: Customer Registration

| Field | Detail |
|-------|--------|
| **Actor** | New customer |
| **Precondition** | Customer does not have an existing account |
| **Trigger** | Customer opens the app and selects "Register" |
| **Main Flow** | 1. Customer enters mobile number, name, and address (home or work) 2. System validates inputs (unique mobile number, required fields) 3. System creates the customer record with loyalty score = 0 4. System issues a JWT token 5. Customer is logged in |
| **Extensions** | 1a. Mobile number already registered → return `409 EMAIL_TAKEN` error |
| **Postcondition** | Customer account exists; customer holds a valid session token |

### UC-2: Find Nearest Coffee Shops

| Field | Detail |
|-------|--------|
| **Actor** | Registered customer |
| **Precondition** | Customer is authenticated; device location is available |
| **Trigger** | Customer opens the "Find Shops" screen |
| **Main Flow** | 1. App sends customer's GPS coordinates to the API 2. System queries shops within a configurable radius (default 5 km) using PostGIS geospatial index 3. System returns shops ordered by distance, filtered to ACTIVE status 4. Customer views shop list with name, address, distance, and opening hours |
| **Extensions** | 2a. No shops within radius → return empty list |
| **Postcondition** | Customer sees a list of nearby shops |

### UC-3: Place an Order

| Field | Detail |
|-------|--------|
| **Actor** | Registered customer |
| **Precondition** | Customer is authenticated; a shop is selected |
| **Trigger** | Customer submits an order from a shop's menu |
| **Main Flow** | 1. Customer selects menu items and quantities 2. App sends the order (shop ID, items, optional idempotency key) 3. System validates: shop is active, shop is currently open (timezone-aware), all menu items exist and are available 4. System selects the least-loaded queue with capacity 5. System creates the order (status = WAITING), snapshots item names and prices, creates a queue entry 6. System returns the order details with total price |
| **Extensions** | 3a. Shop is closed → `409 SHOP_CLOSED` 3b. Menu item unavailable → `400 BAD_REQUEST` 4a. All queues full → `409 QUEUE_FULL` 2a. Duplicate idempotency key → return existing order (no new creation) |
| **Postcondition** | Order exists with status WAITING; customer is in a queue |

### UC-4: Check Queue Position

| Field | Detail |
|-------|--------|
| **Actor** | Customer with an active order |
| **Precondition** | Order exists with status WAITING |
| **Trigger** | Customer opens the order detail screen |
| **Main Flow** | 1. App requests queue position for the order 2. System counts how many entries are ahead in the same queue (by join time) 3. System calculates ETA = position × shop's average service time 4. System returns position (1-based) and ETA in seconds |
| **Postcondition** | Customer knows their place in line and expected wait |

### UC-5: Cancel Order / Exit Queue

| Field | Detail |
|-------|--------|
| **Actor** | Customer with an active order |
| **Precondition** | Order is in PLACED or WAITING status |
| **Trigger** | Customer taps "Cancel Order" |
| **Main Flow** | 1. App sends a DELETE request for the order 2. System verifies the customer owns the order 3. System sets order status to CANCELLED, timestamps the cancellation 4. System sets the queue entry status to CANCELLED 5. System returns the updated order |
| **Extensions** | 2a. Order already FULFILLED or CANCELLED → `409 INVALID_STATE` |
| **Postcondition** | Order is cancelled; queue slot is freed for other customers |

### UC-6: View Order History

| Field | Detail |
|-------|--------|
| **Actor** | Registered customer |
| **Precondition** | Customer is authenticated |
| **Trigger** | Customer opens "My Orders" |
| **Main Flow** | 1. App requests the customer's order history 2. System returns all orders for this customer (all statuses) with item details |
| **Postcondition** | Customer sees their past and current orders |

## 3. Domain / Concept Model

```mermaid
classDiagram
    direction LR

    class AppUser {
        +Long id
        +String email
        +String passwordHash
        +String displayName
        +Role role
    }

    class Customer {
        +Long id
        +String mobileNumber
        +String name
        +String addressLabel
        +String addressLine
        +Double latitude
        +Double longitude
        +int loyaltyScore
    }

    class Shop {
        +Long id
        +Long ownerId
        +String name
        +String contactPhone
        +String contactEmail
        +String addressLine
        +String city
        +String country
        +Double latitude
        +Double longitude
        +String timezone
        +String currency
        +LocalTime openingTime
        +LocalTime closingTime
        +int avgServiceSeconds
        +String status
    }

    class MenuItem {
        +Long id
        +Long shopId
        +String name
        +String description
        +long priceMinor
        +boolean available
    }

    class ShopQueue {
        +Long id
        +Long shopId
        +String label
        +int maxSize
        +int positionIndex
        +boolean active
    }

    class CustOrder {
        +Long id
        +Long customerId
        +Long shopId
        +OrderStatus status
        +long totalMinor
        +String idempotencyKey
        +OffsetDateTime placedAt
        +OffsetDateTime fulfilledAt
        +OffsetDateTime cancelledAt
    }

    class OrderItem {
        +Long id
        +Long orderId
        +Long menuItemId
        +String itemName
        +long unitPriceMinor
        +int quantity
    }

    class QueueEntry {
        +Long id
        +Long queueId
        +Long orderId
        +Long customerId
        +QueueEntryStatus status
        +OffsetDateTime joinedAt
        +OffsetDateTime servedAt
    }

    class OperatorAssignment {
        +Long id
        +Long userId
        +Long shopId
    }

    AppUser "1" --> "*" Shop : owns
    AppUser "1" --> "*" OperatorAssignment : assigned via
    Shop "1" --> "*" MenuItem : has menu
    Shop "1" --> "1..3" ShopQueue : has queues
    Shop "1" --> "*" OperatorAssignment : staffed by
    Customer "1" --> "*" CustOrder : places
    Shop "1" --> "*" CustOrder : receives
    CustOrder "1" --> "1..*" OrderItem : contains
    CustOrder "1" --> "1" QueueEntry : queued as
    ShopQueue "1" --> "*" QueueEntry : holds
    OrderItem "*" --> "1" MenuItem : references
```

## 4. Sequence Diagrams

### 4.1 Place Order

```mermaid
sequenceDiagram
    actor C as Customer App
    participant API as Order Controller
    participant OS as Order Service
    participant QS as Queue Selector
    participant DB as Database

    C->>API: POST /api/v1/orders<br/>{shopId, items[], idempotencyKey?}
    API->>OS: placeOrder(principal, request, idempotencyKey)

    OS->>DB: Check idempotency key (if provided)
    alt Duplicate key found
        OS-->>API: Return existing order
        API-->>C: 200 OK (existing order)
    end

    OS->>DB: Load shop by shopId
    OS->>OS: Validate shop ACTIVE
    OS->>OS: Check OpeningHours.isOpen(shop, clock)
    alt Shop closed
        OS-->>API: throw ConflictException(SHOP_CLOSED)
        API-->>C: 409 Conflict
    end

    OS->>DB: Load menu items by IDs
    OS->>OS: Validate all items exist & available
    OS->>OS: Calculate totalMinor

    OS->>QS: selectQueue(shopId)
    QS->>DB: Load active queues for shop
    QS->>DB: Count WAITING entries per queue
    QS->>QS: Pick queue with fewest entries & capacity
    alt All queues full
        QS-->>OS: Optional.empty()
        OS-->>API: throw ConflictException(QUEUE_FULL)
        API-->>C: 409 Conflict
    end
    QS-->>OS: Selected queue

    OS->>DB: INSERT cust_order (status=WAITING)
    OS->>DB: INSERT order_item (for each line item)
    OS->>DB: INSERT queue_entry (status=WAITING)
    OS-->>API: OrderResponse
    API-->>C: 201 Created
```

### 4.2 Serve Next (Operator)

```mermaid
sequenceDiagram
    actor Op as Operator App
    participant API as Queue Ops Controller
    participant QOS as Queue Ops Service
    participant SA as Shop Access
    participant DB as Database

    Op->>API: POST /api/v1/queues/{queueId}/serve-next
    API->>QOS: serveNext(principal, queueId)

    QOS->>SA: loadShopForOperator(principal, shopId)
    SA->>DB: Load shop
    SA->>DB: Check operator assignment
    alt Not authorized
        SA-->>QOS: throw ForbiddenException
        QOS-->>API: 403 Forbidden
    end

    QOS->>DB: SELECT ... FROM queue_entry<br/>WHERE status=WAITING<br/>ORDER BY joined_at LIMIT 1<br/>FOR UPDATE SKIP LOCKED
    alt No waiting entries
        QOS-->>API: throw NotFoundException
        API-->>Op: 404 Not Found
    end

    QOS->>DB: UPDATE queue_entry SET status=SERVED
    QOS->>DB: UPDATE cust_order SET status=FULFILLED
    QOS->>DB: UPDATE customer SET loyalty_score += 1
    QOS-->>API: ServeResponse
    API-->>Op: 200 OK
```

### 4.3 Cancel Order (Customer)

```mermaid
sequenceDiagram
    actor C as Customer App
    participant API as Order Controller
    participant OS as Order Service
    participant DB as Database

    C->>API: DELETE /api/v1/orders/{orderId}
    API->>OS: cancelOrder(principal, orderId)

    OS->>DB: Load order by ID
    OS->>OS: Verify customer owns order
    alt Not owner
        OS-->>API: throw ForbiddenException
        API-->>C: 403 Forbidden
    end

    OS->>OS: Check status is PLACED or WAITING
    alt Already FULFILLED or CANCELLED
        OS-->>API: throw ConflictException(INVALID_STATE)
        API-->>C: 409 Conflict
    end

    OS->>DB: UPDATE cust_order SET status=CANCELLED
    OS->>DB: UPDATE queue_entry SET status=CANCELLED
    OS-->>API: OrderResponse
    API-->>C: 200 OK
```

## 5. Data Design (ER Diagram)

```mermaid
erDiagram
    app_user {
        bigint id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar display_name "NOT NULL"
        varchar role "OWNER | OPERATOR"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at
    }

    customer {
        bigint id PK
        varchar mobile_number UK "NOT NULL"
        varchar name "NOT NULL"
        varchar address_label
        varchar address_line
        float8 latitude
        float8 longitude
        geography location "PostGIS POINT"
        int loyalty_score "default 0"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at
    }

    shop {
        bigint id PK
        bigint owner_id FK "NOT NULL"
        varchar name "NOT NULL"
        varchar contact_phone
        varchar contact_email
        varchar address_line
        varchar city
        varchar country "ISO 2-char"
        float8 latitude "NOT NULL"
        float8 longitude "NOT NULL"
        geography location "generated POINT"
        varchar timezone "NOT NULL"
        varchar currency "ISO 3-char"
        time opening_time "NOT NULL"
        time closing_time "NOT NULL"
        int avg_service_seconds "default 180"
        varchar status "default ACTIVE"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at
    }

    menu_item {
        bigint id PK
        bigint shop_id FK "NOT NULL"
        varchar name "NOT NULL"
        varchar description "max 500"
        bigint price_minor "NOT NULL"
        boolean available "default true"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at
    }

    queue {
        bigint id PK
        bigint shop_id FK "NOT NULL"
        varchar label "NOT NULL"
        int max_size "NOT NULL"
        int position_index "NOT NULL"
        boolean active "default true"
    }

    operator_assignment {
        bigint id PK
        bigint user_id FK "NOT NULL"
        bigint shop_id FK "NOT NULL"
    }

    cust_order {
        bigint id PK
        bigint customer_id FK "NOT NULL"
        bigint shop_id FK "NOT NULL"
        varchar status "PLACED|WAITING|FULFILLED|CANCELLED"
        bigint total_minor "NOT NULL"
        varchar idempotency_key "partial unique"
        timestamptz placed_at "NOT NULL"
        timestamptz fulfilled_at
        timestamptz cancelled_at
    }

    order_item {
        bigint id PK
        bigint order_id FK "NOT NULL"
        bigint menu_item_id FK "NOT NULL"
        varchar item_name "NOT NULL, snapshot"
        bigint unit_price_minor "NOT NULL, snapshot"
        int quantity "NOT NULL"
    }

    queue_entry {
        bigint id PK
        bigint queue_id FK "NOT NULL"
        bigint order_id FK "NOT NULL, unique"
        bigint customer_id FK "NOT NULL"
        varchar status "WAITING|SERVED|CANCELLED"
        timestamptz joined_at "NOT NULL"
        timestamptz served_at
    }

    app_user ||--o{ shop : "owns"
    app_user ||--o{ operator_assignment : "assigned"
    shop ||--o{ operator_assignment : "staffed by"
    shop ||--o{ menu_item : "has"
    shop ||--|{ queue : "has 1-3"
    shop ||--o{ cust_order : "receives"
    customer ||--o{ cust_order : "places"
    cust_order ||--|{ order_item : "contains"
    cust_order ||--|| queue_entry : "queued as"
    queue ||--o{ queue_entry : "holds"
    menu_item ||--o{ order_item : "referenced by"
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Prices stored as `bigint` minor units (cents) | Avoids floating-point rounding errors in currency calculations |
| `order_item` snapshots `item_name` and `unit_price_minor` | Decouples order history from menu changes; a price change doesn't rewrite history |
| `idempotency_key` has a partial unique index (non-null only) | Enables at-most-once order placement without constraining orders that don't use idempotency |
| PostGIS `geography` column on `shop` | Enables accurate great-circle distance queries across multiple geographies |
| `queue_entry.joined_at` determines FIFO order | Simple, chronologically correct ordering; no sequence gaps on cancellation |
| `FOR UPDATE SKIP LOCKED` on serve operations | Prevents double-serving under concurrent operator access without blocking |

## 6. Data Flow Diagram

```mermaid
flowchart TD
    subgraph Clients
        CA[Customer App<br/>iOS / Android]
        SA[Shop Owner App<br/>Android]
        TPI[Third-Party<br/>Integrators]
    end

    subgraph API Gateway / Load Balancer
        LB[ALB / Reverse Proxy]
    end

    subgraph Application Layer
        AUTH[Auth Service<br/>JWT issuance & validation]
        SHOP[Shop Service<br/>CRUD, menu, queues]
        ORDER[Order Service<br/>place, cancel, position]
        QUEUE[Queue Ops Service<br/>serve-next, serve-specific]
    end

    subgraph Data Layer
        PG[(PostgreSQL 16<br/>+ PostGIS)]
        LB_MIG[Liquibase<br/>Schema Migrations]
    end

    CA --> LB
    SA --> LB
    TPI --> LB
    LB --> AUTH
    LB --> SHOP
    LB --> ORDER
    LB --> QUEUE
    AUTH --> PG
    SHOP --> PG
    ORDER --> PG
    QUEUE --> PG
    LB_MIG --> PG

    classDef client fill:#e1f5fe
    classDef service fill:#fff3e0
    classDef data fill:#e8f5e9
    class CA,SA,TPI client
    class AUTH,SHOP,ORDER,QUEUE service
    class PG,LB_MIG data
```

### Data Flow: Place Order

```
Customer App
  → POST /api/v1/orders (JSON: shopId, items[])
    → JwtAuthFilter: extract & validate Bearer token
      → OrderController: deserialize + Bean Validation
        → OrderService.placeOrder():
          1. Check idempotency key → cust_order table
          2. Load shop → shop table
          3. Validate open hours → OpeningHours utility
          4. Load menu items → menu_item table
          5. Select queue → queue + queue_entry tables
          6. INSERT cust_order → cust_order table
          7. INSERT order_items → order_item table
          8. INSERT queue_entry → queue_entry table
        ← OrderResponse (JSON)
      ← 201 Created
    ← HTTP Response with X-Trace-Id header
  ← Display order confirmation
```
