package com.simplehearing.task.dto;

import com.simplehearing.task.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull @NotEmpty List<UUID> assignedTo,
        LocalDate dueDate,
        TaskPriority priority
) {}
