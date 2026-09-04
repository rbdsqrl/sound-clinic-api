package com.simplehearing.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLeaveRequest(
        @NotNull LocalDate leaveDate,
        /** Inclusive end of the range — omit (or set equal to leaveDate) for a single-day leave. */
        LocalDate endDate,
        String reason
) {}
