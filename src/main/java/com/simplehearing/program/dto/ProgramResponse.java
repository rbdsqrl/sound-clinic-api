package com.simplehearing.program.dto;

import com.simplehearing.program.entity.Program;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        UUID orgId,
        String name,
        BigDecimal perSessionCost,
        boolean isActive,
        Instant createdAt
) {
    public static ProgramResponse from(Program program) {
        return new ProgramResponse(
                program.getId(),
                program.getOrgId(),
                program.getName(),
                program.getPerSessionCost(),
                program.isActive(),
                program.getCreatedAt()
        );
    }
}
