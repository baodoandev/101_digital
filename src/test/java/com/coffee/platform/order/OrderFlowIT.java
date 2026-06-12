package com.coffee.platform.order;

import com.coffee.platform.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class OrderFlowIT extends AbstractIT {

    private String ownerToken;
    private String customerToken;
    private Number shopId;
    private Number menuItemId1;
    private Number menuItemId2;

    @BeforeEach
    void setUpShopAndCustomer() {
        // Register owner
        ownerToken = given()
                .contentType("application/json")
                .body("""
                        {"email":"order-owner-%d@test.com","password":"secret123","displayName":"Order Owner"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201)
                .extract().path("token");

        // Create shop (opening 07:00, closing 19:00, Asia/Singapore)
        // Fixed clock = 2025-01-15T04:00:00Z = 12:00 Singapore → shop is open
        shopId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Order Shop %d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00",
                         "avgServiceSeconds":120}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Add menu items
        menuItemId1 = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Latte %d","description":"Espresso + milk","priceMinor":550}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201)
                .extract().path("id");

        menuItemId2 = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Mocha %d","description":"Chocolate espresso","priceMinor":650}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Add queue
        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"label":"Main Queue","maxSize":10}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/queues")
            .then()
                .statusCode(201);

        // Register customer
        customerToken = given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+65%d","name":"Test Cust"}
                        """.formatted(System.nanoTime() % 100000000L))
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract().path("token");
    }

    @Test
    void placeOrderAndGetIt() {
        // Place order: 2x Latte(550) + 1x Mocha(650) = 1100 + 650 = 1750
        Number orderId = given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[
                            {"menuItemId":%d,"quantity":2},
                            {"menuItemId":%d,"quantity":1}
                        ]}
                        """.formatted(shopId, menuItemId1, menuItemId2))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("WAITING"))
                .body("totalMinor", equalTo(1750))
                .body("currency", equalTo("SGD"))
                .body("items.size()", equalTo(2))
                .extract().path("id");

        // GET the order
        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .get("/api/v1/orders/" + orderId)
            .then()
                .statusCode(200)
                .body("id", equalTo(orderId.intValue()))
                .body("status", equalTo("WAITING"))
                .body("totalMinor", equalTo(1750))
                .body("items.size()", equalTo(2));
    }

    @Test
    void queuePosition() {
        Number orderId = placeDefaultOrder();

        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .get("/api/v1/orders/" + orderId + "/queue-position")
            .then()
                .statusCode(200)
                .body("orderId", equalTo(orderId.intValue()))
                .body("position", equalTo(1))
                .body("etaSeconds", equalTo(120)); // 1 * 120
    }

    @Test
    void cancelOrder() {
        Number orderId = placeDefaultOrder();

        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .delete("/api/v1/orders/" + orderId)
            .then()
                .statusCode(200);

        // Verify cancelled
        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .get("/api/v1/orders/" + orderId)
            .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    void cancelAlreadyCancelledOrder_returns409() {
        Number orderId = placeDefaultOrder();

        // Cancel first time
        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .delete("/api/v1/orders/" + orderId)
            .then()
                .statusCode(200);

        // Cancel again
        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .delete("/api/v1/orders/" + orderId)
            .then()
                .statusCode(409)
                .body("code", equalTo("INVALID_STATE"));
    }

    @Test
    void idempotency() {
        String idemKey = "idem-" + System.nanoTime();

        Number orderId1 = given()
                .header("Authorization", "Bearer " + customerToken)
                .header("Idempotency-Key", idemKey)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(shopId, menuItemId1))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201)
                .extract().path("id");

        Number orderId2 = given()
                .header("Authorization", "Bearer " + customerToken)
                .header("Idempotency-Key", idemKey)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(shopId, menuItemId1))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Same order returned
        org.assertj.core.api.Assertions.assertThat(orderId1.intValue()).isEqualTo(orderId2.intValue());
    }

    @Test
    void shopClosed_returns409() {
        // Create a shop with narrow hours: 01:00-02:00 UTC
        // Fixed clock at 04:00 UTC → outside this window
        Number closedShopId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Closed Shop %d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"UTC","currency":"SGD","openingTime":"01:00","closingTime":"02:00"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Add menu item to the closed shop
        Number closedMenuItem = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Drip %d","priceMinor":300}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + closedShopId + "/menu-items")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Add queue
        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"label":"Q1","maxSize":5}
                        """)
            .when()
                .post("/api/v1/shops/" + closedShopId + "/queues")
            .then()
                .statusCode(201);

        // Try to place order → should fail with SHOP_CLOSED
        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(closedShopId, closedMenuItem))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(409)
                .body("code", equalTo("SHOP_CLOSED"));
    }

    @Test
    void queueFull_returns409() {
        // Create shop with a tiny queue (maxSize=1)
        Number tinyShopId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Tiny Shop %d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");

        Number tinyMenuItem = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Espresso %d","priceMinor":400}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + tinyShopId + "/menu-items")
            .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"label":"Tiny Queue","maxSize":1}
                        """)
            .when()
                .post("/api/v1/shops/" + tinyShopId + "/queues")
            .then()
                .statusCode(201);

        // First order fills the queue
        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(tinyShopId, tinyMenuItem))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201);

        // Second order → QUEUE_FULL
        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(tinyShopId, tinyMenuItem))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(409)
                .body("code", equalTo("QUEUE_FULL"));
    }

    @Test
    void customerCannotSeeOthersOrder() {
        Number orderId = placeDefaultOrder();

        // Register second customer
        String otherToken = given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+65%d","name":"Other Cust"}
                        """.formatted(System.nanoTime() % 100000000L))
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + otherToken)
            .when()
                .get("/api/v1/orders/" + orderId)
            .then()
                .statusCode(403);
    }

    @Test
    void myOrders() {
        placeDefaultOrder();
        placeDefaultOrder();

        given()
                .header("Authorization", "Bearer " + customerToken)
            .when()
                .get("/api/v1/customers/me/orders")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    private Number placeDefaultOrder() {
        return given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(shopId, menuItemId1))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201)
                .extract().path("id");
    }
}
