package com.simplehearing.subscription.service;

import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.entity.SubscriptionPayment;
import com.simplehearing.subscription.enums.PaymentMode;
import com.simplehearing.subscription.enums.SubscriptionPaymentStatus;
import com.simplehearing.subscription.repository.SubscriptionPaymentRepository;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The one place a plan's payment status is worked out and its payment entries are written, so the
 * record-payment screen, a renewal's extra payment and a later top-up can't drift apart.
 * {@code Subscription.amountPaid} stays the running total; each entry here is one dated payment
 * (a negative one is a correction).
 */
@Service
public class SubscriptionPaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final EnrollmentPaymentActivationService activationService;

    public SubscriptionPaymentService(SubscriptionRepository subscriptionRepository,
                                      SubscriptionPaymentRepository paymentRepository,
                                      EnrollmentPaymentActivationService activationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.activationService = activationService;
    }

    /** Sessions × per-session cost, less the discount, rounded to whole rupees (the UI only ever
     *  shows and collects whole-rupee amounts — comparing at paise left plans stuck as PARTIAL). */
    public static BigDecimal formulaTotal(Subscription sub) {
        return formulaTotalFor(sub, sub.getNumSessions());
    }

    /** The same figure for a given number of sessions at the plan's rate and discount. */
    public static BigDecimal formulaTotalFor(Subscription sub, int sessions) {
        BigDecimal discount = sub.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return sub.getPerSessionCost()
                .multiply(BigDecimal.valueOf(sessions))
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /** What the plan costs: the admin-agreed total when one was set, else {@link #formulaTotal}. */
    public static BigDecimal totalDue(Subscription sub) {
        return sub.getTotalOverride() != null
                ? sub.getTotalOverride().setScale(0, RoundingMode.HALF_UP)
                : formulaTotal(sub);
    }

    public static SubscriptionPaymentStatus derivedStatus(Subscription sub) {
        BigDecimal paid = sub.getAmountPaid().setScale(0, RoundingMode.HALF_UP);
        if (paid.compareTo(BigDecimal.ZERO) == 0) return SubscriptionPaymentStatus.PENDING;
        return paid.compareTo(totalDue(sub)) >= 0 ? SubscriptionPaymentStatus.PAID : SubscriptionPaymentStatus.PARTIAL;
    }

    /** Re-derives the status from the current totals and saves it. When the plan has just become
     *  PAID, sessions still held awaiting payment are released — the one edge that does so. */
    @Transactional
    public Subscription refreshStatus(Subscription sub) {
        SubscriptionPaymentStatus previous = sub.getPaymentStatus();
        sub.setPaymentStatus(derivedStatus(sub));
        Subscription saved = subscriptionRepository.save(sub);
        if (previous != SubscriptionPaymentStatus.PAID && saved.getPaymentStatus() == SubscriptionPaymentStatus.PAID) {
            activationService.activate(saved.getId());
        }
        return saved;
    }

    /** Records one payment, adds it to the running total and refreshes the status. */
    @Transactional
    public Subscription addPayment(Subscription sub, BigDecimal amount, LocalDate paidOn, PaymentMode mode,
                                   String note, UUID recordedBy) {
        logEntry(sub, amount, paidOn, mode, note, recordedBy);
        sub.setAmountPaid(sub.getAmountPaid().add(amount));
        return refreshStatus(sub);
    }

    /** Writes the entry only — for when the caller has already set the running total itself (the
     *  record-payment screen sets an absolute figure, so its entry is the difference). */
    @Transactional
    public void logEntry(Subscription sub, BigDecimal amount, LocalDate paidOn, PaymentMode mode,
                         String note, UUID recordedBy) {
        SubscriptionPayment entry = new SubscriptionPayment();
        entry.setOrgId(sub.getOrgId());
        entry.setSubscriptionId(sub.getId());
        entry.setAmount(amount);
        entry.setPaidOn(paidOn != null ? paidOn : LocalDate.now());
        entry.setMode(mode);
        entry.setNote(note != null && !note.isBlank() ? note.trim() : null);
        entry.setRecordedBy(recordedBy);
        paymentRepository.save(entry);
    }
}
