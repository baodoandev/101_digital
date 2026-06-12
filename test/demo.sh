#!/usr/bin/env bash
#
# Coffee Platform — end-to-end demo script
# Exercises the entire happy path and key error cases via curl + jq.
# Usage:  ./test/demo.sh [base_url]
#         Default base_url: http://localhost:8080
#
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
UNIQUE=$(date +%s)

# ── Colour helpers ──────────────────────────────────────────────────
green()  { printf "\033[32m%s\033[0m\n" "$*"; }
red()    { printf "\033[31m%s\033[0m\n" "$*"; }
yellow() { printf "\033[33m%s\033[0m\n" "$*"; }
bold()   { printf "\033[1m%s\033[0m\n" "$*"; }

step() {
  echo
  bold "──────────────────────────────────────────────────────"
  bold "  STEP $1: $2"
  bold "──────────────────────────────────────────────────────"
}

assert_status() {
  local label="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    green "  ✓ $label — HTTP $actual"
    PASS=$((PASS + 1))
  else
    red   "  ✗ $label — expected HTTP $expected, got HTTP $actual"
    FAIL=$((FAIL + 1))
  fi
}

assert_json() {
  local label="$1" field="$2" expected="$3" body="$4"
  local actual
  actual=$(echo "$body" | jq -r "$field" 2>/dev/null || echo "PARSE_ERROR")
  if [ "$actual" = "$expected" ]; then
    green "  ✓ $label — $field = $actual"
    PASS=$((PASS + 1))
  else
    red   "  ✗ $label — $field expected '$expected', got '$actual'"
    FAIL=$((FAIL + 1))
  fi
}

# Helper: perform a request and capture both status and body
do_req() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  local curl_args=(-s -w "\n%{http_code}" -X "$method" "$url")
  curl_args+=(-H "Content-Type: application/json")
  [ -n "$token" ]  && curl_args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ]   && curl_args+=(-d "$body")
  curl "${curl_args[@]}"
}

parse_status() { echo "$1" | tail -1; }
parse_body()   { echo "$1" | sed '$d'; }

# ════════════════════════════════════════════════════════════════════
#  HEALTH CHECK
# ════════════════════════════════════════════════════════════════════
step 0 "Health check"
RESP=$(do_req GET "$BASE_URL/actuator/health")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Health endpoint" 200 "$STATUS"
assert_json   "Status UP" ".status" "UP" "$BODY"

# ════════════════════════════════════════════════════════════════════
#  HAPPY PATH
# ════════════════════════════════════════════════════════════════════

