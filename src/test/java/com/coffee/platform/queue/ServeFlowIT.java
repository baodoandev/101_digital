package com.coffee.platform.queue;

import com.coffee.platform.AbstractIT;
import com.coffee.platform.customer.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class ServeFlowIT extends AbstractIT {

    @Autowired
    private CustomerRepository customerRepo;

    private String ownerToken;
    private Number shopId;
    private Number queueId;
    private Number menuItemId;

    @BeforeEach
    void setUpShop() {
        ownerToken = given()
                .contentType("application/json")
                .body("""
                        {"email":"serve-owner-%d@test.com","password":"secret123","displayName":"Serve Owner"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201)
                .extract().path("token");

        shopId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Serve Shop %d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00",
                         "avgServiceSeconds":120}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");

        menuItemId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Espresso %d","priceMinor":300}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201)
                .extract().path("id");

        queueId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"label":"Q1","maxSize":20}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/queues")
            .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    void serveNextFIFO() {
        String tokenA = registerCustomer();
        String tokenB = registerCustomer();

        Number orderIdA = placeOrder(tokenA);
        Number orderIdB = placeOrder(tokenB);

        // serve-next should return customer A's entry (FIFO)
        Number servedOrderId = given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .post("/api/v1/queues/" + queueId + "/serve-next")
            .then()
                .statusCode(200)
                .body("status", equalTo("SERVED"))
                .extract().path("orderId");

        assertThat(servedOrderId.longValue()).isEqualTo(orderIdA.longValue());
    }

    @Test
    void serveSpecific() {
        String tokenA = registerCustomer();
        String tokenB = registerCustomer();

        placeOrder(tokenA);
        Number orderIdB = placeOrder(tokenB);

        // Get entries to find entryId for B's order
        var entries = given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .get("/api/v1/queues/" + queueId + "/entries")
            .then()
                .statusCode(200)
                .extract().jsonPath();

        // Find the entry for orderIdB
        Number entryIdB = null;
        for (int i = 0; i < entries.getList("id").size(); i++) {
            Number oid = entries.getList("orderId", Number.class).get(i);
            if (oid.longValue() == orderIdB.longValue()) {
                entryIdB = entries.getList("id", Number.class).get(i);
                break;
            }
        }
        assertThat(entryIdB).isNotNull();

        // Serve specific entry (B, not FIFO order)
        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .post("/api/v1/queues/" + queueId + "/entries/" + entryIdB + "/serve")
            .then()
                .statusCode(200)
                .body("status", equalTo("SERVED"))
                .body("orderId", equalTo(orderIdB.intValue()));

        // Verify order B is FULFILLED
        given()
                .header("Authorization", "Bearer " + tokenB)
            .when()
                .get("/api/v1/orders/" + orderIdB)
            .then()
                .statusCode(200)
                .body("status", equalTo("FULFILLED"));
    }

    @Test
    void loyaltyIncremented() {
        var custResponse = given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+659%07d","name":"Loyal Cust"}
                        """.formatted(System.nanoTime() % 10000000))
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract();

        String custToken = custResponse.path("token");
        Number custId = custResponse.path("id");

        int loyaltyBefore = customerRepo.findById(custId.longValue())
                .orElseThrow().getLoyaltyScore();

        placeOrder(custToken);

        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .post("/api/v1/queues/" + queueId + "/serve-next")
            .then()
                .statusCode(200);

        int loyaltyAfter = customerRepo.findById(custId.longValue())
                .orElseThrow().getLoyaltyScore();

        assertThat(loyaltyAfter).isEqualTo(loyaltyBefore + 1);
    }

    @Test
    void serveEmptyQueue_returns404() {
        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .post("/api/v1/queues/" + queueId + "/serve-next")
            .then()
                .statusCode(404);
    }

    @Test
    void queueSnapshot() {
        String tokenA = registerCustomer();
        String tokenB = registerCustomer();

        placeOrder(tokenA);
        placeOrder(tokenB);

        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .get("/api/v1/queues/" + queueId)
            .then()
                .statusCode(200)
                .body("queueId", equalTo(queueId.intValue()))
                .body("label", equalTo("Q1"))
                .body("maxSize", equalTo(20))
                .body("waitingCount", equalTo(2));
    }

    private String registerCustomer() {
        return given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+659%07d","name":"Cust"}
                        """.formatted(System.nanoTime() % 10000000))
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract().path("token");
    }

    private Number placeOrder(String customerToken) {
        return given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(shopId, menuItemId))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201)
                .extract().path("id");
    }
}
