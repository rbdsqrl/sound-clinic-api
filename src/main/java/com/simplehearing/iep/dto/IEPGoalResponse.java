package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;

import java.time.Instant;
import java.util.UUID;

public record IEPGoalResponse(
        UUID id,
        UUID orgId,
        UUID planId,
        String title,
        String goalStatement,
        IEPGoalDomain domain,
        String baseline,
        String targetCriteria,
        String targetDate,
        IEPGoalStatus status,
        String progressTag,
        UUID assignedTherapistId,
        String therapistName,
        int progressCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static IEPGoalResponse from(IEPGoal goal, String therapistName, int progressCount) {
        return new IEPGoalResponse(
                goal.getId(),
                goal.getOrgId(),
                goal.getPlanId(),
                goal.getTitle(),
                goal.getGoalStatement(),
                goal.getDomain(),
                goal.getBaseline(),
                goal.getTargetCriteria(),
                goal.getTargetDate() != null ? goal.getTargetDate().toString() : null,
                goal.getStatus(),
                goal.getProgressTag(),
                goal.getAssignedTherapistId(),
                therapistName,
                progressCount,
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