# ── 1. Register a fresh owner ──────────────────────────────────────
step 1 "Register owner"
OWNER_EMAIL="owner-${UNIQUE}@demo.com"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/owner/register" "" \
  "{\"email\":\"$OWNER_EMAIL\",\"password\":\"password123\",\"displayName\":\"Demo Owner\"}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Register owner" 201 "$STATUS"
OWNER_TOKEN=$(echo "$BODY" | jq -r '.token')
OWNER_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Owner token: ${OWNER_TOKEN:0:20}..."

# ── 2. Create a shop (always-open hours) ───────────────────────────
step 2 "Create shop"
RESP=$(do_req POST "$BASE_URL/api/v1/shops" "$OWNER_TOKEN" \
  '{
    "name":"Demo Coffee Lab",
    "contactPhone":"+1234567890",
    "contactEmail":"demo@coffee.com",
    "addressLine":"1 Demo Street",
    "city":"Singapore",
    "country":"SG",
    "latitude":1.2841,
    "longitude":103.8511,
    "timezone":"Asia/Singapore",
    "currency":"SGD",
    "openingTime":"00:00",
    "closingTime":"23:59",
    "avgServiceSeconds":120
  }')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Create shop" 201 "$STATUS"
SHOP_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Shop ID: $SHOP_ID"

# ── 3. Add menu items ─────────────────────────────────────────────
step 3 "Add menu items"
RESP=$(do_req POST "$BASE_URL/api/v1/shops/$SHOP_ID/menu-items" "$OWNER_TOKEN" \
  '{"name":"Flat White","description":"Double shot, silky milk","priceMinor":550}')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Add menu item 1" 201 "$STATUS"
ITEM1_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Menu item 1 ID: $ITEM1_ID"

RESP=$(do_req POST "$BASE_URL/api/v1/shops/$SHOP_ID/menu-items" "$OWNER_TOKEN" \
  '{"name":"Croissant","description":"Butter croissant","priceMinor":400}')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Add menu item 2" 201 "$STATUS"
ITEM2_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Menu item 2 ID: $ITEM2_ID"

# ── 4. Create a queue ─────────────────────────────────────────────
step 4 "Create queue"
RESP=$(do_req POST "$BASE_URL/api/v1/shops/$SHOP_ID/queues" "$OWNER_TOKEN" \
  '{"label":"Main Queue","maxSize":10}')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Create queue" 201 "$STATUS"
QUEUE_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Queue ID: $QUEUE_ID"

# ── 5. Register a customer ────────────────────────────────────────
step 5 "Register customer"
CUST_MOBILE="+1555${UNIQUE: -7}"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/customer/register" "" \
  "{\"mobileNumber\":\"$CUST_MOBILE\",\"name\":\"Demo Customer\"}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Register customer" 201 "$STATUS"
CUST_TOKEN=$(echo "$BODY" | jq -r '.token')
CUST_ID=$(echo "$BODY" | jq -r '.id')
yellow "  Customer token: ${CUST_TOKEN:0:20}..."

# ── 6. Place an order (customer) ──────────────────────────────────
step 6 "Place order"
RESP=$(do_req POST "$BASE_URL/api/v1/orders" "$CUST_TOKEN" \
  "{\"shopId\":$SHOP_ID,\"items\":[{\"menuItemId\":$ITEM1_ID,\"quantity\":1},{\"menuItemId\":$ITEM2_ID,\"quantity\":1}]}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Place order" 201 "$STATUS"
assert_json   "Order status" ".status" "WAITING" "$BODY"
ORDER_ID=$(echo "$BODY" | jq -r '.id')
TOTAL=$(echo "$BODY" | jq -r '.totalMinor')
yellow "  Order ID: $ORDER_ID, Total: $TOTAL minor units"

# ── 7. Check queue position ───────────────────────────────────────
step 7 "Queue position"
RESP=$(do_req GET "$BASE_URL/api/v1/orders/$ORDER_ID/queue-position" "$CUST_TOKEN")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Queue position" 200 "$STATUS"
POSITION=$(echo "$BODY" | jq -r '.position')
yellow "  Position: $POSITION"

# ── 8. Owner serves next ──────────────────────────────────────────
step 8 "Serve next (owner)"
RESP=$(do_req POST "$BASE_URL/api/v1/queues/$QUEUE_ID/serve-next" "$OWNER_TOKEN")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Serve next" 200 "$STATUS"
assert_json   "Serve status" ".status" "SERVED" "$BODY"

# ── 9. Verify order is FULFILLED ──────────────────────────────────
step 9 "Verify order fulfilled"
RESP=$(do_req GET "$BASE_URL/api/v1/orders/$ORDER_ID" "$CUST_TOKEN")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Get order" 200 "$STATUS"
assert_json   "Order fulfilled" ".status" "FULFILLED" "$BODY"

# ── 10. Customer order history ────────────────────────────────────
step 10 "Customer order history"
RESP=$(do_req GET "$BASE_URL/api/v1/customers/me/orders" "$CUST_TOKEN")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "My orders" 200 "$STATUS"
ORDER_COUNT=$(echo "$BODY" | jq 'length')
yellow "  Orders in history: $ORDER_COUNT"

# ════════════════════════════════════════════════════════════════════
#  ERROR CASES
# ════════════════════════════════════════════════════════════════════

# ── 11. SHOP_CLOSED — create a shop with narrow hours ─────────────
step 11 "Error: shop closed"
RESP=$(do_req POST "$BASE_URL/api/v1/shops" "$OWNER_TOKEN" \
  '{
    "name":"Never Open Shop",
    "country":"SG",
    "latitude":1.28,
    "longitude":103.85,
    "timezone":"Asia/Singapore",
    "currency":"SGD",
    "openingTime":"03:00",
    "closingTime":"03:01"
  }')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
CLOSED_SHOP_ID=$(echo "$BODY" | jq -r '.id')

# Add a menu item and queue so we can attempt an order
do_req POST "$BASE_URL/api/v1/shops/$CLOSED_SHOP_ID/menu-items" "$OWNER_TOKEN" \
  '{"name":"Ghost Coffee","priceMinor":100}' > /dev/null
CLOSED_ITEM_RESP=$(do_req GET "$BASE_URL/api/v1/shops/$CLOSED_SHOP_ID/menu-items" "$OWNER_TOKEN")
CLOSED_ITEM_ID=$(parse_body "$CLOSED_ITEM_RESP" | jq -r '.[0].id')
do_req POST "$BASE_URL/api/v1/shops/$CLOSED_SHOP_ID/queues" "$OWNER_TOKEN" \
  '{"label":"Ghost Queue","maxSize":5}' > /dev/null

RESP=$(do_req POST "$BASE_URL/api/v1/orders" "$CUST_TOKEN" \
  "{\"shopId\":$CLOSED_SHOP_ID,\"items\":[{\"menuItemId\":$CLOSED_ITEM_ID,\"quantity\":1}]}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Shop closed" 409 "$STATUS"
assert_json   "Error code" ".code" "SHOP_CLOSED" "$BODY"

# ── 12. QUEUE_FULL — queue with maxSize=1, fill it ────────────────
step 12 "Error: queue full"
# Create shop with tiny queue
RESP=$(do_req POST "$BASE_URL/api/v1/shops" "$OWNER_TOKEN" \
  '{
    "name":"Tiny Queue Shop",
    "country":"SG",
    "latitude":1.28,
    "longitude":103.85,
    "timezone":"Asia/Singapore",
    "currency":"SGD",
    "openingTime":"00:00",
    "closingTime":"23:59"
  }')
TINY_SHOP_ID=$(parse_body "$RESP" | jq -r '.id')

do_req POST "$BASE_URL/api/v1/shops/$TINY_SHOP_ID/menu-items" "$OWNER_TOKEN" \
  '{"name":"Tiny Coffee","priceMinor":100}' > /dev/null
TINY_ITEM_RESP=$(do_req GET "$BASE_URL/api/v1/shops/$TINY_SHOP_ID/menu-items" "$OWNER_TOKEN")
TINY_ITEM_ID=$(parse_body "$TINY_ITEM_RESP" | jq -r '.[0].id')

do_req POST "$BASE_URL/api/v1/shops/$TINY_SHOP_ID/queues" "$OWNER_TOKEN" \
  '{"label":"Tiny Queue","maxSize":1}' > /dev/null

# Fill the queue with one order
do_req POST "$BASE_URL/api/v1/orders" "$CUST_TOKEN" \
  "{\"shopId\":$TINY_SHOP_ID,\"items\":[{\"menuItemId\":$TINY_ITEM_ID,\"quantity\":1}]}" > /dev/null

# Second order should fail
RESP=$(do_req POST "$BASE_URL/api/v1/orders" "$CUST_TOKEN" \
  "{\"shopId\":$TINY_SHOP_ID,\"items\":[{\"menuItemId\":$TINY_ITEM_ID,\"quantity\":1}]}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Queue full" 409 "$STATUS"
assert_json   "Error code" ".code" "QUEUE_FULL" "$BODY"

# ── 13. Cancel fulfilled order → INVALID_STATE ────────────────────
step 13 "Error: cancel fulfilled order"
RESP=$(do_req DELETE "$BASE_URL/api/v1/orders/$ORDER_ID" "$CUST_TOKEN")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Cancel fulfilled" 409 "$STATUS"
assert_json   "Error code" ".code" "INVALID_STATE" "$BODY"

# ── 14. Duplicate email registration ──────────────────────────────
step 14 "Error: duplicate email"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/owner/register" "" \
  "{\"email\":\"$OWNER_EMAIL\",\"password\":\"password123\",\"displayName\":\"Dup Owner\"}")
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Duplicate email" 409 "$STATUS"
assert_json   "Error code" ".code" "EMAIL_TAKEN" "$BODY"

# ── 15. Invalid credentials ───────────────────────────────────────
step 15 "Error: invalid credentials"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/login" "" \
  '{"email":"nobody@nowhere.com","password":"wrongpassword"}')
STATUS=$(parse_status "$RESP")
assert_status "Invalid credentials" 401 "$STATUS"

# ── 16. Access without token ──────────────────────────────────────
step 16 "Error: no token"
RESP=$(do_req GET "$BASE_URL/api/v1/orders/1" "")
STATUS=$(parse_status "$RESP")
assert_status "No token" 401 "$STATUS"

# ── 17. Customer accessing another customer's order ───────────────
step 17 "Error: forbidden (other customer's order)"
# Register a second customer
CUST2_MOBILE="+1666${UNIQUE: -7}"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/customer/register" "" \
  "{\"mobileNumber\":\"$CUST2_MOBILE\",\"name\":\"Other Customer\"}")
CUST2_TOKEN=$(parse_body "$RESP" | jq -r '.token')

RESP=$(do_req GET "$BASE_URL/api/v1/orders/$ORDER_ID" "$CUST2_TOKEN")
STATUS=$(parse_status "$RESP")
assert_status "Forbidden order access" 403 "$STATUS"

# ── 18. Validation error (empty body) ─────────────────────────────
step 18 "Error: validation (empty body)"
RESP=$(do_req POST "$BASE_URL/api/v1/auth/owner/register" "" '{}')
STATUS=$(parse_status "$RESP")
BODY=$(parse_body "$RESP")
assert_status "Validation error" 400 "$STATUS"
assert_json   "Error code" ".code" "VALIDATION_ERROR" "$BODY"

# ════════════════════════════════════════════════════════════════════
#  SUMMARY
# ════════════════════════════════════════════════════════════════════
echo
bold "══════════════════════════════════════════════════════"
bold "  RESULTS: $PASS passed, $FAIL failed"
bold "══════════════════════════════════════════════════════"

if [ "$FAIL" -gt 0 ]; then
  red "Some tests failed!"
  exit 1
else
  green "All tests passed!"
  exit 0
fi
