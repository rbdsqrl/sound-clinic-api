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
        LocalDate endDate,
        LeaveType leaveType,
        String reason,
        LeaveStatus status,
        UUID reviewedBy,
        String reviewedByFirstName,
        String reviewedByLastName,
        Instant reviewedAt,
        Instant createdAt,
        UUID categoryId,
        String categoryName,
        /** Working days the leave covers (weekly off days and public holidays excluded). Null until enriched. */
        Integer workingDays
) {
    public LeaveResponse withCategory(String name, Integer days) {
        return new LeaveResponse(id, therapistId, therapistFirstName, therapistLastName, leaveDate, endDate,
                leaveType, reason, status, reviewedBy, reviewedByFirstName, reviewedByLastName, reviewedAt,
                createdAt, categoryId, name, days);
    }

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
                leave.getEndDate(),
                leave.getLeaveType(),
                leave.getReason(),
                leave.getStatus(),
                leave.getReviewedBy(),
                reviewedByFirstName,
                reviewedByLastName,
                leave.getReviewedAt(),
                leave.getCreatedAt(),
                leave.getCategoryId(),
                null,
                null
        );
    }
}
