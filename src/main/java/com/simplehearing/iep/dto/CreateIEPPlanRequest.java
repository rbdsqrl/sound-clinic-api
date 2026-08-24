package com.simplehearing.iep.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateIEPPlanRequest(
        @NotBlank String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> tags,
        List<CreateIEPGoalRequest> goals,
        /** Which program this plan belongs to — lets goal mastery be attributed to the right enrollment. */
        UUID enrollmentId
) {}
