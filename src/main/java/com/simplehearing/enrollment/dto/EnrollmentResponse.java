package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import com.simplehearing.enrollment.enums.EnrollmentStatus;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
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
        LocalDate endDate,
        DayOfWeek dayOfWeek,
        /** Empty = no restriction — sessions can land on any day (skipping only holidays/org off-days). */
        Set<DayOfWeek> sessionDays,
        LocalTime startTime,
        EnrollmentStatus status,
        EnrollmentCareStatus careStatus,
        String careStatusNote,
        boolean therapistSignedOff,
        String therapistSignoffNotes,
        int sessionsCompleted,
        int totalSessions,
        Instant createdAt,
        /** Set only when this enrollment was closed by a patient discharge — a force-completed
         *  one (via the care-status override) stays null, which is how the frontend knows
         *  whether "Reactivate" is even offered. */
        UUID dischargedInRecordId
) {
    public static EnrollmentResponse from(
            Enrollment enrollment,
            String therapistFirstName,
            String therapistLastName,
            String programName,
            int sessionsCompleted,
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
                enrollment.getEndDate(),
                enrollment.getDayOfWeek(),
                enrollment.getSessionDays(),
                enrollment.getStartTime(),
                enrollment.getStatus(),
                enrollment.getCareStatus(),
                enrollment.getCareStatusNote(),
                enrollment.isTherapistSignedOff(),
                enrollment.getTherapistSignoffNotes(),
                sessionsCompleted,
                totalSessions,
                enrollment.getCreatedAt(),
                enrollment.getDischargedInRecordId()
        );
    }
}
