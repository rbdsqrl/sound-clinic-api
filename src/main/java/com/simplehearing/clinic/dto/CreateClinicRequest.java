package com.simplehearing.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClinicRequest(
        @NotBlank(message = "Clinic name is required")
        String name,

        String address,
        String phone,
        String email,
        String timezone
) {}
