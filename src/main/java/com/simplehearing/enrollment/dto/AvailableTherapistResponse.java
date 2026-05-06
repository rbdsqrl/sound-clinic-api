package com.simplehearing.enrollment.dto;

import java.util.UUID;

public record AvailableTherapistResponse(
        UUID userId,
        String firstName,
        String lastName,
        UUID clinicId,
        String clinicName
) {}
