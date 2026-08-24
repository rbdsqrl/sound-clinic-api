package com.simplehearing.concern.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RaiseConcernRequest(
        @NotNull UUID enrollmentId,
        @NotBlank String description
) {
}
