package com.simplehearing.activity.dto;

import java.util.List;

public record MagicFillResponse(
        List<String> instructions,
        List<ChecklistQuestionInput> checklist
) {}
