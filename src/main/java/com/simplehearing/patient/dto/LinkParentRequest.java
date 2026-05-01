package com.simplehearing.patient.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkParentRequest(
        @NotNull(message = "Parent user ID is required")
        UUID parentId
) {}
