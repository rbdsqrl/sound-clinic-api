package com.simplehearing.parentfeedback.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ParentFeedbackResponse(
        UUID id,
        UUID sessionId,
        int sessionNumber,
        LocalDate sessionDate,
        String programName,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        String therapistFirstName,
        String therapistLastName,
        String parentFirstName,
        String parentLastName,
        String message,
        Instant createdAt,
        Instant updatedAt,
        boolean sharedWithTherapist,
        Instant sharedAt,
        /** True once an admin has read or shared it. */
        boolean reviewed
) {}
