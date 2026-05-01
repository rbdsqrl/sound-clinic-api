package com.simplehearing.auth.service;

import com.simplehearing.auth.dto.LoginResponse;
import com.simplehearing.auth.dto.RegisterRequest;
import com.simplehearing.auth.entity.RefreshToken;
import com.simplehearing.auth.repository.RefreshTokenRepository;
import com.simplehearing.auth.security.JwtProperties;
import com.simplehearing.auth.security.TokenService;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.dto.UserResponse;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
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

/**
 * Handles public organisation registration.
 * Creates an Organisation and its first BUSINESS_OWNER in a single transaction,
 * then issues tokens so the owner is immediately logged in.
 */
@Service
@Transactional
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(OrganisationRepository organisationRepository,
                               UserRepository userRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               TokenService tokenService,
                               JwtProperties jwtProperties,
                               PasswordEncoder passwordEncoder) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new organisation and its first BUSINESS_OWNER.
     * Returns a LoginResponse — the owner is immediately authenticated.
     */
    public LoginResponse register(RegisterRequest request) {
        if (organisationRepository.existsBySlug(request.slug())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "The slug '" + request.slug() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        // Create organisation
        Organisation org = new Organisation();
        org.setName(request.orgName());
        org.setSlug(request.slug());
        org.setContactEmail(request.email());
        organisationRepository.save(org);

        // Create first BUSINESS_OWNER — not linked to a specific clinic
        User owner = new User();
        owner.setOrgId(org.getId());
        owner.setEmail(request.email());
        owner.setFirstName(request.firstName());
        owner.setLastName(request.lastName());
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setActive(true);
        userRepository.save(owner);

        log.info("Organisation '{}' registered, owner: {}", org.getName(), owner.getEmail());

        // Issue tokens so the owner is immediately logged in
        String accessToken = tokenService.issueAccessToken(owner);
        String rawRefreshToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(owner.getId());
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpiry()));
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
                accessToken,
                rawRefreshToken,
                jwtProperties.getAccessTokenExpiry(),
                UserResponse.from(owner)
        );
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
