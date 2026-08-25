package com.simplehearing.program.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProgramRequest(
        String name,
        String description,
        BigDecimal perSessionCost,
        UUID taxId,
        Boolean priceIncludesTax,
        Boolean removeTax,
        Boolean isActive
) {}
