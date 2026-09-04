package com.simplehearing.resource.dto;

import com.simplehearing.resource.entity.Resource;
import com.simplehearing.resource.enums.ResourceType;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        UUID folderId,
        String name,
        ResourceType type,
        String url,
        Instant createdAt
) {
    /** `resolvedUrl` is `r.getUrl()` passed through StorageService.presign() — a fresh, time-limited
     *  link for anything stored in our own bucket, or the stored value unchanged for a pasted
     *  external link (YouTube, Google Drive, etc.), which presign() leaves untouched. Never use
     *  r.getUrl() directly in a response — a bucket/endpoint change would silently dead-link every
     *  resource stored before that change, since the raw stored URL is frozen at upload time. */
    public static ResourceResponse from(Resource r, String resolvedUrl) {
        return new ResourceResponse(
                r.getId(),
                r.getFolderId(),
                r.getName(),
                r.getType(),
                resolvedUrl,
                r.getCreatedAt()
        );
    }
}
