package com.simplehearing.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A therapist's whole caseload: the therapist-level series, plus one row per patient so
 * a stalled child is visible without opening every record.
 */
public record CaseloadResponse(
        UUID   therapistId,
        String therapistName,
        LocalDate from,
        LocalDate to,
        TimeSeriesResponse series,
        List<PatientRow>   patients,
        /** Children on this therapist's caseload, grouped by therapy/program — mirrors
         *  {@link OrgSnapshotResponse#programBreakdown()} but scoped to one therapist. */
        List<OrgSnapshotResponse.ProgramBreakdown> programBreakdown
) {

    /**
     * @param spark mastery per bucket, aligned to {@code series.buckets()} — nulls preserved
     *              so the sparkline breaks where data is missing.
     */
    public record PatientRow(
            UUID         patientId,
            String       patientName,
            Double       masteryPct,
            Double       deltaPts,
            List<Double> spark,
            int          sessionsCompleted,
            int          sessionsScheduled,
            int          sessionsNoShow,
            Double       coveragePct,
            int          goalsTotal,
            int          goalsCompleted,
            boolean      plateau
    ) {}
}
