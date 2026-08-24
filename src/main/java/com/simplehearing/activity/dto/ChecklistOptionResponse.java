package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityChecklistOption;

import java.util.UUID;

public record ChecklistOptionResponse(UUID id, String optionText) {
    public static ChecklistOptionResponse from(ActivityChecklistOption o) {
        return new ChecklistOptionResponse(o.getId(), o.getOptionText());
    }
}
