package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPTemplateGoal;

import java.time.Instant;
import java.util.UUID;

public record IEPTemplateGoalResponse(
        UUID id,
        UUID templateId,
        UUID orgId,
        String title,
        String goalStatement,
        String domain,
        String baseline,
        String targetCriteria,
        Instant createdAt
) {
    public static IEPTemplateGoalResponse from(IEPTemplateGoal goal) {
        return new IEPTemplateGoalResponse(
                goal.getId(),
                goal.getTemplateId(),
                goal.getOrgId(),
                goal.getTitle(),
                goal.getGoalStatement(),
                goal.getDomain() != null ? goal.getDomain().name() : null,
                goal.getBaseline(),
                goal.getTargetCriteria(),
                goal.getCreatedAt()
        );
    }
}
