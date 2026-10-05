package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateIEPGoalRequest(
        @NotBlank String title,
        String goalStatement,
        @NotNull IEPGoalDomain domain,
        /** Required when domain is CUSTOM — the new or existing custom domain's name. */
        String customDomain,
        String baseline,
        String targetCriteria,
        LocalDate targetDate
) {}
