package com.simplehearing.attendance.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EnrollFaceRequest(
        @NotEmpty(message = "faceDescriptor is required")
        List<Double> faceDescriptor
) {}
