package com.simplehearing.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicHeadRemarksRequest(
        @NotBlank(message = "Remarks are required")
        @Size(max = 4000)
        String remarks
) {}
