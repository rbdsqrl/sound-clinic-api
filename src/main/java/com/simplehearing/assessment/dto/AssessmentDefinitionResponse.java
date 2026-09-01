package com.simplehearing.assessment.dto;

import java.util.List;
import java.util.UUID;

public record AssessmentDefinitionResponse(
        String code,
        String name,
        String description,
        String scoringType,
        Integer maxScore,
        List<CategoryDto> categories
) {
    public record CategoryDto(String name, List<ItemDto> items) {}

    public record ItemDto(int number, String text, String itemType, List<OptionDto> options) {}

    public record OptionDto(UUID id, String label, Integer score) {}
}
