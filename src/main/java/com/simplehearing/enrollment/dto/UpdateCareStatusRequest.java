package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCareStatusRequest(
        @NotNull EnrollmentCareStatus careStatus,
        String note
) {
}
