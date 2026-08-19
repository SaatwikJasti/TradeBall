package com.tradeball.security;

import com.tradeball.config.JwtProperties;
import com.tradeball.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Set<String> DOCUMENTED_DEVELOPMENT_SECRETS = Set.of(
            "dev-only-secret-key-change-in-production-must-be-long-enough-256bits",
            "docker-compose-dev-secret-change-me-to-a-long-random-value-256bits",
            "test-secret-key-that-is-long-enough-for-hs256-signing-algorithms",
            "replace-with-a-random-secret-of-at-least-32-bytes"
    );

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Environment environment) {
        this.properties = properties;
        String secret = properties.secret();
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 bytes");
        }
        if (!environment.matchesProfiles("dev", "test") && DOCUMENTED_DEVELOPMENT_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET must not use a documented development/default value outside the dev or test profiles");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.expirationMs());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }
}
