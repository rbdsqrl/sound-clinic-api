package com.simplehearing.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TherapistFeedbackRequest(
        @NotBlank(message = "A summary is required")
        @Size(max = 4000)
        String summary,

        @Size(max = 4000)
        String progressNotes
) {}
