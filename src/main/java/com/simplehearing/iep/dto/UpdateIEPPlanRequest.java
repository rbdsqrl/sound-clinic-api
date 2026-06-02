package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPPlanStatus;

import java.time.LocalDate;
import java.util.List;

public record UpdateIEPPlanRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> tags,
        IEPPlanStatus status
) {}
