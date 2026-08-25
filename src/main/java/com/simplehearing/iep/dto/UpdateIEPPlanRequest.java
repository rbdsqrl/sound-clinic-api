package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPPlanStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateIEPPlanRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> tags,
        IEPPlanStatus status,
        /** Assign/reassign the plan's therapist. Only a Business Owner or Clinic Head may set this. */
        UUID therapistId
) {}
