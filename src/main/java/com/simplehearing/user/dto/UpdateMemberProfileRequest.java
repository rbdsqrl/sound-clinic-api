package com.simplehearing.user.dto;

import com.simplehearing.user.enums.Role;

import java.util.List;
import java.util.UUID;

/**
 * Editable fields on the member profile page — BUSINESS_OWNER / CLINIC_HEAD only.
 *
 * @param role Changing this is BUSINESS_OWNER-only (not CLINIC_HEAD) and restricted to the
 *             staff roles — never PARENT/PATIENT. Null leaves the role unchanged.
 */
public record UpdateMemberProfileRequest(
        String phone,
        UUID clinicId,
        String qualification,
        String specialization,
        List<UUID> languageIds,
        Role role
) {}
