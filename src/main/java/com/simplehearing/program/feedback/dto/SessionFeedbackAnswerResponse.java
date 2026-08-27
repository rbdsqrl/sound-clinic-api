package com.simplehearing.program.feedback.dto;

import java.util.List;
import java.util.UUID;

public record SessionFeedbackAnswerResponse(
        UUID questionId,
        List<UUID> selectedOptionIds,
        String textAnswer
) {}
