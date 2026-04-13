package com.simplehearing.auth.service;

import com.simplehearing.auth.dto.*;
import com.simplehearing.auth.entity.RefreshToken;
import com.simplehearing.auth.repository.RefreshTokenRepository;
import com.simplehearing.auth.security.JwtProperties;
import com.simplehearing.auth.security.TokenService;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.user.dto.UserResponse;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TokenService tokenService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive. Please contact your administrator.");
        }

        String accessToken = tokenService.issueAccessToken(user);
        String rawRefreshToken = generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpiry()));
        refreshTokenRepository.save(refreshToken);

        log.info("User {} logged in from clinic {}", user.getId(), user.getClinicId());

        return new LoginResponse(
                accessToken,
                rawRefreshToken,
                jwtProperties.getAccessTokenExpiry(),
                UserResponse.from(user)
        );
    }

    public RefreshResponse refresh(RefreshRequest request) {
        String tokenHash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or unknown refresh token"));

        if (stored.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found or inactive"));

        // Rotate: revoke old token, issue new one
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        String newRawToken = generateRawToken();
        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(user.getId());
        newToken.setTokenHash(sha256(newRawToken));
        newToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpiry()));
        refreshTokenRepository.save(newToken);

        return new RefreshResponse(
                tokenService.issueAccessToken(user),
                newRawToken,
                jwtProperties.getAccessTokenExpiry()
        );
    }

    public void logout(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
            t.setRevoked(true);
            t.setRevokedAt(Instant.now());
            refreshTokenRepository.save(t);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String generateRawToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
