package com.simplehearing.program.feedback.dto;

import java.util.List;

public record UpdateProgramFeedbackTemplateRequest(
        List<ProgramFeedbackQuestionInput> questions
) {}
