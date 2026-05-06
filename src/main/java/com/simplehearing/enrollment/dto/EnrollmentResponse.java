package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID subscriptionId,
        UUID patientId,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        String programName,
        int sessionDurationMinutes,
        LocalDate startDate,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        EnrollmentStatus status,
        int sessionsCompleted,
        int totalSessions,
        Instant createdAt
) {
    public static EnrollmentResponse from(
            Enrollment enrollment,
            String therapistFirstName,
            String therapistLastName,
            String programName,
            int totalSessions) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getSubscriptionId(),
                enrollment.getPatientId(),
                enrollment.getTherapistId(),
                therapistFirstName,
                therapistLastName,
                programName,
                enrollment.getSessionDurationMinutes(),
                enrollment.getStartDate(),
                enrollment.getDayOfWeek(),
                enrollment.getStartTime(),
                enrollment.getStatus(),
                enrollment.getSessionsCompleted(),
                totalSessions,
                enrollment.getCreatedAt()
        );
    }
}
