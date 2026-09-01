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
    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(
                r.getId(),
                r.getFolderId(),
                r.getName(),
                r.getType(),
                r.getUrl(),
                r.getCreatedAt()
        );
    }
}
