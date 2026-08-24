package com.simplehearing.activity.dto;

import com.simplehearing.activity.enums.ChecklistQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ChecklistQuestionInput(
        @NotBlank String questionText,
        @NotNull ChecklistQuestionType questionType,
        List<String> options
) {}
