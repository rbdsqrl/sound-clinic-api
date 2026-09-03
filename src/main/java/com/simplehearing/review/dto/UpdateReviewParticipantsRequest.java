package com.simplehearing.review.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Full replacement of a review meeting's participant list. */
public record UpdateReviewParticipantsRequest(
        @NotEmpty List<UUID> participantIds
) {}
