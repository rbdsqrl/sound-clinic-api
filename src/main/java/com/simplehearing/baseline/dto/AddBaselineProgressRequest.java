package com.simplehearing.baseline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddBaselineProgressRequest(
        @NotNull LocalDate entryDate,
        @NotBlank String value
) {}
