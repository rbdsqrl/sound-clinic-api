package com.simplehearing.user.dto;

import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Gender;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record StaffMemberResponse(
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
        boolean faceEnrolled,
        int caseCount,
        Instant createdAt
) {
    public static StaffMemberResponse from(User user, int caseCount) {
        return new StaffMemberResponse(
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
                user.getFaceDescriptor() != null,
                caseCount,
                user.getCreatedAt()
        );
    }
}
