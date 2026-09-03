package com.simplehearing.reassignment.job;

import com.simplehearing.reassignment.entity.TherapistReassignment;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;
import com.simplehearing.reassignment.repository.TherapistReassignmentRepository;
import com.simplehearing.reassignment.service.TherapistReassignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Hands back every temporary reassignment whose window has closed. */
@Component
public class ReassignmentRevertJob {

    private static final Logger log = LoggerFactory.getLogger(ReassignmentRevertJob.class);

    private final TherapistReassignmentRepository reassignmentRepository;
    private final TherapistReassignmentService reassignmentService;

    public ReassignmentRevertJob(TherapistReassignmentRepository reassignmentRepository,
                                 TherapistReassignmentService reassignmentService) {
        this.reassignmentRepository = reassignmentRepository;
        this.reassignmentService = reassignmentService;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void revertExpired() {
        List<TherapistReassignment> expired = reassignmentRepository.findByStatusAndTypeAndEndDateLessThanEqual(
                ReassignmentStatus.ACTIVE, ReassignmentType.TEMPORARY, LocalDate.now());

        if (expired.isEmpty()) return;

        log.info("Reverting {} expired temporary reassignment(s)", expired.size());
        for (TherapistReassignment batch : expired) {
            reassignmentService.revert(batch, false, null);
        }
    }
}
