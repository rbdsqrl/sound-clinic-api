package com.simplehearing.session.job;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * A session flagged PENDING_RESCHEDULE (by leave, a public holiday, or a parent's request) is a
 * plan to pick a new date, not a booking that happened — if nobody actions it before its
 * original date goes by, it was never held. This sweep cancels those so they stop cluttering
 * the "Needs Rescheduling" dashboard card indefinitely; recovering one is a deliberate, separate
 * action from the patient's case (an ad-hoc booking), not an automatic in-place move.
 */
@Component
public class MissedRescheduleCancelJob {

    private static final Logger log = LoggerFactory.getLogger(MissedRescheduleCancelJob.class);

    private final TherapySessionRepository sessionRepository;

    public MissedRescheduleCancelJob(TherapySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(cron = "0 20 0 * * *")
    @Transactional
    public void cancelMissed() {
        List<TherapySession> missed = sessionRepository.findByStatusAndSessionDateBefore(
                TherapySessionStatus.PENDING_RESCHEDULE, LocalDate.now());

        if (missed.isEmpty()) return;

        log.info("Auto-cancelling {} PENDING_RESCHEDULE session(s) whose date has passed unaddressed", missed.size());
        missed.forEach(s -> s.setStatus(TherapySessionStatus.CANCELLED));
        sessionRepository.saveAll(missed);
    }
}
