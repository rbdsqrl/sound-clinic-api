package com.simplehearing.leave.dto;

import com.simplehearing.leave.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewLeaveRequest(
        @NotNull LeaveStatus status   // APPROVED or REJECTED
) {}
