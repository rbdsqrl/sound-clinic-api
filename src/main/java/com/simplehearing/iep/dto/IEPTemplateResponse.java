package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPTemplate;
import com.simplehearing.iep.entity.IEPTemplateGoal;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record IEPTemplateResponse(
        UUID id,
        UUID orgId,
        String name,
        String description,
        List<String> tags,
        List<IEPTemplateGoalResponse> goals,
        int goalCount,
        Instant createdAt
) {
    public static IEPTemplateResponse from(IEPTemplate template, List<IEPTemplateGoal> goals) {
        List<String> tagList = (template.getTags() != null && !template.getTags().isBlank())
                ? Arrays.stream(template.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList())
                : List.of();

        List<IEPTemplateGoalResponse> goalResponses = goals.stream()
                .map(IEPTemplateGoalResponse::from)
                .collect(Collectors.toList());

        return new IEPTemplateResponse(
                template.getId(),
                template.getOrgId(),
                template.getName(),
                template.getDescription(),
                tagList,
                goalResponses,
                goalResponses.size(),
                template.getCreatedAt()
        );
    }
}
