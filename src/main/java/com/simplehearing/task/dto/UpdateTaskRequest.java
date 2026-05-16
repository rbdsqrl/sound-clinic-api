package com.simplehearing.task.dto;

import com.simplehearing.task.enums.TaskPriority;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        String title,
        String description,
        UUID assignedTo,
        LocalDate dueDate,
        TaskPriority priority
) {}
