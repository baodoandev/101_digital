package com.coffee.platform.queue;

import com.coffee.platform.AbstractIT;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class ServeConcurrencyIT extends AbstractIT {

    @Test
    void concurrentServeNext_neverDoubleServes() throws Exception {
        String ownerToken = registerOwner();
        Number shopId = createShop(ownerToken);
        addMenuItem(ownerToken, shopId);
        Number queueId = addQueue(ownerToken, shopId);

        // Place 6 orders from different customers
        for (int i = 0; i < 6; i++) {
            String ct = registerCustomer("+659" + String.format("%07d", System.nanoTime() % 10000000));
            placeOrder(ct, shopId);
        }

        // 2 threads each call serve-next 3 times = 6 total serves
        int threads = 2;
        int servesPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Long> servedEntryIds = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            futures.add(executor.submit(() -> {
                for (int s = 0; s < servesPerThread; s++) {
                    try {
                        Number entryId = given()
                                .header("Authorization", "Bearer " + ownerToken)
                            .when()
                                .post("/api/v1/queues/" + queueId + "/serve-next")
                            .then()
                                .statusCode(200)
                                .extract().path("entryId");
                        servedEntryIds.add(entryId.longValue());
                    } catch (Exception e) {
                        // might get 404 if queue empties faster than expected
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // All 6 entries served, all distinct (no double-serve)
        assertThat(servedEntryIds).hasSize(6);
        assertThat(servedEntryIds).doesNotHaveDuplicates();
    }

    private String registerOwner() {
        return given()
                .contentType("application/json")
                .body("""
                        {"email":"conc-owner-%d@test.com","password":"secret123","displayName":"Owner"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201)
                .extract().path("token");
    }

    private Number createShop(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"name":"Conc Shop %d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00",
                         "avgServiceSeconds":120}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");
    }

    private void addMenuItem(String token, Number shopId) {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"name":"Espresso %d","priceMinor":300}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201);
    }

    private Number addQueue(String token, Number shopId) {
        return given()
                .header("Authorization", "Bearer " + token)
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

    private String registerCustomer(String mobile) {
        return given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"%s","name":"Cust"}
                        """.formatted(mobile))
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract().path("token");
    }

    private void placeOrder(String token, Number shopId) {
        Number menuItemId = given()
                .header("Authorization", "Bearer " + token)
            .when()
                .get("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(200)
                .extract().jsonPath().getList("id", Number.class).get(0);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"shopId":%d,"items":[{"menuItemId":%d,"quantity":1}]}
                        """.formatted(shopId, menuItemId))
            .when()
                .post("/api/v1/orders")
            .then()
                .statusCode(201);
    }
}
