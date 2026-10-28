package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.enums.RenewalOutcome;
import jakarta.validation.constraints.NotNull;

public record RenewalOutcomeRequest(@NotNull RenewalOutcome outcome) {}
