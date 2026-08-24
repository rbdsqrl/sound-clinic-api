package com.simplehearing.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParentFeedbackRequest(
        @NotNull @Min(1) @Max(5)
        Integer communicationRating,

        @NotNull @Min(0) @Max(100)
        Integer progressRatingPct,

        @Size(max = 4000)
        String comments
) {}
