package com.simplehearing.task.dto;

import com.simplehearing.task.enums.TaskPriority;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTaskRequest(
        String title,
        String description,
        List<UUID> assignedTo,
        LocalDate dueDate,
        TaskPriority priority
) {}
