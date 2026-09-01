package com.simplehearing.resource.dto;

import com.simplehearing.resource.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateResourceRequest(
        @NotBlank(message = "Resource name is required")
        String name,

        @NotNull(message = "Resource type is required")
        ResourceType type,

        @NotBlank(message = "URL is required")
        String url,

        /** Null places the resource at the root, alongside the top-level folders. */
        UUID folderId
) {}
