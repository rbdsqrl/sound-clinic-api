package com.simplehearing.subscription.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePaymentRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent,
        @NotNull @DecimalMin("0") BigDecimal amountPaid,
        String paymentNotes
) {}
