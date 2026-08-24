package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.Prop;

import java.util.UUID;

public record PropResponse(UUID id, String name, boolean isActive) {
    public static PropResponse from(Prop p) {
        return new PropResponse(p.getId(), p.getName(), p.isActive());
    }
}
