package com.simplehearing.activity.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AssignActivityRequest(
        @NotNull UUID patientId,
        UUID assignedTherapistId,
        LocalDate startDate
) {}
