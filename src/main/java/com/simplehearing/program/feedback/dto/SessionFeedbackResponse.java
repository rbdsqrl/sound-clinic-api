package com.simplehearing.program.feedback.dto;

import java.util.List;

public record SessionFeedbackResponse(
        List<ProgramFeedbackQuestionResponse> template,
        List<SessionFeedbackAnswerResponse> answers,
        String checklistNotes
) {}
