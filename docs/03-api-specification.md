# API Specification

Base URL: `/api/v1`

All endpoints return JSON. Authenticated endpoints require `Authorization: Bearer <token>` header.

---

## 1. Authentication

### POST /auth/owner/register

Register a new shop owner account.

**Request:**
```json
{
  "email": "owner@example.com",
  "password": "secret123",
  "displayName": "Jane Owner"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| email | string | Required, valid email format, unique |
| password | string | Required, min 6 characters |
| displayName | string | Required |

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "USER",
  "id": 1
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 400 | VALIDATION_ERROR | Missing or invalid fields |
| 409 | EMAIL_TAKEN | Email already registered |

---

### POST /auth/login

Login for shop owners and operators (email + password).

**Request:**
```json
{
  "email": "owner@example.com",
  "password": "secret123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "USER",
  "id": 1
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 401 | UNAUTHORIZED | Invalid email or password |

---

### POST /auth/customer/register

Register a new customer account.

**Request:**
```json
{
  "mobileNumber": "+6591234567",
  "name": "Alex Tan"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| mobileNumber | string | Required, unique |
| name | string | Required |

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "CUSTOMER",
  "id": 1
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 409 | MOBILE_TAKEN | Mobile number already registered |

---

### POST /auth/customer/login

Customer login via mobile number + OTP.

**Request:**
```json
{
  "mobileNumber": "+6591234567",
  "otp": "000000"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "CUSTOMER",
  "id": 1
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 401 | UNAUTHORIZED | Invalid mobile number or OTP |

> Note: OTP is currently a development stub (`000000`). Production would integrate with an SMS gateway (e.g., Twilio, AWS SNS).

---

### GET /auth/me

Get the authenticated user's profile.

**Auth:** Required (any role)

**Response:** `200 OK`
```json
{
  "id": 1,
  "subject": "owner@example.com",
  "type": "USER",
  "role": "OWNER"
}
```

---

## 2. Shops

### POST /shops

Create a new shop.

**Auth:** OWNER only

**Request:**
```json
{
  "name": "Chain Coffee - Raffles",
  "contactPhone": "+6561234567",
  "contactEmail": "raffles@chain.com",
  "addressLine": "1 Raffles Place",
  "city": "Singapore",
  "country": "SG",
  "latitude": 1.2840,
  "longitude": 103.8513,
  "timezone": "Asia/Singapore",
  "currency": "SGD",
  "openingTime": "07:00",
  "closingTime": "19:00",
  "avgServiceSeconds": 180
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| name | string | Required |
| contactPhone | string | Optional |
| contactEmail | string | Optional |
| addressLine | string | Optional |
| city | string | Optional |
| country | string | Required, 2-char ISO code |
| latitude | double | Required |
| longitude | double | Required |
| timezone | string | Required, IANA timezone ID |
| currency | string | Required, 3-char ISO code |
| openingTime | string | Required, `HH:mm` format |
| closingTime | string | Required, `HH:mm` format |
| avgServiceSeconds | int | Optional, default 180 |

**Response:** `201 Created`
```json
{
  "id": 1,
  "ownerId": 1,
  "name": "Chain Coffee - Raffles",
  "contactPhone": "+6561234567",
  "contactEmail": "raffles@chain.com",
  "addressLine": "1 Raffles Place",
  "city": "Singapore",
  "country": "SG",
  "latitude": 1.284,
  "longitude": 103.8513,
  "timezone": "Asia/Singapore",
  "currency": "SGD",
  "openingTime": "07:00",
  "closingTime": "19:00",
  "avgServiceSeconds": 180,
  "status": "ACTIVE"
}
```

---

### GET /shops

List all active shops.

**Auth:** Not required

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Chain Coffee - Raffles",
    "city": "Singapore",
    "country": "SG",
    "latitude": 1.284,
    "longitude": 103.8513,
    "openingTime": "07:00",
    "closingTime": "19:00",
    "status": "ACTIVE"
  }
]
```

---

### GET /shops/{id}

Get a single shop by ID.

**Auth:** Not required

**Response:** `200 OK` — same shape as the create response.

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 404 | NOT_FOUND | Shop does not exist |

---

### GET /shops/nearest

Find shops closest to a given location.

**Auth:** Not required

**Query Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| lat | double | Yes | — | Latitude of search center |
| lng | double | Yes | — | Longitude of search center |
| radiusKm | double | No | 5 | Search radius in kilometers |
| limit | int | No | 10 | Maximum results to return |

**Response:** `200 OK` — array of shop objects ordered by distance.

---

### POST /shops/{shopId}/menu-items

Add a menu item to a shop.

**Auth:** OWNER of the shop

**Request:**
```json
{
  "name": "Flat White",
  "description": "Double-shot espresso with steamed milk",
  "priceMinor": 550
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| name | string | Required, unique per shop |
| description | string | Optional, max 500 chars |
| priceMinor | long | Required, price in minor currency units (e.g., cents) |

**Response:** `201 Created`
```json
{
  "id": 1,
  "shopId": 1,
  "name": "Flat White",
  "description": "Double-shot espresso with steamed milk",
  "priceMinor": 550,
  "available": true
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not the shop owner |
| 409 | DUPLICATE_MENU_ITEM | Item name already exists for this shop |

---

### GET /shops/{shopId}/menu-items

List available menu items for a shop.

**Auth:** Not required

**Response:** `200 OK` — array of menu item objects (only `available = true`).

---

### POST /shops/{shopId}/queues

Add a queue to a shop (max 3 per shop).

**Auth:** OWNER of the shop

**Request:**
```json
{
  "label": "Counter 1",
  "maxSize": 20
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| label | string | Required |
| maxSize | int | Required, minimum 1 |

**Response:** `201 Created`
```json
{
  "id": 1,
  "shopId": 1,
  "label": "Counter 1",
  "maxSize": 20,
  "positionIndex": 0,
  "active": true
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not the shop owner |
| 409 | MAX_QUEUES_REACHED | Shop already has 3 queues |

---

### GET /shops/{shopId}/queues

List queues for a shop.

**Auth:** Not required

**Response:** `200 OK` — array of queue objects.

---

### POST /shops/{shopId}/operators

Assign an operator to a shop.

**Auth:** OWNER of the shop

**Request:**
```json
{
  "userId": 2
}
```

**Response:** `204 No Content`

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not the shop owner |
| 404 | NOT_FOUND | User does not exist or is not an OPERATOR |
| 409 | ALREADY_ASSIGNED | Operator is already assigned to this shop |

---

## 3. Orders

### POST /orders

Place a new order.

**Auth:** CUSTOMER only

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| Idempotency-Key | No | Prevents duplicate order creation on retries |

**Request:**
```json
{
  "shopId": 1,
  "items": [
    {"menuItemId": 1, "quantity": 2},
    {"menuItemId": 3, "quantity": 1}
  ]
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| shopId | long | Required |
| items | array | Required, non-empty |
| items[].menuItemId | long | Required |
| items[].quantity | int | Required, min 1 |

**Response:** `201 Created`
```json
{
  "id": 1,
  "shopId": 1,
  "status": "WAITING",
  "totalMinor": 1650,
  "currency": "SGD",
  "placedAt": "2026-06-14T10:30:00Z",
  "items": [
    {
      "menuItemId": 1,
      "itemName": "Flat White",
      "unitPriceMinor": 550,
      "quantity": 2
    },
    {
      "menuItemId": 3,
      "itemName": "Croissant",
      "unitPriceMinor": 550,
      "quantity": 1
    }
  ]
}
```

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 400 | BAD_REQUEST | Shop not active, menu item unavailable |
| 400 | VALIDATION_ERROR | Missing or invalid fields |
| 404 | NOT_FOUND | Shop or menu item not found |
| 409 | SHOP_CLOSED | Shop is outside opening hours |
| 409 | QUEUE_FULL | All queues at capacity |

---

### GET /orders/{id}

Get order details.

**Auth:** Customer (own order) or Shop owner/operator

**Response:** `200 OK` — same shape as the place order response.

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not authorized to view this order |
| 404 | NOT_FOUND | Order not found |

---

### GET /orders/{id}/queue-position

Get the customer's position in the queue and estimated wait time.

**Auth:** Customer (own order) or Shop owner/operator

**Response:** `200 OK`
```json
{
  "orderId": 1,
  "queueId": 1,
  "position": 3,
  "etaSeconds": 540
}
```

- `position`: 1-based (1 = next to be served)
- `etaSeconds`: position × shop's `avgServiceSeconds`

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 409 | INVALID_STATE | Order is not in WAITING status |

---

### DELETE /orders/{id}

Cancel an order and exit the queue.

**Auth:** CUSTOMER only (own order)

**Response:** `200 OK` — updated order with status `CANCELLED`.

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 403 | FORBIDDEN | Not the order's customer |
| 409 | INVALID_STATE | Order already fulfilled or cancelled |

---

### GET /customers/me/orders

Get the authenticated customer's order history.

**Auth:** CUSTOMER only

**Response:** `200 OK` — array of order objects (all statuses).

---

## 4. Queue Operations

### GET /queues/{id}

Get a queue snapshot.

**Auth:** Shop OWNER or assigned OPERATOR

**Response:** `200 OK`
```json
{
  "queueId": 1,
  "label": "Counter 1",
  "maxSize": 20,
  "waitingCount": 5
}
```

---

### GET /queues/{id}/entries

List all waiting entries in a queue.

**Auth:** Shop OWNER or assigned OPERATOR

**Response:** `200 OK`
```json
[
  {
    "entryId": 1,
    "orderId": 10,
    "customerId": 5,
    "customerName": "Alex Tan",
    "loyaltyScore": 12,
    "joinedAt": "2026-06-14T10:30:00Z",
    "status": "WAITING"
  }
]
```

---

### POST /queues/{id}/serve-next

Serve the next customer in FIFO order.

**Auth:** Shop OWNER or assigned OPERATOR

**Response:** `200 OK`
```json
{
  "entryId": 1,
  "orderId": 10,
  "customerId": 5,
  "status": "SERVED"
}
```

Uses `FOR UPDATE SKIP LOCKED` — concurrent operators serve different entries without blocking.

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 404 | NOT_FOUND | No waiting entries in queue |

---

### POST /queues/{id}/entries/{entryId}/serve

Serve a specific entry (out of FIFO order).

**Auth:** Shop OWNER or assigned OPERATOR

**Response:** `200 OK` — same shape as serve-next.

**Errors:**
| Status | Code | Condition |
|--------|------|-----------|
| 400 | BAD_REQUEST | Entry does not belong to this queue |
| 404 | NOT_FOUND | Entry not found or not in WAITING status |

---

## 5. System

### GET /actuator/health

Health check endpoint.

**Auth:** Not required

**Response:** `200 OK`
```json
{"status": "UP"}
```
