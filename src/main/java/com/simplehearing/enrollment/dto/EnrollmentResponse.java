package com.simplehearing.enrollment.dto;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import com.simplehearing.enrollment.ProgramMonths;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.enums.ProgramStatus;

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
        /** True until the linked subscription's payment is recorded in full — sessions are real
         *  and scheduled, just locked from completion/notes/reschedule-request until then. */
        boolean awaitingPayment,
        EnrollmentCareStatus careStatus,
        String careStatusNote,
        boolean therapistSignedOff,
        String therapistSignoffNotes,
        int sessionsCompleted,
        int totalSessions,
        /** The calendar month ("yyyy-MM") the program is in now, and that month's own counts — what every
         *  screen quotes instead of a running total across renewals. */
        String currentMonth,
        int currentMonthCompleted,
        int currentMonthTotal,
        ProgramStatus programStatus,
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
            int totalSessions,
            ProgramMonths.Summary month) {
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
                enrollment.isAwaitingPayment(),
                enrollment.getCareStatus(),
                enrollment.getCareStatusNote(),
                enrollment.isTherapistSignedOff(),
                enrollment.getTherapistSignoffNotes(),
                sessionsCompleted,
                totalSessions,
                month.month().toString(),
                month.completed(),
                month.total(),
                effectiveProgramStatus(enrollment),
                enrollment.getCreatedAt(),
                enrollment.getDischargedInRecordId()
        );
    }

    /** A program an admin marked Completed stays Active until its last session date has passed — it is never shown as
     *  completed before its real end date. Every other status is shown as stored. */
    private static ProgramStatus effectiveProgramStatus(Enrollment e) {
        boolean completedDecision = e.getRenewalOutcome() == com.simplehearing.enrollment.enums.RenewalOutcome.COMPLETED;
        boolean ended = e.getEndDate() != null && e.getEndDate().isBefore(LocalDate.now());
        return completedDecision && ended ? ProgramStatus.COMPLETED_DISCONTINUED : e.getProgramStatus();
    }
}
