package com.simplehearing.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why a session's cancellation is being requested — required, so the approver sees the reason. */
public record CancellationRequestBody(
        @NotBlank(message = "Please give a reason for the cancellation request")
        @Size(max = 500, message = "The reason can be at most 500 characters")
        String reason
) {}
