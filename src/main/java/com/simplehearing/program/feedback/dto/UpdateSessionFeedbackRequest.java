package com.simplehearing.program.feedback.dto;

import java.util.List;

public record UpdateSessionFeedbackRequest(
        List<SessionFeedbackAnswerInput> answers,
        String checklistNotes
) {}
