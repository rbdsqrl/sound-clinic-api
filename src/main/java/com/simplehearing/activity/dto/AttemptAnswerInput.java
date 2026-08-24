package com.simplehearing.activity.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AttemptAnswerInput(
        @NotNull UUID questionId,
        List<UUID> selectedOptionIds,
        String textAnswer
) {}
