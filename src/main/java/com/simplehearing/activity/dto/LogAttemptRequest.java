package com.simplehearing.activity.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record LogAttemptRequest(
        @NotNull LocalDate attemptDate,
        String note,
        List<AttemptAnswerInput> answers
) {}
