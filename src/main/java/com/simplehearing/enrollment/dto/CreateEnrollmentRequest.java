package com.simplehearing.enrollment.dto;

import com.simplehearing.review.dto.ReviewScheduleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull UUID subscriptionId,
        @NotNull UUID patientId,
        @NotNull UUID therapistId,
        @Min(15) int sessionDurationMinutes,
        @NotNull LocalDate startDate,
        @NotNull LocalTime startTime,

        /** Optional — defaults to the date the last generated session falls on. */
        LocalDate endDate,

        /** Which weekdays sessions may land on. Omit/empty — every day is a candidate
         *  (skipping only holidays and the org's weekly off days), same as before this existed. */
        Set<DayOfWeek> sessionDays,

        /** Optional — omit to set up the therapy plan without any review meetings. */
        @Valid ReviewScheduleRequest reviewSchedule
) {}
