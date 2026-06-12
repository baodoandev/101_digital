package com.coffee.platform.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private Clock fixedClock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
        jwtService = new JwtService(SECRET, 3600, fixedClock);
    }

    @Test
    void generateAndParse_user() {
        String token = jwtService.generateToken(1L, "owner@chain.com", PrincipalType.USER, "OWNER");
        AuthPrincipal p = jwtService.parseToken(token);
        assertThat(p.id()).isEqualTo(1L);
        assertThat(p.subject()).isEqualTo("owner@chain.com");
        assertThat(p.type()).isEqualTo(PrincipalType.USER);
        assertThat(p.role()).isEqualTo("OWNER");
    }

    @Test
    void generateAndParse_customer() {
        String token = jwtService.generateToken(5L, "+6591110001", PrincipalType.CUSTOMER, "CUSTOMER");
        AuthPrincipal p = jwtService.parseToken(token);
        assertThat(p.id()).isEqualTo(5L);
        assertThat(p.type()).isEqualTo(PrincipalType.CUSTOMER);
        assertThat(p.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void expiredToken_throws() {
        JwtService pastService = new JwtService(SECRET, 3600,
                Clock.fixed(Instant.parse("2020-01-01T12:00:00Z"), ZoneOffset.UTC));
        String token = pastService.generateToken(1L, "test", PrincipalType.USER, "OWNER");
        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void invalidToken_throws() {
        assertThatThrownBy(() -> jwtService.parseToken("garbage.token.here"))
                .isInstanceOf(Exception.class);
    }
}
