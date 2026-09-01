package com.simplehearing.resource.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateResourceFolderRequest(
        @NotBlank(message = "Folder name is required")
        String name,

        /** Null creates a top-level folder. */
        UUID parentFolderId
) {}
