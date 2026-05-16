package com.simplehearing.session.dto;

import com.simplehearing.session.entity.SessionAttachment;

import java.time.Instant;
import java.util.UUID;

public record SessionAttachmentResponse(
        UUID id,
        UUID sessionId,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        Instant createdAt
) {
    public static SessionAttachmentResponse from(SessionAttachment a) {
        return new SessionAttachmentResponse(
                a.getId(),
                a.getSessionId(),
                a.getFileName(),
                a.getFileUrl(),
                a.getContentType(),
                a.getFileSizeBytes(),
                a.getCreatedAt());
    }
}
