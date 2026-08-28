package com.simplehearing.baseline.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddBaselineProgressRequest(
        @NotNull LocalDate entryDate,
        @NotBlank String value,
        /** Optional 0-100 score alongside the free-text value, so this entry can be charted. */
        @Min(value = 0, message = "scorePercent must be between 0 and 100")
        @Max(value = 100, message = "scorePercent must be between 0 and 100")
        Integer scorePercent
) {}
