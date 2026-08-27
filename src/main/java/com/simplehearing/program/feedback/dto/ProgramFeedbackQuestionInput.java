package com.simplehearing.program.feedback.dto;

import com.simplehearing.program.feedback.enums.FeedbackQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProgramFeedbackQuestionInput(
        @NotBlank String questionText,
        @NotNull FeedbackQuestionType questionType,
        List<String> options
) {}
