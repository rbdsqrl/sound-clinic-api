package com.simplehearing.task.dto;

import com.simplehearing.task.entity.TaskAttachment;

import java.time.Instant;
import java.util.UUID;

public record TaskAttachmentResponse(
        UUID id,
        UUID taskId,
        UUID uploadedBy,
        String uploadedByFirstName,
        String uploadedByLastName,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        Instant createdAt
) {
    public static TaskAttachmentResponse from(TaskAttachment a, String firstName, String lastName) {
        return new TaskAttachmentResponse(
                a.getId(), a.getTaskId(), a.getUploadedBy(),
                firstName, lastName,
                a.getFileName(), a.getFileUrl(), a.getContentType(),
                a.getFileSizeBytes(), a.getCreatedAt());
    }
}
