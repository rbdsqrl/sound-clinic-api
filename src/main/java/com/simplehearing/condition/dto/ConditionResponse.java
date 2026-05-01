package com.simplehearing.condition.dto;

import com.simplehearing.condition.entity.Condition;

import java.util.UUID;

public record ConditionResponse(UUID id, String name, String description) {
    public static ConditionResponse from(Condition c) {
        return new ConditionResponse(c.getId(), c.getName(), c.getDescription());
    }
}
