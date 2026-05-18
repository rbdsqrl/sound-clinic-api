package com.simplehearing.session.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RescheduleSessionRequest(
        LocalDate newDate,
        UUID substituteTherapistId
) {}
