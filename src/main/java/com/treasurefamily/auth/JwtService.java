package com.treasurefamily.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AuthProperties properties;
    private final SecretKey key;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(secretBytes(properties.getJwtSecret()));
    }

    public String createToken(String userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.tokenTtlSeconds());
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long expiresInSeconds() {
        return properties.tokenTtlSeconds();
    }

    private static byte[] secretBytes(String secret) {
        String raw = (secret == null || secret.isBlank())
                ? "dev-only-change-me-please-use-jwt-secret-env"
                : secret;
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("无法初始化登录密钥", ex);
        }
    }
}
