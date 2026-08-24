package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.Activity;
import com.simplehearing.activity.enums.ActivityDifficulty;
import com.simplehearing.activity.enums.AgeUnit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID orgId,
        String orgName,
        boolean mine,
        String title,
        String aboutActivity,
        UUID therapyId,
        String therapyName,
        List<SkillResponse> skills,
        List<LanguageResponse> languages,
        Integer durationWeeks,
        Integer ageMinValue,
        AgeUnit ageMinUnit,
        Integer ageMaxValue,
        AgeUnit ageMaxUnit,
        ActivityDifficulty difficulty,
        List<String> instructions,
        List<ChecklistQuestionResponse> checklist,
        List<PropResponse> props,
        String tipsAndSuggestions,
        List<ActivityResourceResponse> resources,
        List<String> links,
        boolean isShared,
        UUID sourceActivityId,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static ActivityResponse from(
            Activity a, String orgName, boolean mine, String therapyName,
            List<SkillResponse> skills, List<LanguageResponse> languages,
            List<String> instructions, List<ChecklistQuestionResponse> checklist,
            List<PropResponse> props, List<ActivityResourceResponse> resources, List<String> links) {
        return new ActivityResponse(
                a.getId(), a.getOrgId(), orgName, mine,
                a.getTitle(), a.getAboutActivity(), a.getTherapyId(), therapyName,
                skills, languages,
                a.getDurationWeeks(), a.getAgeMinValue(), a.getAgeMinUnit(), a.getAgeMaxValue(), a.getAgeMaxUnit(),
                a.getDifficulty(), instructions, checklist, props, a.getTipsAndSuggestions(),
                resources, links, a.isShared(), a.getSourceActivityId(), a.isActive(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
