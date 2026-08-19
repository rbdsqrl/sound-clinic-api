package com.simplehearing.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParentFeedbackRequest(
        @NotNull @Min(1) @Max(5)
        Integer rating,

        @Size(max = 4000)
        String comments
) {}
