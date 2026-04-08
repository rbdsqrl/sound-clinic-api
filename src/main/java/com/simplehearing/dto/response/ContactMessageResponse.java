package com.simplehearing.dto.response;

import com.simplehearing.entity.ContactMessage;

import java.time.LocalDateTime;

public record ContactMessageResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String message,
    boolean read,
    LocalDateTime submittedAt
) {
    public static ContactMessageResponse from(ContactMessage m) {
        return new ContactMessageResponse(
            m.getId(), m.getFullName(), m.getEmail(),
            m.getPhone(), m.getMessage(), m.isRead(), m.getSubmittedAt()
        );
    }
}
