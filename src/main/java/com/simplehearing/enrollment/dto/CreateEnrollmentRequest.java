package com.simplehearing.enrollment.dto;

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
        @NotNull LocalTime startTime
) {}
