package com.simplehearing.patient.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AddConditionRequest(
        @NotNull(message = "Condition ID is required")
        UUID conditionId,

        LocalDate diagnosedAt,
        String notes
) {}
