package com.simplehearing.resource.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignResourceRequest(
        @NotNull UUID patientId
) {}
