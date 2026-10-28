package com.simplehearing.enrollment.dto;

import java.time.LocalDate;
import java.util.UUID;

/** A program in its last week — one row of the admin Renewals task list. */
public record RenewalDueResponse(
        UUID enrollmentId,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        String programName,
        String therapistFirstName,
        String therapistLastName,
        LocalDate endDate,
        /** Days from today to the last session — negative once it has passed. */
        int daysLeft,
        /** Plan sessions still to happen. */
        int sessionsLeft
) {}
