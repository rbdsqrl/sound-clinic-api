package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.entity.Inquiry;
import com.simplehearing.inquiry.enums.InquiryStatus;
import com.simplehearing.inquiry.enums.PreferredTime;

import java.time.Instant;
import java.util.UUID;

public record InquiryResponse(
        UUID id,
        UUID orgId,
        String name,
        String email,
        String phone,
        String reason,
        PreferredTime preferredTime,
        InquiryStatus status,
        String adminNotes,
        Instant appointmentDate,
        String appointmentNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getOrgId(),
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getPhone(),
                inquiry.getReason(),
                inquiry.getPreferredTime(),
                inquiry.getStatus(),
                inquiry.getAdminNotes(),
                inquiry.getAppointmentDate(),
                inquiry.getAppointmentNotes(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}
