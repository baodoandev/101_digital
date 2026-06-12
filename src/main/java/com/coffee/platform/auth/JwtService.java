package com.coffee.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiresSeconds;
    private final Clock clock;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expires-seconds}") long expiresSeconds,
                      Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresSeconds = expiresSeconds;
        this.clock = clock;
    }

    public String generateToken(Long id, String subject, PrincipalType type, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(subject)
                .claim("id", id)
                .claim("type", type.name())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresSeconds)))
                .signWith(key)
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthPrincipal(
                claims.get("id", Long.class),
                claims.getSubject(),
                PrincipalType.valueOf(claims.get("type", String.class)),
                claims.get("role", String.class)
        );
    }
}
