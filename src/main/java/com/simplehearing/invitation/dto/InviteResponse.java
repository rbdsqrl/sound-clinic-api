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
    /**
     * The stored status is only moved to EXPIRED when somebody tries to accept a lapsed
     * invitation, so a pending row that nobody touched keeps reading PENDING forever.
     * Derive the effective status here instead of persisting it: leaving the row PENDING
     * is what lets it still be resent.
     */
    private static Invitation.Status effectiveStatus(Invitation invitation) {
        if (invitation.getStatus() == Invitation.Status.PENDING
                && invitation.getExpiresAt() != null
                && invitation.getExpiresAt().isBefore(Instant.now())) {
            return Invitation.Status.EXPIRED;
        }
        return invitation.getStatus();
    }

    public static InviteResponse from(Invitation invitation, String acceptLink, String clinicName) {
        return new InviteResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                effectiveStatus(invitation),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                acceptLink,
                clinicName
        );
    }
}
