package com.simplehearing.task.dto;

import com.simplehearing.task.entity.Task;
import com.simplehearing.task.enums.TaskPriority;
import com.simplehearing.task.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID orgId,
        String title,
        String description,
        UUID assignedTo,
        String assignedToFirstName,
        String assignedToLastName,
        UUID assignedBy,
        String assignedByFirstName,
        String assignedByLastName,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        int commentCount,
        int attachmentCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task t,
                                    String assignedToFirst, String assignedToLast,
                                    String assignedByFirst, String assignedByLast,
                                    int commentCount, int attachmentCount) {
        return new TaskResponse(
                t.getId(), t.getOrgId(), t.getTitle(), t.getDescription(),
                t.getAssignedTo(), assignedToFirst, assignedToLast,
                t.getAssignedBy(), assignedByFirst, assignedByLast,
                t.getDueDate(), t.getPriority(), t.getStatus(),
                commentCount, attachmentCount,
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
