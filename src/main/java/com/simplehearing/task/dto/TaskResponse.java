package com.simplehearing.task.dto;

import com.simplehearing.task.entity.Task;
import com.simplehearing.task.enums.TaskPriority;
import com.simplehearing.task.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID orgId,
        String title,
        String description,
        List<AssigneeInfo> assignees,
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
    public record AssigneeInfo(UUID id, String firstName, String lastName) {}
}
