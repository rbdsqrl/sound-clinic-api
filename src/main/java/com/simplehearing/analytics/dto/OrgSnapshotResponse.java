package com.simplehearing.analytics.dto;

import com.simplehearing.patient.enums.PatientStage;

import java.util.List;

/**
 * Org-wide clinical-outcome rollups that don't fit the bucketed time-series shape used
 * elsewhere in this module — a duration average, a per-program breakdown, and a funnel
 * snapshot are all "right now" figures, not a trend over a window.
 */
public record OrgSnapshotResponse(
        /** Average span (start date to end date) across enrollments that have an end date, in weeks. Null with no data. */
        Double avgTherapyDurationWeeks,
        /** How many enrollments the average above is built from — so a thin sample reads as thin. */
        int enrollmentsWithDuration,
        List<ProgramBreakdown> programBreakdown,
        List<StageCount> stageCounts
) {
    public record ProgramBreakdown(String programName, int patientCount, int enrollmentCount) {}

    public record StageCount(PatientStage stage, int count) {}
}
