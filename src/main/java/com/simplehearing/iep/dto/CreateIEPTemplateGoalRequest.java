package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import jakarta.validation.constraints.NotBlank;

public record CreateIEPTemplateGoalRequest(
        @NotBlank String title,
        String goalStatement,
        IEPGoalDomain domain,
        String baseline,
        String targetCriteria
) {}
