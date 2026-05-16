package com.simplehearing.task.dto;

import com.simplehearing.task.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {}
