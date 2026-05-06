package com.simplehearing.patient.dto;

import com.simplehearing.patient.enums.PatientStage;
import jakarta.validation.constraints.NotNull;

public record UpdatePatientStageRequest(
        @NotNull PatientStage stage
) {}
