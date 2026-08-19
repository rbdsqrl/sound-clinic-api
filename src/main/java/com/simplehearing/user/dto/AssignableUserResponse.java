package com.simplehearing.user.dto;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.util.UUID;

/**
 * The bare minimum needed to pick someone as a task assignee — name, role, id.
 *
 * Deliberately leaner than {@link StaffMemberResponse}: every staff member can read this
 * list in order to assign a task, so it must not carry personal details such as phone,
 * date of birth, or face-enrolment state.
 */
public record AssignableUserResponse(
        UUID id,
        String firstName,
        String lastName,
        Role role
) {
    public static AssignableUserResponse from(User user) {
        return new AssignableUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
