package com.simplehearing.baseline.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BaselineReportResponse(
        UUID id,
        UUID patientId,
        String ageAtAdmission,
        String ageOnDate,
        String cdct,
        Instant createdAt,
        Instant updatedAt,
        /** Always all 13 fixed domains, in a stable display order, even when a domain has no
         *  baseline value or current entries logged yet. */
        List<BaselineDomainResponse> domains
) {
}
