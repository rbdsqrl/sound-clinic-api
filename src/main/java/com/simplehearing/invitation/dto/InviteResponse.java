package com.simplehearing.invitation.dto;

import com.simplehearing.invitation.entity.Invitation;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        String email,
        Role role,
        Invitation.Status status,
        Instant expiresAt,
        Instant createdAt,
        /** Null when returned from the list endpoint — raw token is never stored. */
        String acceptLink,
        /** Name of the clinic the invite is scoped to; null for BUSINESS_OWNER invites. */
        String clinicName
) {
    public static InviteResponse from(Invitation invitation, String acceptLink, String clinicName) {
        return new InviteResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                acceptLink,
                clinicName
        );
    }
}
