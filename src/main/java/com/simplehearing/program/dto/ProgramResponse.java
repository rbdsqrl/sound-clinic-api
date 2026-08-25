package com.simplehearing.program.dto;

import com.simplehearing.program.entity.Program;
import com.simplehearing.tax.entity.Tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        UUID orgId,
        String name,
        String description,
        BigDecimal perSessionCost,
        UUID taxId,
        String taxName,
        Double taxRate,
        boolean priceIncludesTax,
        BigDecimal totalCost,
        boolean isActive,
        Instant createdAt
) {
    /** @param tax the program's tax, resolved by the caller — null if the program has no tax set */
    public static ProgramResponse from(Program program, Tax tax) {
        BigDecimal cost = program.getPerSessionCost();
        BigDecimal total = cost;

        if (tax != null) {
            BigDecimal rateFraction = BigDecimal.valueOf(tax.getRate()).divide(BigDecimal.valueOf(100));
            total = program.isPriceIncludesTax()
                    ? cost
                    : cost.multiply(BigDecimal.ONE.add(rateFraction)).setScale(2, RoundingMode.HALF_UP);
        }

        return new ProgramResponse(
                program.getId(),
                program.getOrgId(),
                program.getName(),
                program.getDescription(),
                program.getPerSessionCost(),
                program.getTaxId(),
                tax != null ? tax.getName() : null,
                tax != null ? tax.getRate() : null,
                program.isPriceIncludesTax(),
                total,
                program.isActive(),
                program.getCreatedAt()
        );
    }
}
