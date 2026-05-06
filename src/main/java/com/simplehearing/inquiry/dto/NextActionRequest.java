package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.enums.InquiryActionOutcome;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record NextActionRequest(
        @NotNull InquiryActionOutcome outcome,
        String notes,
        Instant appointmentDate,   // required when outcome = APPOINTMENT_BOOKED or SCHEDULE_FOLLOWUP
        String appointmentNotes
) {}
