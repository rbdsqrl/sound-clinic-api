package com.simplehearing.subscription.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Flips awaitingPayment false on every enrollment (and its sessions) tied to a subscription, the
 * moment that subscription's payment status reaches PAID — see
 * SubscriptionController#recordPayment, the only call site, and only on the genuine
 * PENDING/PARTIAL -&gt; PAID transition.
 */
@Service
public class EnrollmentPaymentActivationService {

    private final EnrollmentRepository enrollmentRepository;
    private final TherapySessionRepository sessionRepository;

    public EnrollmentPaymentActivationService(EnrollmentRepository enrollmentRepository,
                                              TherapySessionRepository sessionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
    }

    /** Activates every still-awaiting-payment enrollment for this subscription (normally exactly
     *  one, but not assumed) and all of their still-awaiting-payment sessions. Fetch+loop+saveAll
     *  — mirrors PatientService#setActive, this codebase's existing style for a same-flag bulk
     *  flip; no @Modifying bulk-update precedent exists here. */
    @Transactional
    public void activate(UUID subscriptionId) {
        List<Enrollment> enrollments = enrollmentRepository.findBySubscriptionId(subscriptionId).stream()
                .filter(Enrollment::isAwaitingPayment)
                .toList();
        if (enrollments.isEmpty()) return;

        enrollments.forEach(e -> e.setAwaitingPayment(false));
        enrollmentRepository.saveAll(enrollments);

        List<UUID> enrollmentIds = enrollments.stream().map(Enrollment::getId).toList();
        List<TherapySession> sessions = sessionRepository.findByEnrollmentIdIn(enrollmentIds).stream()
                .filter(TherapySession::isAwaitingPayment)
                .toList();
        sessions.forEach(s -> s.setAwaitingPayment(false));
        sessionRepository.saveAll(sessions);
    }
}
