package com.simplehearing.assessment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record CreateAssessmentRequest(
        @NotNull LocalDate assessmentDate,
        @NotEmpty Map<Integer, Integer> itemScores
) {}
