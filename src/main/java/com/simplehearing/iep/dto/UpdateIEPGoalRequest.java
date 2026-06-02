package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;

import java.time.LocalDate;

public record UpdateIEPGoalRequest(
        String title,
        String goalStatement,
        IEPGoalDomain domain,
        String baseline,
        String targetCriteria,
        LocalDate targetDate,
        IEPGoalStatus status,
        String progressTag
) {}
