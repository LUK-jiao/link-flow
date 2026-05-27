package com.linkflow.gateway.auth;

import com.linkflow.api.dto.user.UserLoginResultDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final Duration expireDuration;

    public JwtTokenService(
            @Value("${linkflow.auth.jwt.secret}") String secret,
            @Value("${linkflow.auth.jwt.expire-minutes}") long expireMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireDuration = Duration.ofMinutes(expireMinutes);
    }

    public String generateToken(UserLoginResultDTO user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireDuration)))
                .signWith(secretKey)
                .compact();
    }

    public CurrentUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new CurrentUser(
                    claims.get("userId", Long.class),
                    claims.get("username", String.class),
                    claims.get("role", String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException();
        }
    }
}
