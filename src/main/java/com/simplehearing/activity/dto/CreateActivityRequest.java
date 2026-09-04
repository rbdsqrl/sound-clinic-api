package com.simplehearing.activity.dto;

import com.simplehearing.activity.enums.AgeUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateActivityRequest(
        @NotBlank String title,
        @NotBlank String aboutActivity,
        UUID programId,
        List<UUID> skillIds,
        List<UUID> languageIds,
        @NotNull @Min(1) Integer durationWeeks,
        @NotNull @Min(0) Integer ageMinValue,
        @NotNull AgeUnit ageMinUnit,
        @NotNull @Min(0) Integer ageMaxValue,
        @NotNull AgeUnit ageMaxUnit,
        String difficulty,
        List<String> instructions,
        List<ChecklistQuestionInput> checklist,
        List<UUID> propIds,
        String tipsAndSuggestions,
        List<String> links,
        /** Ids of items picked from the org-wide Resources library. Any id that doesn't exist,
         *  or belongs to another org, is silently skipped rather than rejected. */
        List<UUID> resourceIds,
        Boolean isShared
) {}
