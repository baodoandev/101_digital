package com.coffee.platform.shop;

import com.coffee.platform.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class ShopDataIT extends AbstractIT {

    private String ownerToken;

    @BeforeEach
    void registerOwner() {
        ownerToken = given()
                .contentType("application/json")
                .body("""
                        {"email":"shop-owner-%d@test.com","password":"secret123","displayName":"Shop Owner"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201)
                .extract().path("token");
    }

    @Test
    void createShopAndListIt() {
        Number shopId = given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Test Coffee","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00"}
                        """)
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .body("name", equalTo("Test Coffee"))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .get("/api/v1/shops")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .get("/api/v1/shops/" + shopId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Test Coffee"));
    }

    @Test
    void addMenuItemAndList() {
        Number shopId = createShop();

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Flat White","description":"Double shot","priceMinor":550}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201)
                .body("name", equalTo("Flat White"));

        given()
                .header("Authorization", "Bearer " + ownerToken)
            .when()
                .get("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void duplicateMenuItemName_returns409() {
        Number shopId = createShop();
        String menuBody = """
                {"name":"Latte","description":"Espresso + milk","priceMinor":500}
                """;

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body(menuBody)
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body(menuBody)
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_MENU_ITEM"));
    }

    @Test
    void addQueueAndMaxThreeLimit() {
        Number shopId = createShop();
        for (int i = 1; i <= 3; i++) {
            given()
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType("application/json")
                    .body("""
                            {"label":"Queue %d","maxSize":10}
                            """.formatted(i))
                .when()
                    .post("/api/v1/shops/" + shopId + "/queues")
                .then()
                    .statusCode(201);
        }

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"label":"Queue 4","maxSize":10}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/queues")
            .then()
                .statusCode(409)
                .body("code", equalTo("MAX_QUEUES_REACHED"));
    }

    @Test
    void nearestShopQuery() {
        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Nearby Coffee","country":"SG","latitude":1.2841,"longitude":103.8511,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00"}
                        """)
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .queryParam("lat", 1.2841)
                .queryParam("lng", 103.8511)
                .queryParam("radiusKm", 1)
                .queryParam("limit", 5)
            .when()
                .get("/api/v1/shops/nearest")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void assignOperator_nonExistentUser_returns404() {
        Number shopId = createShop();

        given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"userId":99999}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/operators")
            .then()
                .statusCode(404);
    }

    @Test
    void forbiddenForNonOwner() {
        Number shopId = createShop();

        String customerToken = given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+6599990099","name":"Cust"}
                        """)
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body("""
                        {"name":"Hack","priceMinor":100}
                        """)
            .when()
                .post("/api/v1/shops/" + shopId + "/menu-items")
            .then()
                .statusCode(403);
    }

    private Number createShop() {
        return given()
                .header("Authorization", "Bearer " + ownerToken)
                .contentType("application/json")
                .body("""
                        {"name":"S-%d","country":"SG","latitude":1.28,"longitude":103.85,
                         "timezone":"Asia/Singapore","currency":"SGD","openingTime":"07:00","closingTime":"19:00"}
                        """.formatted(System.nanoTime()))
            .when()
                .post("/api/v1/shops")
            .then()
                .statusCode(201)
                .extract().path("id");
    }
}
