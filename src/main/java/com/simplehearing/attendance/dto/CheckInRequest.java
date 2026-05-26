package com.simplehearing.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CheckInRequest(
        @NotNull(message = "clinicId is required")
        UUID clinicId,

        Double latitude,
        Double longitude,
        List<Double> faceDescriptor
) {}
