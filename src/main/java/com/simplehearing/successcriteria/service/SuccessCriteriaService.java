package com.simplehearing.successcriteria.service;

import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPGoalProgress;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.repository.IEPGoalProgressRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.successcriteria.dto.SuccessCriteriaResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The three axes a discharge is measured against: goal mastery (from IEP trial logs on plans
 * linked to the enrollment), therapist sign-off (an explicit confirmation), and parent
 * satisfaction (average perceived-progress rating from review meetings). Thresholds are
 * per-org, configurable, defaulting to 90% / 70% (see {@link Organisation}).
 */
@Service
public class SuccessCriteriaService {

    private final EnrollmentRepository enrollmentRepository;
    private final IEPPlanRepository iepPlanRepository;
    private final IEPGoalRepository iepGoalRepository;
    private final IEPGoalProgressRepository iepGoalProgressRepository;
    private final ReviewMeetingRepository reviewMeetingRepository;
    private final OrganisationRepository organisationRepository;

    public SuccessCriteriaService(
            EnrollmentRepository enrollmentRepository,
            IEPPlanRepository iepPlanRepository,
            IEPGoalRepository iepGoalRepository,
            IEPGoalProgressRepository iepGoalProgressRepository,
            ReviewMeetingRepository reviewMeetingRepository,
            OrganisationRepository organisationRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.iepPlanRepository = iepPlanRepository;
        this.iepGoalRepository = iepGoalRepository;
        this.iepGoalProgressRepository = iepGoalProgressRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.organisationRepository = organisationRepository;
    }

    public SuccessCriteriaResponse compute(UUID orgId, UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));

        // ── Goal mastery ──────────────────────────────────────────────────────
        List<IEPPlan> plans = iepPlanRepository.findByEnrollmentId(enrollmentId);
        List<UUID> planIds = plans.stream().map(IEPPlan::getId).toList();
        List<IEPGoal> goals = planIds.isEmpty() ? List.of() : iepGoalRepository.findByPlanIdIn(planIds);
        List<UUID> goalIds = goals.stream().map(IEPGoal::getId).toList();
        List<IEPGoalProgress> progress = goalIds.isEmpty() ? List.of() : iepGoalProgressRepository.findByGoalIdIn(goalIds);

        long trialsPassed = 0;
        long trialsTotal = 0;
        for (IEPGoalProgress p : progress) {
            if (p.getTrialsTotal() == null || p.getTrialsTotal() <= 0) continue;
            trialsTotal += p.getTrialsTotal();
            trialsPassed += p.getTrialsPassed() == null ? 0 : Math.min(p.getTrialsPassed(), p.getTrialsTotal());
        }
        // Not computable (no linked plan, or no trial data) — surfaced as null, never as a false failure.
        Double goalMasteryPct = trialsTotal > 0
                ? Math.round((100.0 * trialsPassed / trialsTotal) * 10.0) / 10.0
                : null;
        Boolean goalMasteryMet = goalMasteryPct != null ? goalMasteryPct >= org.getGoalMasteryThresholdPct() : null;

        // ── Parent satisfaction ───────────────────────────────────────────────
        List<ReviewMeeting> meetings = reviewMeetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(enrollmentId);
        List<Integer> progressRatings = meetings.stream()
                .map(ReviewMeeting::getProgressRatingPct)
                .filter(r -> r != null)
                .toList();
        Double parentSatisfactionPct = progressRatings.isEmpty() ? null
                : progressRatings.stream().mapToInt(Integer::intValue).average().orElse(0);
        Boolean parentSatisfactionMet = parentSatisfactionPct != null
                ? parentSatisfactionPct >= org.getParentSatisfactionThresholdPct() : null;

        boolean overallSuccessful = Boolean.TRUE.equals(goalMasteryMet)
                && enrollment.isTherapistSignedOff()
                && Boolean.TRUE.equals(parentSatisfactionMet);

        return new SuccessCriteriaResponse(
                goalMasteryPct, goalMasteryMet,
                enrollment.isTherapistSignedOff(),
                parentSatisfactionPct, parentSatisfactionMet,
                overallSuccessful);
    }
}
