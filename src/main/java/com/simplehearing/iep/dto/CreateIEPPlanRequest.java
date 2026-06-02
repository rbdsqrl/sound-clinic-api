package com.simplehearing.iep.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreateIEPPlanRequest(
        @NotBlank String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> tags,
        List<CreateIEPGoalRequest> goals
) {}
