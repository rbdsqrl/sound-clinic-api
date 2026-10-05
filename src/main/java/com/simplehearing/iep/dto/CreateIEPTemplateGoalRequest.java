package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import jakarta.validation.constraints.NotBlank;

public record CreateIEPTemplateGoalRequest(
        @NotBlank String title,
        String goalStatement,
        IEPGoalDomain domain,
        /** Required when domain is CUSTOM. */
        String customDomain,
        String baseline,
        String targetCriteria
) {}
