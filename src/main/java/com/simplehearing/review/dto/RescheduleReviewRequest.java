package com.simplehearing.review.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleReviewRequest(
        @NotNull LocalDate meetingDate,
        @NotNull LocalTime startTime,
        Integer durationMinutes
) {}
