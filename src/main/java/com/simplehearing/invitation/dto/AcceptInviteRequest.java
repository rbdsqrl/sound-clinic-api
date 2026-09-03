package com.simplehearing.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "First name is required")
        String firstName,

        String lastName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
