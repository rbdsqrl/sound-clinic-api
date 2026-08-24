package com.simplehearing.concern.dto;

import com.simplehearing.concern.entity.EnrollmentConcern;
import com.simplehearing.concern.enums.ConcernStatus;

import java.time.Instant;
import java.util.UUID;

public record ConcernResponse(
        UUID id,
        UUID enrollmentId,
        String programName,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        UUID raisedBy,
        Instant raisedAt,
        String description,
        ConcernStatus status,
        Instant acknowledgedAt,
        String resolutionNotes,
        Instant resolvedAt
) {
    public static ConcernResponse from(
            EnrollmentConcern c,
            String programName,
            String patientFirstName,
            String patientLastName,
            String therapistFirstName,
            String therapistLastName) {
        return new ConcernResponse(
                c.getId(),
                c.getEnrollmentId(),
                programName,
                c.getPatientId(),
                patientFirstName,
                patientLastName,
                c.getTherapistId(),
                therapistFirstName,
                therapistLastName,
                c.getRaisedBy(),
                c.getRaisedAt(),
                c.getDescription(),
                c.getStatus(),
                c.getAcknowledgedAt(),
                c.getResolutionNotes(),
                c.getResolvedAt()
        );
    }
}
