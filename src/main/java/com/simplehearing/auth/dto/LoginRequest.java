package com.simplehearing.auth.dto;

import com.simplehearing.common.util.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    /** Normalised before validation so a stray space or capital never reaches the lookup. */
    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
