package com.simplehearing.user.dto;

import com.simplehearing.activity.dto.LanguageResponse;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** The member profile page — a staff member's contact/qualification details, plus their caseload count. */
public record MemberProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        Set<Role> additionalRoles,
        boolean isActive,
        UUID clinicId,
        String clinicName,
        String qualification,
        String specialization,
        List<LanguageResponse> languages,
        int caseCount,
        Instant createdAt
) {
    public static MemberProfileResponse from(User user, String clinicName, List<LanguageResponse> languages, int caseCount) {
        return new MemberProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getAdditionalRoles(),
                user.isActive(),
                user.getClinicId(),
                clinicName,
                user.getQualification(),
                user.getSpecialization(),
                languages,
                caseCount,
                user.getCreatedAt()
        );
    }
}
