package com.simplehearing.sharedmedia.dto;

import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.sharedmedia.enums.SharedMediaDirection;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record SharedMediaResponse(
        UUID id,
        UUID patientId,
        SharedMediaDirection direction,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        String note,
        UUID uploadedById,
        String uploadedByName,
        Role uploadedByRole,
        Instant createdAt
) {
    public static SharedMediaResponse from(SharedMedia m, String uploaderName, Role uploaderRole, String presignedUrl) {
        return new SharedMediaResponse(
                m.getId(),
                m.getPatientId(),
                m.getDirection(),
                m.getFileName(),
                presignedUrl,
                m.getContentType(),
                m.getFileSizeBytes(),
                m.getNote(),
                m.getUploadedBy(),
                uploaderName,
                uploaderRole,
                m.getCreatedAt()
        );
    }
}
