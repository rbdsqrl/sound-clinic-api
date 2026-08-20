package com.simplehearing.analytics.enums;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Bucket size for a analytics time series.
 *
 * <p>Bucketing runs on {@code LocalDate} throughout — {@code session_date} and
 * {@code meeting_date} are date-typed, so there is no timezone conversion to get wrong.
 * Never bucket on {@code created_at}: it is an {@code Instant} and would shift rows
 * across day boundaries depending on the server zone.
 */
public enum Granularity {

    DAILY,
    WEEKLY,
    MONTHLY;

    private static final DateTimeFormatter DAY_LABEL   = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    /** First day of the bucket the given date falls into. ISO-8601 weeks, so Monday starts the week. */
    public LocalDate bucketStart(LocalDate date) {
        return switch (this) {
            case DAILY   -> date;
            case WEEKLY  -> date.with(WeekFields.ISO.dayOfWeek(), 1);
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    /** Start of the bucket after the one beginning at {@code bucketStart}. */
    public LocalDate next(LocalDate bucketStart) {
        return switch (this) {
            case DAILY   -> bucketStart.plusDays(1);
            case WEEKLY  -> bucketStart.plusWeeks(1);
            case MONTHLY -> bucketStart.plusMonths(1);
        };
    }

    /** Short axis label — "18 Aug", "W34", "Aug 2026". */
    public String label(LocalDate bucketStart) {
        return switch (this) {
            case DAILY   -> bucketStart.format(DAY_LABEL);
            case WEEKLY  -> "W" + bucketStart.get(WeekFields.ISO.weekOfWeekBasedYear());
            case MONTHLY -> bucketStart.format(MONTH_LABEL);
        };
    }
}
