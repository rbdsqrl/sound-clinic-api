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
        /** Share of finalised sessions (completed + no-show + cancelled) that were cancelled —
         *  same base as {@code attendancePct}, so the two read as complementary outcomes rather
         *  than each picking its own denominator. A still-upcoming session isn't an outcome yet. */
        Double cancelledPct,
        /** Share of ALL sessions in the window ever moved (reschedule_count > 0) — deliberately
         *  over the full total, not just finalised ones: a session can be rescheduled and still
         *  be sitting in SCHEDULED status today. */
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
