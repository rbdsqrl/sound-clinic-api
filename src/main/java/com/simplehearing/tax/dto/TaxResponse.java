package com.simplehearing.tax.dto;

import com.simplehearing.tax.entity.Tax;

import java.time.Instant;
import java.util.UUID;

public record TaxResponse(UUID id, UUID orgId, String name, double rate, boolean isActive, Instant createdAt) {
    public static TaxResponse from(Tax t) {
        return new TaxResponse(t.getId(), t.getOrgId(), t.getName(), t.getRate(), t.isActive(), t.getCreatedAt());
    }
}
