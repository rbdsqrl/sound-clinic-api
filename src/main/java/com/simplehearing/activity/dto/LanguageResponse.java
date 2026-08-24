package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.Language;

import java.util.UUID;

public record LanguageResponse(UUID id, String name, boolean isActive) {
    public static LanguageResponse from(Language l) {
        return new LanguageResponse(l.getId(), l.getName(), l.isActive());
    }
}
