package com.simplehearing.patient.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTherapistRequest(
        @NotNull(message = "Therapist user ID is required")
        UUID therapistId
) {}
