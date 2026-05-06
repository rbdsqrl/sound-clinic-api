package com.simplehearing.subscription.dto;

import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.enums.SubscriptionPaymentStatus;
import com.simplehearing.subscription.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID patientId,
        UUID programId,
        String programName,
        int numSessions,
        BigDecimal perSessionCost,
        BigDecimal discountPercent,
        BigDecimal amountPaid,
        BigDecimal totalAmount,
        SubscriptionPaymentStatus paymentStatus,
        SubscriptionStatus status,
        String paymentNotes,
        String notes,
        Instant createdAt
) {
    public static SubscriptionResponse from(Subscription sub, String programName) {
        // totalAmount = numSessions × perSessionCost × (1 - discountPercent / 100)
        BigDecimal discount = sub.getDiscountPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal total = sub.getPerSessionCost()
                .multiply(BigDecimal.valueOf(sub.getNumSessions()))
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(2, RoundingMode.HALF_UP);

        return new SubscriptionResponse(
                sub.getId(),
                sub.getPatientId(),
                sub.getProgramId(),
                programName,
                sub.getNumSessions(),
                sub.getPerSessionCost(),
                sub.getDiscountPercent(),
                sub.getAmountPaid(),
                total,
                sub.getPaymentStatus(),
                sub.getStatus(),
                sub.getPaymentNotes(),
                sub.getNotes(),
                sub.getCreatedAt()
        );
    }
}
