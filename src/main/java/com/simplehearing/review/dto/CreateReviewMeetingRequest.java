package com.simplehearing.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Adds a single ad-hoc review meeting to an existing enrollment. */
public record CreateReviewMeetingRequest(
        @NotNull UUID enrollmentId,
        @NotNull LocalDate meetingDate,
        @NotNull LocalTime startTime,
        @Min(15) @Max(240) int durationMinutes
) {}
