package com.simplehearing.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Session cadence for one patient, across every concurrent enrollment — not just one program.
 * Additive companion to {@link TimeSeriesResponse}; kept separate since it folds across
 * enrollments rather than a single one.
 */
public record FrequencyResponse(
        List<WeeklyFrequency> weekly,
        List<ProgramTotal> byProgram
) {
    public record WeeklyFrequency(
            LocalDate weekStart,
            int totalSessions,
            int planSessions,
            int adHocSessions,
            List<ProgramCount> byProgram
    ) {}

    public record ProgramCount(String programName, int count) {}

    public record ProgramTotal(String programName, int totalSessions) {}
}
