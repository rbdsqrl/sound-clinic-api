package com.simplehearing.auth.security;

import com.simplehearing.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public TokenService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    /**
     * Issues a short-lived access token (HS256) embedding userId, orgId, clinicId and role as claims.
     * clinicId is omitted from the token when null (e.g. BUSINESS_OWNER has no clinic).
     */
    public String issueAccessToken(User user) {
        long now = System.currentTimeMillis();
        // Embed every role the user holds so Spring Security grants all of them
        String allRoles = user.getAllRoles().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(","));

        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim("orgId", user.getOrgId().toString())
                .claim("role", user.getRole().name())   // primary role (for activeRole default)
                .claim("roles", allRoles);              // comma-separated full set

        if (user.getClinicId() != null) {
            builder.claim("clinicId", user.getClinicId().toString());
        }

        return builder
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getAccessTokenExpiry() * 1_000L))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the token and returns its claims.
     * Throws {@link JwtException} if the token is invalid or expired.
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
