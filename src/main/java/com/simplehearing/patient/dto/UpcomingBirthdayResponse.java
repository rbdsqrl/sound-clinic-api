package com.simplehearing.patient.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingBirthdayResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        int daysUntil
) {}
