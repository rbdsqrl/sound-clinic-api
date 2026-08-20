package com.simplehearing.common.dto;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.util.UUID;

/**
 * Someone attending a meeting — name and role only, no personal details.
 *
 * Shared by general meetings, where participants are stored explicitly, and review
 * meetings, where they are derived from the therapist and the patient's linked parents.
 */
public record ParticipantResponse(
        UUID id,
        String firstName,
        String lastName,
        Role role,
        /** The person the meeting is organised by, where there is one. */
        boolean isOrganiser
) {
    public static ParticipantResponse from(User user, boolean isOrganiser) {
        return new ParticipantResponse(
                user.getId(), user.getFirstName(), user.getLastName(), user.getRole(), isOrganiser);
    }
}
