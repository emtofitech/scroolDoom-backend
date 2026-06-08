package com.scrolldoom.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @PostConstruct
    public void validateConfig() {
        if (secret == null || secret.isBlank()) {
            log.error("JWT_SECRET environment variable is not set. Application cannot start.");
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        if (secret.length() < 32) {
            log.error("JWT_SECRET must be at least 32 characters long for HS256");
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        log.info("JWT secret configured successfully");
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String firebaseUid, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(firebaseUid)
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String firebaseUid, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(firebaseUid)
                .claim("email", email)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractFirebaseUid(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
