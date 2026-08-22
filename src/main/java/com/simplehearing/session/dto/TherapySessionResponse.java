package com.simplehearing.session.dto;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.RescheduleReason;
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
        String feedback,
        String progressReport,
        Integer performanceScore,
        Instant completedAt,
        RescheduleReason rescheduleReason,

        /** True once a parent has asked for this session to be moved. Never resets. */
        boolean parentRescheduleRequested,
        /** Sessions of this plan the parent may still ask to move. */
        int parentReschedulesRemaining,

        /** Booked by hand from the calendar rather than generated with the plan. */
        boolean adHoc,
        /** False when it is an extra, on top of the sessions the family paid for. */
        boolean countsTowardPlan,
        /** True when an extra session still has to be paid for. */
        boolean requiresPayment
) {
    public static TherapySessionResponse from(
            TherapySession session,
            String patientFirstName,
            String patientLastName,
            String therapistFirstName,
            String therapistLastName,
            String programName,
            int totalSessions,
            int parentReschedulesRemaining) {
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
                session.getFeedback(),
                session.getProgressReport(),
                session.getPerformanceScore(),
                session.getCompletedAt(),
                session.getRescheduleReason(),
                session.isParentRescheduleRequested(),
                parentReschedulesRemaining,
                session.isAdHoc(),
                session.isCountsTowardPlan(),
                session.isRequiresPayment());
    }
}
