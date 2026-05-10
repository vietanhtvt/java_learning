package com.taskflow.security;

import com.taskflow.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UUID userId, String username) {
        return buildToken(userId, username, jwtProperties.expirationMs(), Map.of("type", "access"));
    }

    public String generateRefreshToken(UUID userId, String username) {
        return buildToken(userId, username, jwtProperties.refreshExpirationMs(), Map.of("type", "refresh"));
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String id = parseClaims(token).get("uid", String.class);
        return UUID.fromString(id);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }

    // --- private helpers ---

    private String buildToken(UUID userId, String username, long expirationMs, Map<String, ?> extraClaims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .claims(extraClaims)
            .claim("uid", userId.toString())
            .subject(username)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMs))
            .signWith(signingKey())
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(
                jwtProperties.secret().getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
