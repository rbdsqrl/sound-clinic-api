package com.simplehearing.patient.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePatientActiveRequest(
        @NotNull Boolean active
) {}
