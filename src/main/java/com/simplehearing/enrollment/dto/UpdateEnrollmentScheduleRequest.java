package com.simplehearing.enrollment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * Edits an ongoing plan's schedule — a new start time and/or which weekdays sessions land on,
 * effective from a chosen date, optionally also handing the plan to a different therapist in
 * the same call. Sessions before {@code effectiveDate} are untouched; sessions on/after it are
 * re-dated onto the new pattern. All fields but {@code effectiveDate} are optional — at least
 * one of startTime/sessionDays/therapistId must be set, or there's nothing to change.
 */
public record UpdateEnrollmentScheduleRequest(
        @NotNull LocalDate effectiveDate,
        LocalTime startTime,
        Set<DayOfWeek> sessionDays,
        UUID therapistId,
        String reason
) {}
