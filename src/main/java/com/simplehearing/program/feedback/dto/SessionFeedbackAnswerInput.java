package com.simplehearing.program.feedback.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SessionFeedbackAnswerInput(
        @NotNull UUID questionId,
        List<UUID> selectedOptionIds,
        String textAnswer
) {}
