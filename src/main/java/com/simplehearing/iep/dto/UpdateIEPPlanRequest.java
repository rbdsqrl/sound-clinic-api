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
        UUID therapistId,
        /** Link the plan to one of the child's ongoing therapies. */
        UUID enrollmentId,
        /** True removes the plan's therapy link (enrollmentId is ignored when this is set). */
        Boolean unlinkEnrollment
) {}
