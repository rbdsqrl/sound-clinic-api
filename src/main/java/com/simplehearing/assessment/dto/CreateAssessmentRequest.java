package com.simplehearing.assessment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateAssessmentRequest(
        @NotNull LocalDate assessmentDate,
        @NotEmpty Map<Integer, ItemResponse> responses
) {
    /** Exactly one of optionId / optionIds / text is populated, depending on the item's type. */
    public record ItemResponse(UUID optionId, List<UUID> optionIds, String text) {}
}
