package com.simplehearing.resource.dto;

import com.simplehearing.resource.entity.ResourceFolder;

import java.time.Instant;
import java.util.UUID;

public record ResourceFolderResponse(
        UUID id,
        UUID parentFolderId,
        String name,
        long subfolderCount,
        long resourceCount,
        Instant createdAt
) {
    public static ResourceFolderResponse from(ResourceFolder f, long subfolderCount, long resourceCount) {
        return new ResourceFolderResponse(
                f.getId(),
                f.getParentFolderId(),
                f.getName(),
                subfolderCount,
                resourceCount,
                f.getCreatedAt()
        );
    }
}
