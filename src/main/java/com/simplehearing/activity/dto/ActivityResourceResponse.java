package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityResource;

import java.util.UUID;

public record ActivityResourceResponse(
        UUID id, String fileName, String fileUrl, String contentType, Long fileSizeBytes
) {
    public static ActivityResourceResponse from(ActivityResource r) {
        return new ActivityResourceResponse(r.getId(), r.getFileName(), r.getFileUrl(), r.getContentType(), r.getFileSizeBytes());
    }
}
