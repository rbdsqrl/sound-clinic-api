package com.simplehearing.auth.dto;

import com.simplehearing.common.util.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email
) {
    /** Normalised before validation so a stray space or capital never reaches the lookup. */
    public ForgotPasswordRequest {
        email = EmailNormalizer.normalize(email);
    }
}
