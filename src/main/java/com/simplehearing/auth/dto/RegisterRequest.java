package com.simplehearing.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        // ── Organisation details ──────────────────────────────────────────────
        @NotBlank(message = "Organisation name is required")
        String orgName,

        /**
         * URL-safe slug, e.g. "city-hearing". Unique across all organisations.
         * Lowercase letters, digits, and hyphens only.
         */
        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]{3,50}$",
                 message = "Slug must be 3–50 characters: lowercase letters, digits, and hyphens only")
        String slug,

        // ── Business owner details ────────────────────────────────────────────
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
