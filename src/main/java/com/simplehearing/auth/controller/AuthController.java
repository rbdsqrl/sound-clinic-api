package com.simplehearing.auth.controller;

import com.simplehearing.auth.dto.*;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.auth.service.AuthService;
import com.simplehearing.auth.service.PasswordResetService;
import com.simplehearing.auth.service.RegistrationService;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication and token management")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          RegistrationService registrationService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Register a new clinic",
               description = "Creates a clinic and its first BUSINESS_OWNER account. Returns tokens — the owner is immediately logged in.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clinic registered successfully", registrationService.register(request)));
    }

    @Operation(summary = "Login with email and password", description = "Returns a short-lived access token and a 7-day refresh token.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new access token. The old refresh token is rotated.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @Operation(summary = "Logout", description = "Revokes the provided refresh token, ending the session.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request a password reset link",
               description = "Mails a single-use reset link to the address if it belongs to an active account. "
                           + "Always returns the same response so the endpoint cannot be used to discover registered emails.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                            HttpServletRequest servletRequest) {
        passwordResetService.requestReset(request, clientIp(servletRequest));
        return ResponseEntity.ok(ApiResponse.success(
                "If that email is registered, a password reset link is on its way.", null));
    }

    @Operation(summary = "Validate a password reset link",
               description = "Checks a reset token and returns the masked email it belongs to. 404 if unknown, 410 if expired or already used.")
    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResponse<ResetTokenPreviewResponse>> validateResetToken(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(passwordResetService.validateToken(token)));
    }

    @Operation(summary = "Set a new password",
               description = "Consumes the reset token, sets the new password, and revokes all existing sessions for that user.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password updated successfully. You can now sign in.", null));
    }

    @Operation(summary = "Get current user", description = "Returns the profile of the currently authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(principal.getUser())));
    }

    /** Behind a proxy the real caller is the first entry in X-Forwarded-For. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
