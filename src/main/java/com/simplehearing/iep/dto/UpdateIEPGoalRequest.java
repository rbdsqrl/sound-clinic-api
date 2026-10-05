package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;

import java.time.LocalDate;

public record UpdateIEPGoalRequest(
        String title,
        String goalStatement,
        IEPGoalDomain domain,
        /** Required when domain is CUSTOM. */
        String customDomain,
        String baseline,
        String targetCriteria,
        LocalDate targetDate,
        IEPGoalStatus status,
        String progressTag
) {}
