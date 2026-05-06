package com.simplehearing.leave.dto;

import com.simplehearing.leave.enums.LeaveType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLeaveRequest(
        @NotNull LocalDate leaveDate,
        @NotNull LeaveType leaveType,
        String reason
) {}
