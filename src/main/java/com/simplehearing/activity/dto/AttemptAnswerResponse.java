package com.simplehearing.activity.dto;

import java.util.List;
import java.util.UUID;

public record AttemptAnswerResponse(
        UUID questionId,
        String questionText,
        List<UUID> selectedOptionIds,
        List<String> selectedOptionTexts,
        String textAnswer
) {}
