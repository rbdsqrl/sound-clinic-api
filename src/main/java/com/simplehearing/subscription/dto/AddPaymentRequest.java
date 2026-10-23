package com.simplehearing.subscription.dto;

import com.simplehearing.subscription.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One payment received against a plan — added to what's already been paid. */
public record AddPaymentRequest(
        @NotNull @DecimalMin(value = "1", message = "Enter an amount of at least 1") BigDecimal amount,
        /** Defaults to today. */
        LocalDate paidOn,
        PaymentMode mode,
        String note
) {}
