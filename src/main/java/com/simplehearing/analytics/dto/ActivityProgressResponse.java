package com.simplehearing.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/** Lightweight, additive companion to {@link TimeSeriesResponse} — activity assignment /
 *  attempt counts for a patient. Kept separate from the IEP mastery-trend folding logic. */
public record ActivityProgressResponse(
        int assignedCount,
        int inProgressCount,
        int completedCount,
        int discontinuedCount,
        Double completionRatePct,
        List<WeeklyAttemptPoint> weeklyAttempts
) {
    public record WeeklyAttemptPoint(LocalDate weekStart, int attempts) {}
}
