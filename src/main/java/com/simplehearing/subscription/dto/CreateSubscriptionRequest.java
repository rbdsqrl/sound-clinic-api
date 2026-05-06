package com.simplehearing.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID patientId,
        @NotNull UUID programId,
        @Min(1) int numSessions,
        String notes
) {}
