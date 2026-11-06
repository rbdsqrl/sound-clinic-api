package com.simplehearing.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID patientId,
        @NotNull UUID programId,
        @Min(1) int numSessions,
        String notes,

        /** Optional per-session fee override for this one case — the program's own price is
         *  unaffected. Omit to use the program's price (with tax) as before. */
        @DecimalMin(value = "0", inclusive = true) BigDecimal perSessionCost,

        /** The start date and weekdays chosen up front — kept so the enrollment step starts from the same schedule. */
        java.time.LocalDate plannedStartDate,
        java.util.Set<java.time.DayOfWeek> plannedSessionDays
) {}
