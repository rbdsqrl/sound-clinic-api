package com.simplehearing.therapy.dto;

import com.simplehearing.therapy.entity.Therapy;

import java.time.Instant;
import java.util.UUID;

public record TherapyResponse(UUID id, UUID orgId, String name, boolean isActive, Instant createdAt) {
    public static TherapyResponse from(Therapy t) {
        return new TherapyResponse(t.getId(), t.getOrgId(), t.getName(), t.isActive(), t.getCreatedAt());
    }
}
