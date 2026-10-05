package com.simplehearing.iep.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.iep.dto.GoalPacingResponse;
import com.simplehearing.iep.dto.GoalPacingResponse.GoalPace;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.enums.IEPGoalStatus;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Spreads a plan's active goals across the upcoming sessions of the therapy it's linked to. */
@Service
public class GoalPacingService {

    private static final Set<TherapySessionStatus> UPCOMING = EnumSet.of(
            TherapySessionStatus.SCHEDULED, TherapySessionStatus.PENDING_RESCHEDULE, TherapySessionStatus.CANCELLATION_REQUESTED);

    private final IEPPlanRepository planRepository;
    private final IEPGoalRepository goalRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TherapySessionRepository sessionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;

    public GoalPacingService(IEPPlanRepository planRepository, IEPGoalRepository goalRepository,
                             EnrollmentRepository enrollmentRepository, TherapySessionRepository sessionRepository,
                             SubscriptionRepository subscriptionRepository, ProgramRepository programRepository) {
        this.planRepository = planRepository;
        this.goalRepository = goalRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
    }

    public GoalPacingResponse pacing(UUID planId, UUID orgId) {
        return compute(requirePlan(planId, orgId), orgId, LocalDate.now());
    }

    /** Writes the suggested target dates onto the plan's active goals (completed goals are left alone). */
    @Transactional
    public GoalPacingResponse apply(UUID planId, UUID orgId) {
        IEPPlan plan = requirePlan(planId, orgId);
        LocalDate today = LocalDate.now();
        GoalPacingResponse current = compute(plan, orgId, today);
        if (!current.linked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Link this plan to an ongoing therapy first — goals are spread across that therapy's sessions");
        }
        if (current.remainingSessions() == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "There are no upcoming sessions left to spread the goals across");
        }

        Map<UUID, LocalDate> suggested = new HashMap<>();
        current.goals().forEach(g -> { if (g.suggestedTargetDate() != null) suggested.put(g.goalId(), g.suggestedTargetDate()); });
        List<IEPGoal> goals = goalRepository.findByPlanIdOrderByCreatedAtAsc(planId);
        goals.stream().filter(g -> suggested.containsKey(g.getId())).forEach(g -> g.setTargetDate(suggested.get(g.getId())));
        goalRepository.saveAll(goals);
        return compute(plan, orgId, today);
    }

    private IEPPlan requirePlan(UUID planId, UUID orgId) {
        return planRepository.findByIdAndOrgId(planId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("IEP plan not found"));
    }

    private GoalPacingResponse compute(IEPPlan plan, UUID orgId, LocalDate today) {
        Enrollment enrollment = plan.getEnrollmentId() == null ? null
                : enrollmentRepository.findById(plan.getEnrollmentId()).filter(e -> orgId.equals(e.getOrgId())).orElse(null);
        if (enrollment == null) {
            return new GoalPacingResponse(false, plan.getId(), null, null, 0, 0, 0, 0, 0, false,
                    "This plan isn't linked to an ongoing therapy.", List.of());
        }

        List<TherapySession> sessions = sessionRepository.findByEnrollmentIdOrderBySessionNumberAsc(enrollment.getId()).stream()
                .filter(TherapySession::isCountsTowardPlan)
                .filter(s -> s.getStatus() != TherapySessionStatus.CANCELLED)
                .sorted(Comparator.comparing(TherapySession::getSessionDate).thenComparing(TherapySession::getStartTime))
                .toList();
        int completed = (int) sessions.stream().filter(s -> s.getStatus() == TherapySessionStatus.COMPLETED).count();
        // Upcoming sessions only — a scheduled session that's already in the past was never recorded, so it isn't capacity.
        List<TherapySession> remaining = sessions.stream()
                .filter(s -> UPCOMING.contains(s.getStatus()) && !s.getSessionDate().isBefore(today)).toList();

        List<IEPGoal> allGoals = goalRepository.findByPlanIdOrderByCreatedAtAsc(plan.getId());
        List<IEPGoal> active = allGoals.stream().filter(g -> g.getStatus() != IEPGoalStatus.COMPLETED).toList();

        // Share the remaining sessions between the active goals in order: everyone gets floor(R/n), the first
        // R mod n goals get one more. A goal's suggested date is the date of the last session in its share.
        Map<UUID, int[]> budgetAndEnd = new HashMap<>();   // goalId -> {budget, index of last session in its share (-1 = none)}
        int n = active.size(), r = remaining.size(), cursor = 0;
        for (int i = 0; i < n; i++) {
            int budget = n == 0 ? 0 : r / n + (i < r % n ? 1 : 0);
            cursor += budget;
            budgetAndEnd.put(active.get(i).getId(), new int[]{budget, budget == 0 ? r - 1 : cursor - 1});
        }

        List<GoalPace> paces = new ArrayList<>();
        for (IEPGoal g : allGoals) {
            if (g.getStatus() == IEPGoalStatus.COMPLETED) {
                paces.add(new GoalPace(g.getId(), g.getTitle(), g.getStatus(), null, null, g.getTargetDate(), false, false));
                continue;
            }
            int[] be = budgetAndEnd.get(g.getId());
            LocalDate suggested = (be == null || be[1] < 0 || remaining.isEmpty()) ? null : remaining.get(Math.min(be[1], r - 1)).getSessionDate();
            LocalDate current = g.getTargetDate();
            boolean overdue = current != null && current.isBefore(today);
            boolean differs = suggested != null && (current == null || Math.abs(ChronoUnit.DAYS.between(current, suggested)) > 7);
            paces.add(new GoalPace(g.getId(), g.getTitle(), g.getStatus(), be == null ? 0 : be[0], suggested, current, overdue, differs));
        }

        // Sessions per week: the therapy's chosen session days, or — when it has none — what its sessions actually add up to.
        double perWeek = enrollment.getSessionDays().isEmpty() ? observedPerWeek(sessions) : enrollment.getSessionDays().size();
        boolean overloaded = n > 0 && r < n * GoalPacingResponse.MIN_SESSIONS_PER_GOAL;
        return new GoalPacingResponse(true, plan.getId(), enrollment.getId(), programName(enrollment), perWeek,
                sessions.size(), completed, r, n, overloaded, summary(n, r, overloaded), paces);
    }

    private static double observedPerWeek(List<TherapySession> sessions) {
        if (sessions.size() < 2) return 0;
        long days = ChronoUnit.DAYS.between(sessions.get(0).getSessionDate(), sessions.get(sessions.size() - 1).getSessionDate());
        double perWeek = sessions.size() / Math.max(1.0, days / 7.0);
        return Math.round(perWeek * 10.0) / 10.0;
    }

    private String programName(Enrollment e) {
        return subscriptionRepository.findById(e.getSubscriptionId())
                .flatMap(s -> programRepository.findById(s.getProgramId()))
                .map(p -> p.getName()).orElse(null);
    }

    private static String summary(int goals, int sessionsLeft, boolean overloaded) {
        if (goals == 0) return "No active goals to spread.";
        if (sessionsLeft == 0) return goals + " active goal" + (goals == 1 ? "" : "s") + " but no upcoming sessions left in this therapy.";
        String each = String.format(Locale.ROOT, "%.1f", (double) sessionsLeft / goals).replace(".0", "");
        return sessionsLeft + " session" + (sessionsLeft == 1 ? "" : "s") + " left for " + goals + " goal" + (goals == 1 ? "" : "s")
                + " — about " + each + " each." + (overloaded ? " That's tight: consider fewer goals or more sessions." : "");
    }
}
