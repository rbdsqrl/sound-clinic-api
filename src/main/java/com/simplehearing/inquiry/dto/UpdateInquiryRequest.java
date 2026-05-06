package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.enums.InquiryStatus;

import java.time.Instant;

public record UpdateInquiryRequest(
        InquiryStatus status,
        String adminNotes,
        Instant appointmentDate,
        String appointmentNotes,
        Boolean clearAppointment
) {}
