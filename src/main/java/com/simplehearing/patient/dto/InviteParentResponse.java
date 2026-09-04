package com.simplehearing.patient.dto;

import com.simplehearing.user.enums.Role;

import java.util.UUID;

/**
 * Either an invite was sent (inviteLink set), or the email already belongs to an active account
 * in this organisation (existingUser set, inviteLink null) — no invite is sent in that case.
 * The caller confirms with the user before POSTing that id to
 * POST /patients/{id}/parents/link-existing-user to grant them Parent access.
 */
public record InviteParentResponse(String inviteLink, ExistingUserSummary existingUser) {

    public record ExistingUserSummary(UUID id, String firstName, String lastName, Role role) {}

    public static InviteParentResponse invited(String inviteLink) {
        return new InviteParentResponse(inviteLink, null);
    }

    public static InviteParentResponse existingUser(ExistingUserSummary summary) {
        return new InviteParentResponse(null, summary);
    }
}
