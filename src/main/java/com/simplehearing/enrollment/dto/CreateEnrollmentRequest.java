package com.simplehearing.enrollment.dto;

import com.simplehearing.review.dto.ReviewScheduleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
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

        /** Optional — omit to set up the therapy plan without any review meetings. */
        @Valid ReviewScheduleRequest reviewSchedule
) {}
