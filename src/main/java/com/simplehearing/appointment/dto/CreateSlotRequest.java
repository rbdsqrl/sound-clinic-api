package com.simplehearing.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSlotRequest(
        @NotNull UUID therapistId,
        @NotNull UUID clinicId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive @Max(480) int slotDurationMinutes
) {}
