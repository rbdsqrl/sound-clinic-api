package com.simplehearing.auth.service;

import com.simplehearing.auth.dto.ForgotPasswordRequest;
import com.simplehearing.auth.dto.ResetPasswordRequest;
import com.simplehearing.auth.dto.ResetTokenPreviewResponse;
import com.simplehearing.auth.entity.PasswordResetToken;
import com.simplehearing.auth.repository.PasswordResetTokenRepository;
import com.simplehearing.auth.repository.RefreshTokenRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.util.EmailNormalizer;
import com.simplehearing.common.util.TokenHasher;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** Deliberately shorter than an invitation — a reset link is a live credential sitting in an inbox. */
    private static final long EXPIRY_MINUTES = 60;

    private static final long RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final long MAX_REQUESTS_PER_EMAIL = 3;
    private static final long MAX_REQUESTS_PER_IP = 10;

    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                UserRepository userRepository,
                                OrganisationRepository organisationRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Issues a reset token and mails the link.
     *
     * Returns silently for unknown, inactive, and rate-limited addresses alike — the caller
     * always sees the same success message, so this endpoint cannot be used to discover
     * which emails belong to real accounts.
     */
    public void requestReset(ForgotPasswordRequest request, String requestIp) {
        // Matched exactly as login does — emails are stored as entered, never normalised.
        Optional<User> maybeUser = userRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .filter(User::isActive);

        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for unknown or inactive email — no mail sent");
            return;
        }

        User user = maybeUser.get();
        Instant windowStart = Instant.now().minus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES);

        if (tokenRepository.countByUserIdSince(user.getId(), windowStart) >= MAX_REQUESTS_PER_EMAIL) {
            log.warn("Password reset rate limit hit for user {}", user.getId());
            return;
        }
        if (requestIp != null
                && tokenRepository.countByRequestedIpSince(requestIp, windowStart) >= MAX_REQUESTS_PER_IP) {
            log.warn("Password reset rate limit hit for ip {}", requestIp);
            return;
        }

        // A new link invalidates any earlier unused one.
        tokenRepository.voidUnusedByUserId(user.getId(), Instant.now());

        String rawToken = TokenHasher.generateRawToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(TokenHasher.sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
        token.setRequestedIp(requestIp);
        tokenRepository.save(token);

        log.info("Password reset token issued for user {}", user.getId());

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                "/reset-password?token=" + rawToken,
                user.getFirstName(),
                orgNameFor(user),
                EXPIRY_MINUTES);
    }

    /** Validates a token for the reset page and returns the masked address it belongs to. */
    @Transactional(readOnly = true)
    public ResetTokenPreviewResponse validateToken(String token) {
        PasswordResetToken resetToken = requireValidToken(token);

        String email = userRepository.findById(resetToken.getUserId())
                .map(User::getEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "This reset link is no longer valid"));

        return new ResetTokenPreviewResponse(maskEmail(email));
    }

    /**
     * Sets the new password, spends the token, and revokes every existing session —
     * a reset is often being used to evict someone, so leaving refresh tokens alive would defeat it.
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        PasswordResetToken resetToken = requireValidToken(request.token());

        User user = userRepository.findById(resetToken.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "This reset link is no longer valid"));

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);

        refreshTokenRepository.deleteAllByUserId(user.getId());

        log.info("Password reset completed for user {} — all sessions revoked", user.getId());

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName(), orgNameFor(user));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PasswordResetToken requireValidToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(TokenHasher.sha256(token))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid or unknown reset link"));

        // Covers both a spent link and one superseded by a newer request.
        if (resetToken.getUsedAt() != null) {
            throw new ApiException(HttpStatus.GONE,
                    "This reset link is no longer valid. Request a new one to continue.");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.GONE, "This reset link has expired");
        }
        return resetToken;
    }

    private String orgNameFor(User user) {
        return organisationRepository.findById(user.getOrgId())
                .map(o -> o.getName())
                .orElse("Simple Hearing");
    }

    /** jane.smith@clinic.com → j&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;h@clinic.com */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return email;

        String local = email.substring(0, at);
        String domain = email.substring(at);

        if (local.length() <= 2) {
            return local.charAt(0) + "•••" + domain;
        }
        return local.charAt(0)
                + "•".repeat(Math.min(local.length() - 2, 8))
                + local.charAt(local.length() - 1)
                + domain;
    }
}
