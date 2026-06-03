package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.enums.IEPPlanStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record IEPPlanResponse(
        UUID id,
        UUID orgId,
        UUID patientId,
        UUID therapistId,
        String therapistName,
        String patientName,
        String title,
        String startDate,
        String endDate,
        IEPPlanStatus status,
        List<String> tags,
        List<IEPGoalResponse> goals,
        int totalGoals,
        int completedGoals,
        Instant createdAt,
        Instant updatedAt
) {
    public static IEPPlanResponse from(IEPPlan plan, String therapistName,
                                       List<IEPGoalResponse> goals, int completedGoals) {
        return from(plan, therapistName, null, goals, completedGoals);
    }

    public static IEPPlanResponse from(IEPPlan plan, String therapistName, String patientName,
                                       List<IEPGoalResponse> goals, int completedGoals) {
        List<String> tagList = (plan.getTags() != null && !plan.getTags().isBlank())
                ? Arrays.stream(plan.getTags().split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList())
                : List.of();

        return new IEPPlanResponse(
                plan.getId(),
                plan.getOrgId(),
                plan.getPatientId(),
                plan.getTherapistId(),
                therapistName,
                patientName,
                plan.getTitle(),
                plan.getStartDate() != null ? plan.getStartDate().toString() : null,
                plan.getEndDate() != null ? plan.getEndDate().toString() : null,
                plan.getStatus(),
                tagList,
                goals,
                goals.size(),
                completedGoals,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
