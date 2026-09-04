package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityResource;

import java.util.UUID;

public record ActivityResourceResponse(
        UUID id, String fileName, String fileUrl, String contentType, Long fileSizeBytes
) {
    /** `resolvedUrl` is `r.getFileUrl()` passed through StorageService.presign() — a fresh,
     *  time-limited link, rather than the permanent one baked in at upload time (which a
     *  bucket/endpoint change would silently dead-link). Never use r.getFileUrl() directly here. */
    public static ActivityResourceResponse from(ActivityResource r, String resolvedUrl) {
        return new ActivityResourceResponse(r.getId(), r.getFileName(), resolvedUrl, r.getContentType(), r.getFileSizeBytes());
    }
}
