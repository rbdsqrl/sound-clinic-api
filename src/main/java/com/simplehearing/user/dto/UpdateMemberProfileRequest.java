package com.simplehearing.user.dto;

import java.util.List;
import java.util.UUID;

/** Editable fields on the member profile page — BUSINESS_OWNER / CLINIC_HEAD only. */
public record UpdateMemberProfileRequest(
        String phone,
        UUID clinicId,
        String qualification,
        String specialization,
        List<UUID> languageIds
) {}
