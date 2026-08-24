package com.simplehearing.activity.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MagicFillRequest(
        @NotBlank String title,
        String aboutActivity,
        String therapyName,
        List<String> skillNames,
        Integer ageMinValue,
        String ageMinUnit,
        Integer ageMaxValue,
        String ageMaxUnit,
        String difficulty,
        @NotBlank String section // "instructions" | "checklist"
) {}
