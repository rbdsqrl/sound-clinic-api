package com.simplehearing.leave.dto;

import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.enums.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveResponse(
        UUID id,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        LocalDate leaveDate,
        LeaveType leaveType,
        String reason,
        LeaveStatus status,
        UUID reviewedBy,
        String reviewedByFirstName,
        String reviewedByLastName,
        Instant reviewedAt,
        Instant createdAt
) {
    public static LeaveResponse from(
            Leave leave,
            String therapistFirstName,
            String therapistLastName,
            String reviewedByFirstName,
            String reviewedByLastName) {
        return new LeaveResponse(
                leave.getId(),
                leave.getTherapistId(),
                therapistFirstName,
                therapistLastName,
                leave.getLeaveDate(),
                leave.getLeaveType(),
                leave.getReason(),
                leave.getStatus(),
                leave.getReviewedBy(),
                reviewedByFirstName,
                reviewedByLastName,
                leave.getReviewedAt(),
                leave.getCreatedAt()
        );
    }
}
