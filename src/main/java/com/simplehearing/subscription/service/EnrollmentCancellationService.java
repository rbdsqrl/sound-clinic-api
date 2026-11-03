package com.simplehearing.subscription.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.review.service.ReviewMeetingService;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cancels every still-open enrollment tied to a subscription, and their still-ahead sessions and
 * review meetings, the moment the subscription itself is cancelled (SubscriptionController#cancel), and
 * a single enrollment the same way when it is cancelled on its own (EnrollmentController#cancel). Mirrors PatientService#setActive's
 * cancel-cascade (same "still ahead" date/status filter), just scoped to one subscription's
 * enrollment(s) instead of every enrollment a patient has. A cancelled subscription has no
 * restore path, so unlike that cascade this one doesn't need a dedicated "cancelled by X" marker
 * for anything to restore from later.
 */
@Service
public class EnrollmentCancellationService {

    private final EnrollmentRepository enrollmentRepository;
    private final TherapySessionRepository sessionRepository;
    private final ReviewMeetingRepository reviewMeetingRepository;
    private final ReviewMeetingService reviewMeetingService;

    public EnrollmentCancellationService(EnrollmentRepository enrollmentRepository,
                                         TherapySessionRepository sessionRepository,
                                         ReviewMeetingRepository reviewMeetingRepository,
                                         ReviewMeetingService reviewMeetingService) {
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.reviewMeetingService = reviewMeetingService;
    }

    @Transactional
    public void cancelForSubscription(UUID subscriptionId) {
        List<Enrollment> enrollments = enrollmentRepository.findBySubscriptionId(subscriptionId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();
        cancelAll(enrollments);
    }

    /** Cancels one enrollment together with its still-ahead sessions and review meetings. */
    @Transactional
    public void cancelEnrollment(Enrollment enrollment) {
        cancelAll(List.of(enrollment));
    }

    private void cancelAll(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) return;
        cancelUpcoming(enrollments, "Program cancelled");
        enrollments.forEach(e -> e.setStatus(EnrollmentStatus.CANCELLED));
        enrollmentRepository.saveAll(enrollments);
    }

    /**
     * Cancels the still-ahead sessions and review meetings of these enrollments without changing the enrollments
     * themselves — for a close-out (such as a discharge) that sets its own enrollment status.
     */
    @Transactional
    public void cancelUpcoming(List<Enrollment> enrollments, String meetingReason) {
        if (enrollments.isEmpty()) return;

        LocalDate today = LocalDate.now();
        List<UUID> enrollmentIds = enrollments.stream().map(Enrollment::getId).toList();

        List<TherapySession> stillAhead = sessionRepository.findByEnrollmentIdIn(enrollmentIds).stream()
                .filter(s -> !s.getSessionDate().isBefore(today))
                .filter(s -> s.getStatus() == TherapySessionStatus.SCHEDULED
                        || s.getStatus() == TherapySessionStatus.PENDING_RESCHEDULE
                        || s.getStatus() == TherapySessionStatus.CANCELLATION_REQUESTED)
                .toList();
        stillAhead.forEach(s -> s.setStatus(TherapySessionStatus.CANCELLED));
        sessionRepository.saveAll(stillAhead);

        for (Enrollment e : enrollments) {
            cancelUpcomingMeetings(e.getId(), meetingReason);
        }
    }

    /** Cancels one enrollment's still-ahead scheduled review meetings. */
    @Transactional
    public void cancelUpcomingMeetings(UUID enrollmentId, String reason) {
        LocalDate today = LocalDate.now();
        reviewMeetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(enrollmentId).stream()
                .filter(m -> !m.getMeetingDate().isBefore(today))
                .filter(m -> m.getStatus() == ReviewMeetingStatus.SCHEDULED)
                .toList()
                .forEach(m -> reviewMeetingService.cancel(m, reason));
    }
}
