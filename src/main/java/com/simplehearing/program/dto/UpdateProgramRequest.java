package com.simplehearing.program.dto;

import java.math.BigDecimal;

public record UpdateProgramRequest(
        String name,
        String description,
        BigDecimal perSessionCost,
        Boolean isActive
) {}
