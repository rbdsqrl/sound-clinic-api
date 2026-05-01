package com.simplehearing.patient.dto;

import com.simplehearing.user.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePatientRequest(
        @NotNull(message = "Clinic ID is required")
        UUID clinicId,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        LocalDate dateOfBirth,
        Gender gender,
        String notes
) {}
