package com.simplehearing.session.dto;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TherapySessionResponse(
        UUID id,
        UUID enrollmentId,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        String programName,
        int sessionNumber,
        int totalSessions,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        TherapySessionStatus status,
        String notes,
        Instant completedAt
) {
    public static TherapySessionResponse from(
            TherapySession session,
            String patientFirstName,
            String patientLastName,
            String therapistFirstName,
            String therapistLastName,
            String programName,
            int totalSessions) {
        return new TherapySessionResponse(
                session.getId(),
                session.getEnrollmentId(),
                session.getPatientId(),
                patientFirstName,
                patientLastName,
                session.getTherapistId(),
                therapistFirstName,
                therapistLastName,
                programName,
                session.getSessionNumber(),
                totalSessions,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getNotes(),
                session.getCompletedAt()
        );
    }
}
