package com.simplehearing.subscription.dto;

import com.simplehearing.subscription.entity.SubscriptionPayment;
import com.simplehearing.subscription.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionPaymentResponse(
        UUID id,
        UUID subscriptionId,
        BigDecimal amount,
        LocalDate paidOn,
        PaymentMode mode,
        String note,
        UUID recordedById,
        String recordedByName,
        Instant createdAt
) {
    public static SubscriptionPaymentResponse from(SubscriptionPayment p, String recordedByName) {
        return new SubscriptionPaymentResponse(p.getId(), p.getSubscriptionId(), p.getAmount(), p.getPaidOn(),
                p.getMode(), p.getNote(), p.getRecordedBy(), recordedByName, p.getCreatedAt());
    }
}
