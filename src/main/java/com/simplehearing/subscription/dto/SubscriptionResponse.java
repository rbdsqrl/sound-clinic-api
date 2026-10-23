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
        Instant createdAt,
        /** Set when an admin agreed a total for the whole plan (an ad hoc discount) — totalAmount is then this figure. */
        BigDecimal totalOverride
) {
    public static SubscriptionResponse from(Subscription sub, String programName) {
        // totalAmount: the admin-agreed total if there is one, else sessions × per-session cost less the
        // discount percentage, rounded to whole rupees — the same figure the payment status is judged against.
        BigDecimal total = com.simplehearing.subscription.service.SubscriptionPaymentService.totalDue(sub);
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
                sub.getCreatedAt(),
                sub.getTotalOverride()
        );
    }
}
