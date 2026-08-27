package com.simplehearing.program.feedback.dto;

import com.simplehearing.program.feedback.entity.ProgramFeedbackOption;

import java.util.UUID;

public record ProgramFeedbackOptionResponse(
        UUID id,
        String optionText
) {
    public static ProgramFeedbackOptionResponse from(ProgramFeedbackOption o) {
        return new ProgramFeedbackOptionResponse(o.getId(), o.getOptionText());
    }
}
