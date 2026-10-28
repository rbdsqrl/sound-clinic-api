package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.enums.ProgramStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProgramStatusRequest(@NotNull ProgramStatus status) {}
