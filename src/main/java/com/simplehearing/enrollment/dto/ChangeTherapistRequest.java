package com.simplehearing.enrollment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Hands an ongoing therapy plan to a different therapist. */
public record ChangeTherapistRequest(
        @NotNull UUID therapistId,
        String reason
) {}
