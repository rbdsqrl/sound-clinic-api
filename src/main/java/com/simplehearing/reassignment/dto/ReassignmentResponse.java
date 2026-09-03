package com.simplehearing.reassignment.dto;

import com.simplehearing.reassignment.entity.TherapistReassignment;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReassignmentResponse(
        UUID id,
        UUID fromTherapistId,
        String fromTherapistName,
        UUID toTherapistId,
        String toTherapistName,
        ReassignmentType type,
        LocalDate startDate,
        LocalDate endDate,
        ReassignmentStatus status,
        String reason,
        Instant createdAt,
        Instant revertedAt,
        List<ReassignmentCaseSummary> cases
) {
    public static ReassignmentResponse from(TherapistReassignment r,
                                            String fromTherapistName,
                                            String toTherapistName,
                                            List<ReassignmentCaseSummary> cases) {
        return new ReassignmentResponse(
                r.getId(),
                r.getFromTherapistId(),
                fromTherapistName,
                r.getToTherapistId(),
                toTherapistName,
                r.getType(),
                r.getStartDate(),
                r.getEndDate(),
                r.getStatus(),
                r.getReason(),
                r.getCreatedAt(),
                r.getRevertedAt(),
                cases);
    }
}
