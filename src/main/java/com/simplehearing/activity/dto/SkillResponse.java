package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.Skill;

import java.util.UUID;

public record SkillResponse(UUID id, String name, boolean isActive) {
    public static SkillResponse from(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.isActive());
    }
}
