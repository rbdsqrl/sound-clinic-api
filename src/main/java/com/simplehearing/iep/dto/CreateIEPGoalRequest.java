package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateIEPGoalRequest(
        @NotBlank String title,
        String goalStatement,
        @NotNull IEPGoalDomain domain,
        String baseline,
        String targetCriteria,
        LocalDate targetDate
) {}
