package com.simplehearing.discharge.dto;

import com.simplehearing.successcriteria.dto.SuccessCriteriaResponse;

import java.util.List;
import java.util.UUID;

/** A dry run of what discharging this patient right now would look like — no data is written. */
public record DischargePreviewResponse(
        List<EnrollmentPreview> enrollments,
        boolean allCriteriaMet
) {
    public record EnrollmentPreview(
            UUID enrollmentId, String programName, String therapistName, SuccessCriteriaResponse criteria) {}
}
