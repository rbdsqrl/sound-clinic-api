package com.simplehearing.memberdocument.dto;

import com.simplehearing.memberdocument.entity.MemberDocument;
import com.simplehearing.memberdocument.enums.MemberDocumentCategory;

import java.time.Instant;
import java.util.UUID;

public record MemberDocumentResponse(
        UUID id,
        UUID memberId,
        MemberDocumentCategory category,
        String title,
        String fileName,
        /** Short-lived presigned download URL — never the stored URL. */
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        String notes,
        UUID uploadedById,
        String uploadedByName,
        Instant createdAt
) {
    public static MemberDocumentResponse from(MemberDocument d, String uploadedByName, String presignedUrl) {
        return new MemberDocumentResponse(
                d.getId(), d.getMemberId(), d.getCategory(), d.getTitle(), d.getFileName(),
                presignedUrl, d.getContentType(), d.getFileSizeBytes(), d.getNotes(),
                d.getUploadedBy(), uploadedByName, d.getCreatedAt());
    }
}
