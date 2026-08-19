package com.simplehearing.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The review-meeting schedule captured while setting up a therapy plan.
 * Optional — an enrollment can be created without any review meetings.
 */
public record ReviewScheduleRequest(
        @NotNull LocalTime startTime,

        @Min(15) @Max(240)
        int durationMinutes,

        /** Weeks between meetings. Defaults to a fortnightly rhythm. */
        @Min(1) @Max(26)
        int intervalWeeks,

        /** First meeting date. Falls back to one interval after the therapy start date. */
        LocalDate firstMeetingDate,

        /** Last date meetings may fall on. Falls back to the enrollment's end date. */
        LocalDate endDate
) {
    public static final int DEFAULT_INTERVAL_WEEKS = 2;
    public static final int DEFAULT_DURATION_MINUTES = 30;

    public int intervalWeeksOrDefault() {
        return intervalWeeks > 0 ? intervalWeeks : DEFAULT_INTERVAL_WEEKS;
    }

    public int durationMinutesOrDefault() {
        return durationMinutes > 0 ? durationMinutes : DEFAULT_DURATION_MINUTES;
    }
}
