package com.simplehearing.resource.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateResourceFolderRequest(
        @NotBlank(message = "Folder name is required")
        String name
) {}
