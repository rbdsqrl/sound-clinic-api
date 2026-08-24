package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityAssignment;
import com.simplehearing.activity.enums.AssignmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityAssignmentResponse(
        UUID id,
        UUID activityId,
        String activityTitle,
        UUID patientId,
        String patientName,
        UUID assignedBy,
        String assignedByName,
        UUID assignedTherapistId,
        String assignedTherapistName,
        AssignmentStatus status,
        LocalDate startDate,
        LocalDate dueDate,
        int attemptCount,
        Instant createdAt
) {
    public static ActivityAssignmentResponse from(
            ActivityAssignment a, String activityTitle, String patientName,
            String assignedByName, String assignedTherapistName, int attemptCount) {
        return new ActivityAssignmentResponse(
                a.getId(), a.getActivityId(), activityTitle, a.getPatientId(), patientName,
                a.getAssignedBy(), assignedByName, a.getAssignedTherapistId(), assignedTherapistName,
                a.getStatus(), a.getStartDate(), a.getDueDate(), attemptCount, a.getCreatedAt());
    }
}
