package com.simplehearing.user.dto;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Gender;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID orgId,
        UUID clinicId,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        Role role,
        Set<Role> additionalRoles,
        boolean isActive,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getOrgId(),
                user.getClinicId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getRole(),
                user.getAdditionalRoles(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
