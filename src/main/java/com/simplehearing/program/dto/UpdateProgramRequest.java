package com.simplehearing.program.dto;

import java.math.BigDecimal;

public record UpdateProgramRequest(
        String name,
        BigDecimal perSessionCost,
        Boolean isActive
) {}
