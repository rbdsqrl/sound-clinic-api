package com.simplehearing.sharedmedia.dto;

import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.sharedmedia.enums.SharedMediaDirection;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code readAt}/{@code readById}/{@code readByName} say when the intended recipient first opened
 * the post (null = unread). {@code patientName} is filled only for the dashboard inbox, where posts
 * from several children are listed together.
 */
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
        String uploadedByCustomRole,
        Instant createdAt,
        Instant readAt,
        UUID readById,
        String readByName,
        String patientName
) {
    public static SharedMediaResponse from(SharedMedia m, String uploaderName, Role uploaderRole, String uploaderCustomRole,
                                           String presignedUrl, String readByName, String patientName) {
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
                uploaderCustomRole,
                m.getCreatedAt(),
                m.getReadAt(),
                m.getReadBy(),
                readByName,
                patientName
        );
    }
}
