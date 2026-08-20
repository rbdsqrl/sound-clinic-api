package com.simplehearing.invitation.dto;

import com.simplehearing.user.enums.Role;
import com.simplehearing.common.util.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email,

        @NotNull(message = "Role is required")
        Role role,

        /**
         * Required for THERAPIST and PARENT invitations.
         * Omit (or null) for BUSINESS_OWNER invitations.
         */
        UUID clinicId
) {
    /** Normalised before validation so a stray space or capital never reaches the lookup. */
    public InviteRequest {
        email = EmailNormalizer.normalize(email);
    }
}
