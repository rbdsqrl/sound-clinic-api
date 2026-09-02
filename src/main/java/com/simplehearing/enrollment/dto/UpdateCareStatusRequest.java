package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * The three manual-override fields are only honoured when {@code careStatus} is
 * PROGRAM_COMPLETED and the caller is admin-tier (see EnrollmentController) — they let that
 * force-complete action fill in the discharge success criteria that would otherwise never
 * arrive (goal mastery / parent satisfaction are normally computed from ongoing trial logs
 * and review meetings; therapist sign-off from the therapist's own confirmation).
 */
public record UpdateCareStatusRequest(
        @NotNull EnrollmentCareStatus careStatus,
        String note,
        @DecimalMin("0") @DecimalMax("100") Double manualGoalMasteryPct,
        @DecimalMin("0") @DecimalMax("100") Double manualParentSatisfactionPct,
        Boolean therapistSignedOff
) {
}
