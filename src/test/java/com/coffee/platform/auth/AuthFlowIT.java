package com.coffee.platform.auth;

import com.coffee.platform.AbstractIT;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AuthFlowIT extends AbstractIT {

    @Test
    void ownerRegisterLoginAndMe() {
        // Register
        String token = given()
                .contentType("application/json")
                .body("""
                        {"email":"it-owner@test.com","password":"secret123","displayName":"IT Owner"}
                        """)
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("type", equalTo("USER"))
                .extract().path("token");

        // Me
        given()
                .header("Authorization", "Bearer " + token)
            .when()
                .get("/api/v1/auth/me")
            .then()
                .statusCode(200)
                .body("role", equalTo("OWNER"))
                .body("subject", equalTo("it-owner@test.com"));

        // Login
        given()
                .contentType("application/json")
                .body("""
                        {"email":"it-owner@test.com","password":"secret123"}
                        """)
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void customerRegisterLoginAndMe() {
        String token = given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+6599990001","name":"Test Customer"}
                        """)
            .when()
                .post("/api/v1/auth/customer/register")
            .then()
                .statusCode(201)
                .body("type", equalTo("CUSTOMER"))
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
            .when()
                .get("/api/v1/auth/me")
            .then()
                .statusCode(200)
                .body("role", equalTo("CUSTOMER"));

        // Login with stub OTP
        given()
                .contentType("application/json")
                .body("""
                        {"mobileNumber":"+6599990001","otp":"000000"}
                        """)
            .when()
                .post("/api/v1/auth/customer/login")
            .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void duplicateEmail_returns409() {
        given()
                .contentType("application/json")
                .body("""
                        {"email":"dup@test.com","password":"secret123","displayName":"First"}
                        """)
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(201);

        given()
                .contentType("application/json")
                .body("""
                        {"email":"dup@test.com","password":"secret123","displayName":"Second"}
                        """)
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(409)
                .body("code", equalTo("EMAIL_TAKEN"));
    }

    @Test
    void invalidCredentials_returns401() {
        given()
                .contentType("application/json")
                .body("""
                        {"email":"noexist@test.com","password":"wrong"}
                        """)
            .when()
                .post("/api/v1/auth/login")
            .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    void validationError_returns400() {
        given()
                .contentType("application/json")
                .body("""
                        {"email":"notanemail","password":"x","displayName":""}
                        """)
            .when()
                .post("/api/v1/auth/owner/register")
            .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("fieldErrors", not(empty()));
    }

    @Test
    void unauthenticatedMe_returns401() {
        given()
            .when()
                .get("/api/v1/auth/me")
            .then()
                .statusCode(401);
    }
}
