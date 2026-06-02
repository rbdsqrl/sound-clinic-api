package com.simplehearing.program.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProgramRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal perSessionCost
) {}
