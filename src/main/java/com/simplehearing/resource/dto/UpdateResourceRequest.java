package com.simplehearing.resource.dto;

import com.simplehearing.resource.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateResourceRequest(
        @NotBlank(message = "Resource name is required")
        String name,

        @NotNull(message = "Resource type is required")
        ResourceType type,

        @NotBlank(message = "URL is required")
        String url
) {}
