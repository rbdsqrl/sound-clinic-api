package com.simplehearing.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The Schedule tab's flat session log plus its KPI strip, both computed over the same filtered
 * set of sessions so the numbers above the table always match the rows below it. Percentages
 * are null (not zero) when there is nothing to divide by — see {@code AnalyticsService.pct}.
 */
public record ScheduleResponse(
        int totalSessions,
        Double cancelledPct,
        Double rescheduledPct,
        Double attendancePct,
        int totalDurationMinutes,
        Integer avgDurationMinutes,
        List<Entry> sessions
) {
    /** {@code cost} is the per-session rate actually charged on the enrollment's subscription
     *  at the time of purchase — null when the session isn't tied to a priced subscription. */
    public record Entry(
            UUID sessionId,
            LocalDate sessionDate,
            String startTime,
            int durationMinutes,
            String programName,
            String patientName,
            String therapistName,
            String status,
            BigDecimal cost
    ) {}
}
