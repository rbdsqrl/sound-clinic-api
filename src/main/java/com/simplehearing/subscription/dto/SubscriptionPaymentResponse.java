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
        Instant createdAt,
        /** The program run (first to last session date) the payment was made for; null when unknown. */
        LocalDate periodStart,
        LocalDate periodEnd
) {
    public static SubscriptionPaymentResponse from(SubscriptionPayment p, String recordedByName) {
        return from(p, recordedByName, null, null);
    }

    /** {@code fallbackStart/End} (the program's overall run) fill in when the payment has no run of its own. */
    public static SubscriptionPaymentResponse from(SubscriptionPayment p, String recordedByName,
                                                   LocalDate fallbackStart, LocalDate fallbackEnd) {
        boolean own = p.getPeriodStart() != null && p.getPeriodEnd() != null;
        return new SubscriptionPaymentResponse(p.getId(), p.getSubscriptionId(), p.getAmount(), p.getPaidOn(),
                p.getMode(), p.getNote(), p.getRecordedBy(), recordedByName, p.getCreatedAt(),
                own ? p.getPeriodStart() : fallbackStart, own ? p.getPeriodEnd() : fallbackEnd);
    }
}
