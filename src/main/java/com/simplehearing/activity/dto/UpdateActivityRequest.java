package com.simplehearing.activity.dto;

import com.simplehearing.activity.enums.AgeUnit;

import java.util.List;
import java.util.UUID;

/** Every field is optional — only non-null fields are applied. Collection fields (instructions,
 *  checklist, skillIds, languageIds, propIds, links) fully replace the existing set when present. */
public record UpdateActivityRequest(
        String title,
        String aboutActivity,
        UUID programId,
        List<UUID> skillIds,
        List<UUID> languageIds,
        Integer durationWeeks,
        Integer ageMinValue,
        AgeUnit ageMinUnit,
        Integer ageMaxValue,
        AgeUnit ageMaxUnit,
        String difficulty,
        List<String> instructions,
        List<ChecklistQuestionInput> checklist,
        List<UUID> propIds,
        String tipsAndSuggestions,
        List<String> links,
        Boolean isShared,
        Boolean isActive
) {}
