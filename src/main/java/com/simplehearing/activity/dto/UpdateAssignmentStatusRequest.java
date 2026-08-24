package com.simplehearing.activity.dto;

import com.simplehearing.activity.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAssignmentStatusRequest(@NotNull AssignmentStatus status) {}
