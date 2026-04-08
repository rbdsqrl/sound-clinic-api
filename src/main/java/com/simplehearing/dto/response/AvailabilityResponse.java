package com.simplehearing.dto.response;

import java.util.List;

public record AvailabilityResponse(
    String date,
    String dayLabel,
    List<String> availableSlots
) {}
