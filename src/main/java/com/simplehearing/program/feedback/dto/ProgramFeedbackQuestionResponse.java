package com.simplehearing.program.feedback.dto;

import com.simplehearing.program.feedback.entity.ProgramFeedbackQuestion;
import com.simplehearing.program.feedback.enums.FeedbackQuestionType;

import java.util.List;
import java.util.UUID;

public record ProgramFeedbackQuestionResponse(
        UUID id,
        String questionText,
        FeedbackQuestionType questionType,
        List<ProgramFeedbackOptionResponse> options
) {
    public static ProgramFeedbackQuestionResponse from(ProgramFeedbackQuestion q, List<ProgramFeedbackOptionResponse> options) {
        return new ProgramFeedbackQuestionResponse(q.getId(), q.getQuestionText(), q.getQuestionType(), options);
    }
}
