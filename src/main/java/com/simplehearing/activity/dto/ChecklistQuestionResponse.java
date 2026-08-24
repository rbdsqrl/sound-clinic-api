package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityChecklistQuestion;
import com.simplehearing.activity.enums.ChecklistQuestionType;

import java.util.List;
import java.util.UUID;

public record ChecklistQuestionResponse(
        UUID id,
        String questionText,
        ChecklistQuestionType questionType,
        List<ChecklistOptionResponse> options
) {
    public static ChecklistQuestionResponse from(ActivityChecklistQuestion q, List<ChecklistOptionResponse> options) {
        return new ChecklistQuestionResponse(q.getId(), q.getQuestionText(), q.getQuestionType(), options);
    }
}
