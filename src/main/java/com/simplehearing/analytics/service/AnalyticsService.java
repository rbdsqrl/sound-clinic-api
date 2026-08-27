package com.simplehearing.analytics.service;

import com.simplehearing.activity.entity.Activity;
import com.simplehearing.activity.entity.ActivityAssignment;
import com.simplehearing.activity.entity.ActivityAttemptLog;
import com.simplehearing.activity.entity.ActivitySkill;
import com.simplehearing.activity.entity.Skill;
import com.simplehearing.activity.enums.AssignmentStatus;
import com.simplehearing.activity.repository.ActivityAssignmentRepository;
import com.simplehearing.activity.repository.ActivityAttemptLogRepository;
import com.simplehearing.activity.repository.ActivityRepository;
import com.simplehearing.activity.repository.ActivitySkillRepository;
import com.simplehearing.activity.repository.SkillRepository;
import com.simplehearing.analytics.dto.ActivityProgressResponse;
import com.simplehearing.analytics.dto.CaseloadResponse;
import com.simplehearing.analytics.dto.EngagementOverviewResponse;
import com.simplehearing.analytics.dto.FrequencyResponse;
import com.simplehearing.analytics.dto.OrgSnapshotResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse.Bucket;
import com.simplehearing.analytics.dto.TimeSeriesResponse.DomainSeries;
import com.simplehearing.analytics.dto.TimeSeriesResponse.SubjectType;
import com.simplehearing.analytics.dto.TimeSeriesResponse.Totals;
import com.simplehearing.analytics.enums.Granularity;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.invitation.entity.Invitation;
import com.simplehearing.invitation.repository.InvitationRepository;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPGoalProgress;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.enums.IEPGoalStatus;
import com.simplehearing.iep.repository.IEPGoalProgressRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the daily / weekly / monthly progress series from what therapists already record.
 *
 * <p><b>Why the folding happens in Java rather than SQL.</b> This mirrors the existing
 * precedent in {@code InquiryController.analytics}: fetch the rows for a bounded window,
 * fold them with streams. Keeping the bucketing in {@code java.time} puts ISO week rules and
 * the null-vs-zero distinction in one readable place rather than spreading them across SQL
 * date functions, and the windows are capped at {@link #MAX_WINDOW_DAYS} with supporting
 * indexes from migration 045. Move to SQL {@code GROUP BY date_trunc} with projection
 * interfaces once a single window starts returning tens of thousands of rows.
 */
@Service
public class AnalyticsService {

    /** Widest window a single request may ask for, so one call cannot scan years of rows. */
    private static final int MAX_WINDOW_DAYS = 731;

    private final TherapySessionRepository  sessionRepository;
    private final IEPGoalProgressRepository progressRepository;
    private final IEPGoalRepository         goalRepository;
    private final IEPPlanRepository         planRepository;
    private final ReviewMeetingRepository   reviewMeetingRepository;
    private final PatientRepository         patientRepository;
    private final UserRepository            userRepository;
    private final OrganisationRepository    organisationRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityAttemptLogRepository activityAttemptLogRepository;
    private final EnrollmentRepository      enrollmentRepository;
    private final SubscriptionRepository    subscriptionRepository;
    private final ProgramRepository         programRepository;
    private final InvitationRepository      invitationRepository;
    private final ActivityRepository        activityRepository;
    private final SkillRepository           skillRepository;
    private final ActivitySkillRepository   activitySkillRepository;

    public AnalyticsService(TherapySessionRepository sessionRepository,
                            IEPGoalProgressRepository progressRepository,
                            IEPGoalRepository goalRepository,
                            IEPPlanRepository planRepository,
                            ReviewMeetingRepository reviewMeetingRepository,
                            PatientRepository patientRepository,
                            UserRepository userRepository,
                            OrganisationRepository organisationRepository,
                            ActivityAssignmentRepository activityAssignmentRepository,
                            ActivityAttemptLogRepository activityAttemptLogRepository,
                            EnrollmentRepository enrollmentRepository,
                            SubscriptionRepository subscriptionRepository,
                            ProgramRepository programRepository,
                            InvitationRepository invitationRepository,
                            ActivityRepository activityRepository,
                            SkillRepository skillRepository,
                            ActivitySkillRepository activitySkillRepository) {
        this.sessionRepository       = sessionRepository;
        this.progressRepository      = progressRepository;
        this.goalRepository          = goalRepository;
        this.planRepository          = planRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.patientRepository       = patientRepository;
        this.userRepository          = userRepository;
        this.organisationRepository  = organisationRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityAttemptLogRepository = activityAttemptLogRepository;
        this.enrollmentRepository    = enrollmentRepository;
        this.subscriptionRepository  = subscriptionRepository;
        this.programRepository       = programRepository;
        this.invitationRepository    = invitationRepository;
        this.activityRepository      = activityRepository;
        this.skillRepository         = skillRepository;
        this.activitySkillRepository = activitySkillRepository;
    }

    /** Additive companion to {@link #patientProgress} — activity assignment/attempt counts for
     *  a patient. Kept separate from the mastery/domain folding above (own inputs, own query path). */
    public ActivityProgressResponse patientActivityProgress(UUID orgId, UUID patientId, LocalDate from, LocalDate to) {
        validateWindow(from, to);

        List<ActivityAssignment> assignments = activityAssignmentRepository
                .findByOrgIdAndPatientIdOrderByCreatedAtDesc(orgId, patientId);

        int assigned = 0, inProgress = 0, completed = 0, discontinued = 0;
        for (ActivityAssignment a : assignments) {
            switch (a.getStatus()) {
                case ASSIGNED -> assigned++;
                case IN_PROGRESS -> inProgress++;
                case COMPLETED -> completed++;
                case DISCONTINUED -> discontinued++;
            }
        }
        Double completionRate = assignments.isEmpty() ? null : (100.0 * completed / assignments.size());

        List<UUID> assignmentIds = assignments.stream().map(ActivityAssignment::getId).toList();
        List<ActivityAttemptLog> attempts = assignmentIds.isEmpty() ? List.of() :
                activityAttemptLogRepository.findByOrgIdAndAssignmentIdInAndAttemptDateBetween(orgId, assignmentIds, from, to);

        Map<LocalDate, Integer> byWeek = new TreeMap<>();
        for (ActivityAttemptLog log : attempts) {
            LocalDate weekStart = log.getAttemptDate().with(java.time.DayOfWeek.MONDAY);
            byWeek.merge(weekStart, 1, Integer::sum);
        }
        List<ActivityProgressResponse.WeeklyAttemptPoint> weekly = byWeek.entrySet().stream()
                .map(e -> new ActivityProgressResponse.WeeklyAttemptPoint(e.getKey(), e.getValue()))
                .toList();

        return new ActivityProgressResponse(assigned, inProgress, completed, discontinued, completionRate, weekly);
    }

    /**
     * Session cadence for one patient, folded across every enrollment they have — a patient can
     * have two programs running concurrently, each generating its own sessions on the same week
     * (or even the same day). Attribution is solid by construction: each session belongs to
     * exactly one enrollment already, so summing per week across enrollments is correct — the
     * risk was only ever in presentation, not counting.
     */
    public FrequencyResponse patientSessionFrequency(UUID orgId, UUID patientId, LocalDate from, LocalDate to) {
        validateWindow(from, to);

        List<TherapySession> sessions = sessionRepository
                .findByOrgIdAndPatientIdBetween(orgId, patientId, from, to)
                .stream()
                .filter(s -> s.getStatus() != TherapySessionStatus.CANCELLED)
                .toList();

        Set<UUID> enrollmentIds = sessions.stream().map(TherapySession::getEnrollmentId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Enrollment> enrollmentMap = enrollmentRepository.findAllById(enrollmentIds).stream()
                .collect(java.util.stream.Collectors.toMap(Enrollment::getId, e -> e));

        Map<UUID, String> programNameByEnrollment = new HashMap<>();
        for (Enrollment e : enrollmentMap.values()) {
            subscriptionRepository.findById(e.getSubscriptionId()).ifPresent(sub ->
                    programRepository.findById(sub.getProgramId()).ifPresent(prog ->
                            programNameByEnrollment.put(e.getId(), prog.getName())));
        }

        Map<LocalDate, List<TherapySession>> byWeek = new TreeMap<>();
        for (TherapySession s : sessions) {
            LocalDate weekStart = s.getSessionDate().with(java.time.DayOfWeek.MONDAY);
            byWeek.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(s);
        }

        List<FrequencyResponse.WeeklyFrequency> weekly = new ArrayList<>();
        for (Map.Entry<LocalDate, List<TherapySession>> entry : byWeek.entrySet()) {
            List<TherapySession> weekSessions = entry.getValue();
            int planCount = (int) weekSessions.stream().filter(s -> !s.isAdHoc()).count();
            int adHocCount = weekSessions.size() - planCount;

            Map<String, Integer> byProgram = new LinkedHashMap<>();
            for (TherapySession s : weekSessions) {
                String programName = programNameByEnrollment.getOrDefault(s.getEnrollmentId(), "Unknown Program");
                byProgram.merge(programName, 1, Integer::sum);
            }
            List<FrequencyResponse.ProgramCount> programCounts = byProgram.entrySet().stream()
                    .map(e -> new FrequencyResponse.ProgramCount(e.getKey(), e.getValue()))
                    .toList();

            weekly.add(new FrequencyResponse.WeeklyFrequency(entry.getKey(), weekSessions.size(), planCount, adHocCount, programCounts));
        }

        Map<String, Integer> totalsByProgram = new LinkedHashMap<>();
        for (TherapySession s : sessions) {
            String programName = programNameByEnrollment.getOrDefault(s.getEnrollmentId(), "Unknown Program");
            totalsByProgram.merge(programName, 1, Integer::sum);
        }
        List<FrequencyResponse.ProgramTotal> byProgramTotals = totalsByProgram.entrySet().stream()
                .map(e -> new FrequencyResponse.ProgramTotal(e.getKey(), e.getValue()))
                .toList();

        return new FrequencyResponse(weekly, byProgramTotals);
    }

    /**
     * Org-wide clinical-outcome rollup: average therapy duration, children by program, and
     * the current admission → discharge funnel. All "right now" figures, not a windowed trend,
     * so this is deliberately separate from {@link #orgOverview}.
     */
    public OrgSnapshotResponse orgSnapshot(UUID orgId) {
        List<Enrollment> enrollments = enrollmentRepository.findByOrgId(orgId);

        // Average duration — only enrollments with a known end date have a defined span.
        List<Long> durationsDays = enrollments.stream()
                .filter(e -> e.getEndDate() != null)
                .map(e -> ChronoUnit.DAYS.between(e.getStartDate(), e.getEndDate()))
                .filter(d -> d >= 0)
                .toList();
        Double avgDurationWeeks = durationsDays.isEmpty() ? null
                : Math.round((durationsDays.stream().mapToLong(Long::longValue).average().orElse(0) / 7.0) * 10.0) / 10.0;

        // Program breakdown — resolve subscription -> program in bulk rather than per-enrollment.
        Set<UUID> subscriptionIds = enrollments.stream().map(Enrollment::getSubscriptionId).collect(Collectors.toSet());
        Map<UUID, UUID> programIdBySubscription = subscriptionRepository.findAllById(subscriptionIds).stream()
                .collect(Collectors.toMap(Subscription::getId, Subscription::getProgramId));
        Set<UUID> programIds = new HashSet<>(programIdBySubscription.values());
        Map<UUID, String> programNames = programRepository.findAllById(programIds).stream()
                .collect(Collectors.toMap(Program::getId, Program::getName));

        Map<String, Set<UUID>> patientsByProgram = new LinkedHashMap<>();
        Map<String, Integer> enrollmentCountByProgram = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            UUID programId = programIdBySubscription.get(e.getSubscriptionId());
            String programName = programId != null ? programNames.getOrDefault(programId, "Unknown Program") : "Unknown Program";
            patientsByProgram.computeIfAbsent(programName, k -> new HashSet<>()).add(e.getPatientId());
            enrollmentCountByProgram.merge(programName, 1, Integer::sum);
        }
        List<OrgSnapshotResponse.ProgramBreakdown> programBreakdown = patientsByProgram.entrySet().stream()
                .map(en -> new OrgSnapshotResponse.ProgramBreakdown(
                        en.getKey(), en.getValue().size(), enrollmentCountByProgram.get(en.getKey())))
                .sorted(Comparator.comparingInt(OrgSnapshotResponse.ProgramBreakdown::patientCount).reversed())
                .toList();

        // Admission -> discharge funnel — every stage shown, zero-filled, in the funnel's own order.
        Map<PatientStage, Integer> counts = new HashMap<>();
        for (Patient p : patientRepository.findByOrgId(orgId)) {
            counts.merge(p.getStage(), 1, Integer::sum);
        }
        List<OrgSnapshotResponse.StageCount> stageCounts = List.of(PatientStage.values()).stream()
                .map(s -> new OrgSnapshotResponse.StageCount(s, counts.getOrDefault(s, 0)))
                .toList();

        return new OrgSnapshotResponse(avgDurationWeeks, durationsDays.size(), programBreakdown, stageCounts);
    }

    /** Session count per calendar day in the window — feeds the GitHub-style activity heatmap. */
    public List<EngagementOverviewResponse.TrendPoint> sessionHeatmap(UUID orgId, LocalDate from, LocalDate to) {
        validateWindow(from, to);
        List<TherapySession> sessions = sessionRepository
                .findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(orgId, from, to);

        Map<LocalDate, Integer> byDay = new TreeMap<>();
        for (TherapySession s : sessions) {
            byDay.merge(s.getSessionDate(), 1, Integer::sum);
        }
        return byDay.entrySet().stream()
                .map(e -> new EngagementOverviewResponse.TrendPoint(e.getKey(), e.getValue()))
                .toList();
    }

    private static final List<int[]> AGE_BANDS = List.of(
            new int[]{0, 2}, new int[]{3, 5}, new int[]{6, 8}, new int[]{9, 11},
            new int[]{12, 14}, new int[]{15, Integer.MAX_VALUE});

    /**
     * Org-wide engagement rollup for the "Overview" analytics tab — active/invited user counts,
     * average session length, skills and age-group breakdowns, session/checklist trends, and the
     * activities patients are assigned most. Distinct from {@link #orgSnapshot} (clinical-outcome,
     * point-in-time) and {@link #orgOverview} (goal-mastery trend) — this is activity/engagement.
     */
    public EngagementOverviewResponse engagementOverview(UUID orgId, LocalDate from, LocalDate to) {
        validateWindow(from, to);

        // Active users
        List<User> staff = userRepository.findByOrgIdAndRoleIn(orgId,
                List.of(Role.THERAPIST, Role.DOCTOR, Role.CLINIC_HEAD, Role.BUSINESS_OWNER));
        List<Patient> patients = patientRepository.findByOrgId(orgId);
        EngagementOverviewResponse.UserCounts activeUsers =
                new EngagementOverviewResponse.UserCounts(staff.size(), patients.size());

        // Invited users — pending invitations, split staff vs parent (a parent invite is patient-linked)
        List<Invitation> pending = invitationRepository.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .filter(i -> i.getStatus() == Invitation.Status.PENDING)
                .toList();
        long pendingMembers = pending.stream().filter(i -> i.getRole() != Role.PARENT).count();
        long pendingCases = pending.stream().filter(i -> i.getRole() == Role.PARENT).count();
        EngagementOverviewResponse.UserCounts invitedUsers =
                new EngagementOverviewResponse.UserCounts((int) pendingMembers, (int) pendingCases);

        // Sessions in window — backs the trend, total, average duration and heatmap-adjacent figures
        List<TherapySession> sessions = sessionRepository
                .findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(orgId, from, to);

        Map<LocalDate, Integer> sessionsByDay = new TreeMap<>();
        List<Long> durations = new ArrayList<>();
        for (TherapySession s : sessions) {
            sessionsByDay.merge(s.getSessionDate(), 1, Integer::sum);
            durations.add(java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes());
        }
        List<EngagementOverviewResponse.TrendPoint> sessionsTrend = sessionsByDay.entrySet().stream()
                .map(e -> new EngagementOverviewResponse.TrendPoint(e.getKey(), e.getValue()))
                .toList();
        Integer avgDurationMinutes = durations.isEmpty() ? null
                : (int) Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0));

        // Age groups — zero-filled bands so a thin org doesn't drop a bar silently
        int[] ageCounts = new int[AGE_BANDS.size()];
        LocalDate today = LocalDate.now();
        for (Patient p : patients) {
            if (p.getDateOfBirth() == null) continue;
            int age = java.time.Period.between(p.getDateOfBirth(), today).getYears();
            for (int i = 0; i < AGE_BANDS.size(); i++) {
                if (age >= AGE_BANDS.get(i)[0] && age <= AGE_BANDS.get(i)[1]) { ageCounts[i]++; break; }
            }
        }
        List<EngagementOverviewResponse.NameCount> ageGroups = new ArrayList<>();
        String[] ageLabels = {"0-2", "3-5", "6-8", "9-11", "12-14", "15+"};
        for (int i = 0; i < ageLabels.length; i++) {
            ageGroups.add(new EngagementOverviewResponse.NameCount(ageLabels[i], ageCounts[i]));
        }

        // Skills breakdown — how often each skill's activities have been assigned
        List<Activity> activities = activityRepository.findByOrgIdOrderByCreatedAtDesc(orgId);
        List<EngagementOverviewResponse.NameCount> skillsBreakdown = skillsBreakdown(orgId, activities);

        // Every assignment in the org backs both "most assigned activities" and, via its attempt
        // logs, the "checklist filled" trend.
        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByOrgId(orgId);
        Map<UUID, String> activityTitles = activities.stream()
                .collect(Collectors.toMap(Activity::getId, Activity::getTitle));
        Map<UUID, Integer> assignmentCountByActivity = new LinkedHashMap<>();
        for (ActivityAssignment a : allAssignments) {
            assignmentCountByActivity.merge(a.getActivityId(), 1, Integer::sum);
        }
        List<EngagementOverviewResponse.NameCount> mostAssigned = assignmentCountByActivity.entrySet().stream()
                .map(e -> new EngagementOverviewResponse.NameCount(
                        activityTitles.getOrDefault(e.getKey(), "Unknown Activity"), e.getValue()))
                .sorted(Comparator.comparingInt(EngagementOverviewResponse.NameCount::count).reversed())
                .limit(5)
                .toList();

        List<UUID> assignmentIds = allAssignments.stream().map(ActivityAssignment::getId).toList();
        Map<LocalDate, Integer> checklistByDay = new TreeMap<>();
        if (!assignmentIds.isEmpty()) {
            List<ActivityAttemptLog> attempts = activityAttemptLogRepository
                    .findByOrgIdAndAssignmentIdInAndAttemptDateBetween(orgId, assignmentIds, from, to);
            for (ActivityAttemptLog log : attempts) {
                checklistByDay.merge(log.getAttemptDate(), 1, Integer::sum);
            }
        }
        List<EngagementOverviewResponse.TrendPoint> checklistFilledTrend = checklistByDay.entrySet().stream()
                .map(e -> new EngagementOverviewResponse.TrendPoint(e.getKey(), e.getValue()))
                .toList();

        return new EngagementOverviewResponse(
                activeUsers, invitedUsers, avgDurationMinutes, skillsBreakdown, ageGroups,
                sessionsTrend, sessions.size(), checklistFilledTrend, mostAssigned);
    }

    private List<EngagementOverviewResponse.NameCount> skillsBreakdown(UUID orgId, List<Activity> activities) {
        List<UUID> activityIds = activities.stream().map(Activity::getId).toList();
        if (activityIds.isEmpty()) return List.of();
        List<ActivitySkill> links = activitySkillRepository.findByActivityIdIn(activityIds);
        Map<UUID, String> skillNames = skillRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(orgId).stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ActivitySkill link : links) {
            String name = skillNames.get(link.getSkillId());
            if (name == null) continue;
            counts.merge(name, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(e -> new EngagementOverviewResponse.NameCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(EngagementOverviewResponse.NameCount::count).reversed())
                .toList();
    }

    // ── Public entry points ──────────────────────────────────────────────────

    /** One child's progress: mastery trend, per-domain small multiples, attendance and coverage. */
    public TimeSeriesResponse patientProgress(UUID orgId, UUID patientId, Granularity granularity,
                                              LocalDate from, LocalDate to, String domainFilter) {
        validateWindow(from, to);

        Patient patient = patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        List<IEPGoal> goals = goalsForPlans(planRepository.findByOrgIdAndPatientIdOrderByCreatedAtDesc(orgId, patientId));

        Inputs inputs = new Inputs(
                sessionRepository.findByOrgIdAndPatientIdBetween(orgId, patientId, from, to),
                progressForGoals(goals, from, to),
                goals,
                reviewMeetingRepository.findByOrgIdAndPatientIdOrderByMeetingDateAsc(orgId, patientId));

        return assemble(SubjectType.PATIENT, patientId, fullName(patient.getFirstName(), patient.getLastName()),
                granularity, from, to, inputs, domainFilter);
    }

    /** A therapist's whole caseload — their own series plus a row per patient they are assigned to. */
    public CaseloadResponse therapistCaseload(UUID orgId, UUID therapistId, Granularity granularity,
                                              LocalDate from, LocalDate to, String domainFilter) {
        validateWindow(from, to);

        User therapist = userRepository.findById(therapistId)
                .filter(u -> orgId.equals(u.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        List<IEPPlan>  plans = planRepository.findByOrgIdAndTherapistId(orgId, therapistId);
        List<IEPGoal>  goals = goalsForPlans(plans);

        List<TherapySession> sessions = sessionRepository.findByOrgIdAndTherapistIdBetween(orgId, therapistId, from, to);

        Inputs inputs = new Inputs(
                sessions,
                progressForGoals(goals, from, to),
                goals,
                reviewMeetingRepository.findByOrgIdAndTherapistIdOrderByMeetingDateAsc(orgId, therapistId));

        TimeSeriesResponse series = assemble(
                SubjectType.THERAPIST, therapistId,
                fullName(therapist.getFirstName(), therapist.getLastName()),
                granularity, from, to, inputs, domainFilter);

        // Per-patient rows. The patient set is everyone the therapist either saw or planned for
        // in the window, so a child with sessions but no IEP still appears.
        Map<UUID, UUID> planToPatient = plans.stream()
                .collect(Collectors.toMap(IEPPlan::getId, IEPPlan::getPatientId, (a, b) -> a));

        List<UUID> patientIds = new ArrayList<>(new java.util.LinkedHashSet<>(
                java.util.stream.Stream.concat(
                        sessions.stream().map(TherapySession::getPatientId),
                        plans.stream().map(IEPPlan::getPatientId)
                ).toList()));

        Map<UUID, String> patientNames = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> fullName(p.getFirstName(), p.getLastName())));

        List<CaseloadResponse.PatientRow> rows = new ArrayList<>();
        for (UUID pid : patientIds) {
            List<IEPGoal> patientGoals = goals.stream()
                    .filter(g -> pid.equals(planToPatient.get(g.getPlanId())))
                    .toList();

            Inputs patientInputs = new Inputs(
                    sessions.stream().filter(s -> pid.equals(s.getPatientId())).toList(),
                    progressForGoals(patientGoals, from, to),
                    patientGoals,
                    List.of());

            TimeSeriesResponse s = assemble(SubjectType.PATIENT, pid,
                    patientNames.getOrDefault(pid, "Unknown"), granularity, from, to, patientInputs, domainFilter);

            rows.add(new CaseloadResponse.PatientRow(
                    pid,
                    patientNames.getOrDefault(pid, "Unknown"),
                    s.totals().masteryPct(),
                    s.totals().masteryDeltaPts(),
                    s.buckets().stream().map(Bucket::masteryPct).toList(),
                    s.totals().sessionsCompleted(),
                    s.totals().sessionsScheduled(),
                    s.totals().sessionsNoShow(),
                    s.totals().coveragePct(),
                    s.totals().goalsTotal(),
                    s.totals().goalsCompleted(),
                    isPlateau(s.totals().masteryDeltaPts(), populatedBuckets(s.buckets()))));
        }

        // Stalled and least-covered patients first — the rows that need attention.
        rows.sort(Comparator
                .comparing(CaseloadResponse.PatientRow::plateau, Comparator.reverseOrder())
                .thenComparing(r -> r.deltaPts() == null ? Double.MAX_VALUE : r.deltaPts()));

        return new CaseloadResponse(therapistId, fullName(therapist.getFirstName(), therapist.getLastName()),
                from, to, series, rows);
    }

    /** Org-wide rollup. Daily is rejected upstream — day-to-day org churn is noise. */
    public TimeSeriesResponse orgOverview(UUID orgId, Granularity granularity,
                                          LocalDate from, LocalDate to, String domainFilter) {
        validateWindow(from, to);

        String orgName = organisationRepository.findById(orgId)
                .map(Organisation::getName)
                .orElse("Organisation");

        Inputs inputs = new Inputs(
                sessionRepository.findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(orgId, from, to),
                progressRepository.findByOrgIdAndSessionDateBetween(orgId, from, to),
                goalRepository.findByOrgId(orgId),
                reviewMeetingRepository.findInRange(orgId, from, to));

        return assemble(SubjectType.ORGANISATION, orgId, orgName, granularity, from, to, inputs, domainFilter);
    }

    // ── Assembly ─────────────────────────────────────────────────────────────

    /** The raw rows a series is folded from. */
    private record Inputs(List<TherapySession> sessions,
                          List<IEPGoalProgress> progress,
                          List<IEPGoal> goals,
                          List<ReviewMeeting> meetings) {}

    private TimeSeriesResponse assemble(SubjectType type, UUID subjectId, String subjectName,
                                        Granularity g, LocalDate from, LocalDate to,
                                        Inputs in, String domainFilter) {

        // Contiguous, zero-filled bucket spine. A period with no data still gets a slot so the
        // frontend can draw a break rather than joining across the gap.
        List<LocalDate> starts = new ArrayList<>();
        for (LocalDate cur = g.bucketStart(from); !cur.isAfter(to); cur = g.next(cur)) {
            starts.add(cur);
        }
        Map<LocalDate, Integer> indexOf = new HashMap<>();
        for (int i = 0; i < starts.size(); i++) indexOf.put(starts.get(i), i);
        int n = starts.size();

        Map<UUID, IEPGoal> goalsById = in.goals().stream()
                .collect(Collectors.toMap(IEPGoal::getId, Function.identity(), (a, b) -> a));

        List<IEPGoal> scopedGoals = in.goals().stream()
                .filter(goal -> matchesDomain(goal, domainFilter))
                .toList();

        // ── Accumulators ─────────────────────────────────────────────────────
        int[] trialsPassed  = new int[n];
        int[] trialsTotal   = new int[n];
        int[] completed     = new int[n];
        int[] noShow        = new int[n];
        int[] cancelled     = new int[n];
        int[] rescheduled   = new int[n];
        int[] logged        = new int[n];
        int[] scoreCount    = new int[n];
        int[] scoreSum      = new int[n];
        int[] ratingCount   = new int[n];
        int[] ratingSum     = new int[n];
        int[] progressRatingCount = new int[n];
        int[] progressRatingSum   = new int[n];
        int   parentFeedbackCount = 0;

        Map<String, int[]> domainPassed = new LinkedHashMap<>();
        Map<String, int[]> domainTotal  = new LinkedHashMap<>();

        // ── Sessions ─────────────────────────────────────────────────────────
        for (TherapySession s : in.sessions()) {
            Integer i = indexOf.get(g.bucketStart(s.getSessionDate()));
            if (i == null) continue;

            switch (s.getStatus()) {
                case COMPLETED             -> completed[i]++;
                case NO_SHOW               -> noShow[i]++;
                case CANCELLED, CANCELLATION_REQUESTED -> cancelled[i]++;
                case PENDING_RESCHEDULE    -> { /* awaiting action — reported separately */ }
                case SCHEDULED             -> { /* still ahead of us — counted only in scheduled total */ }
            }

            // A move is a fact about the session, whatever state it ended in.
            if (s.getRescheduleCount() > 0) rescheduled[i]++;

            if (s.getStatus() == TherapySessionStatus.COMPLETED && hasTherapistInput(s)) logged[i]++;

            if (s.getPerformanceScore() != null) {
                scoreCount[i]++;
                scoreSum[i] += s.getPerformanceScore();
            }
        }

        // ── IEP trial progress ───────────────────────────────────────────────
        for (IEPGoalProgress p : in.progress()) {
            if (p.getTrialsTotal() == null || p.getTrialsTotal() <= 0) continue;

            IEPGoal goal = goalsById.get(p.getGoalId());
            if (!matchesDomain(goal, domainFilter)) continue;

            Integer i = indexOf.get(g.bucketStart(p.getSessionDate()));
            if (i == null) continue;

            int passed = p.getTrialsPassed() == null ? 0 : Math.min(p.getTrialsPassed(), p.getTrialsTotal());
            trialsPassed[i] += passed;
            trialsTotal[i]  += p.getTrialsTotal();

            if (goal != null && goal.getDomain() != null) {
                String d = goal.getDomain().name();
                domainPassed.computeIfAbsent(d, k -> new int[n])[i] += passed;
                domainTotal.computeIfAbsent(d, k -> new int[n])[i]  += p.getTrialsTotal();
            }
        }

        // ── Parent ratings from review meetings ──────────────────────────────
        // communicationRating is the successor to the old single-axis parentRating —
        // see 063-review-meeting-rating-axes.sql. Historical rows were backfilled, so
        // this reads correctly across the whole series, not just post-migration data.
        for (ReviewMeeting m : in.meetings()) {
            if (m.getMeetingDate().isBefore(from) || m.getMeetingDate().isAfter(to)) continue;
            if (m.getCommunicationRating() == null && m.getProgressRatingPct() == null) continue;

            Integer i = indexOf.get(g.bucketStart(m.getMeetingDate()));
            if (i == null) continue;

            if (m.getCommunicationRating() != null) {
                ratingCount[i]++;
                ratingSum[i] += m.getCommunicationRating();
            }
            if (m.getProgressRatingPct() != null) {
                progressRatingCount[i]++;
                progressRatingSum[i] += m.getProgressRatingPct();
            }
            parentFeedbackCount++;
        }

        // ── Buckets ──────────────────────────────────────────────────────────
        List<Bucket> buckets = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            buckets.add(new Bucket(
                    starts.get(i),
                    g.label(starts.get(i)),
                    pct(trialsPassed[i], trialsTotal[i]),
                    trialsPassed[i],
                    trialsTotal[i],
                    completed[i],
                    noShow[i],
                    cancelled[i],
                    rescheduled[i],
                    logged[i],
                    mean(scoreSum[i], scoreCount[i]),
                    mean(ratingSum[i], ratingCount[i]),
                    mean(progressRatingSum[i], progressRatingCount[i])));
        }

        // ── Per-domain series ────────────────────────────────────────────────
        List<DomainSeries> domains = new ArrayList<>();
        for (String d : domainPassed.keySet()) {
            int[] dp = domainPassed.get(d);
            int[] dt = domainTotal.get(d);

            List<Double> series = new ArrayList<>(n);
            for (int i = 0; i < n; i++) series.add(pct(dp[i], dt[i]));

            Double delta   = deltaAcross(series);
            int    totalDt = java.util.Arrays.stream(dt).sum();
            int    passDp  = java.util.Arrays.stream(dp).sum();

            domains.add(new DomainSeries(
                    d,
                    series,
                    pct(passDp, totalDt),
                    delta,
                    totalDt,
                    isPlateau(delta, (int) series.stream().filter(java.util.Objects::nonNull).count())));
        }
        domains.sort(Comparator.comparing(DomainSeries::domain));

        // ── Totals ───────────────────────────────────────────────────────────
        int sumPassed    = java.util.Arrays.stream(trialsPassed).sum();
        int sumTotal     = java.util.Arrays.stream(trialsTotal).sum();
        int sumCompleted = java.util.Arrays.stream(completed).sum();
        int sumNoShow    = java.util.Arrays.stream(noShow).sum();
        int sumCancelled = java.util.Arrays.stream(cancelled).sum();
        int sumLogged    = java.util.Arrays.stream(logged).sum();

        int goalsTotal     = scopedGoals.size();
        int goalsCompleted = (int) scopedGoals.stream()
                .filter(goal -> goal.getStatus() == IEPGoalStatus.COMPLETED)
                .count();

        Totals totals = new Totals(
                pct(sumPassed, sumTotal),
                deltaAcross(buckets.stream().map(Bucket::masteryPct).toList()),
                sumPassed,
                sumTotal,
                in.sessions().size(),
                sumCompleted,
                sumNoShow,
                sumCancelled,
                sumLogged,
                pct(sumLogged, sumCompleted),
                goalsTotal,
                goalsCompleted,
                mean(java.util.Arrays.stream(scoreSum).sum(), java.util.Arrays.stream(scoreCount).sum()),
                mean(java.util.Arrays.stream(ratingSum).sum(), java.util.Arrays.stream(ratingCount).sum()),
                mean(java.util.Arrays.stream(progressRatingSum).sum(), java.util.Arrays.stream(progressRatingCount).sum()),
                parentFeedbackCount);

        // Raw per-session scores, in the order they were delivered.
        List<TimeSeriesResponse.SessionPoint> sessionPoints = in.sessions().stream()
                .filter(x -> x.getPerformanceScore() != null)
                .sorted(Comparator.comparing(TherapySession::getSessionDate)
                        .thenComparing(TherapySession::getSessionNumber))
                .map(x -> new TimeSeriesResponse.SessionPoint(
                        x.getId(), x.getSessionDate(), x.getSessionNumber(),
                        x.getPerformanceScore(), x.isAdHoc()))
                .toList();

        int sessionsMoved   = (int) in.sessions().stream().filter(x -> x.getRescheduleCount() > 0).count();
        int totalMoves      = in.sessions().stream().mapToInt(TherapySession::getRescheduleCount).sum();
        int parentRequested = (int) in.sessions().stream().filter(TherapySession::isParentRescheduleRequested).count();
        int awaitingAction  = (int) in.sessions().stream()
                .filter(x -> x.getStatus() == TherapySessionStatus.PENDING_RESCHEDULE).count();
        // Moves the clinic made off its own bat — the family never asked.
        int clinicInitiated = (int) in.sessions().stream()
                .filter(x -> x.getRescheduleCount() > 0 && !x.isParentRescheduleRequested()).count();

        TimeSeriesResponse.RescheduleStats reschedules = new TimeSeriesResponse.RescheduleStats(
                sessionsMoved, totalMoves, parentRequested, clinicInitiated, awaitingAction);

        return new TimeSeriesResponse(type, subjectId, subjectName, g, from, to,
                buckets, domains, sessionPoints, reschedules, totals);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void validateWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Window too wide — request at most " + MAX_WINDOW_DAYS + " days");
        }
    }

    private List<IEPGoal> goalsForPlans(List<IEPPlan> plans) {
        if (plans.isEmpty()) return List.of();
        return goalRepository.findByPlanIdIn(plans.stream().map(IEPPlan::getId).toList());
    }

    private List<IEPGoalProgress> progressForGoals(List<IEPGoal> goals, LocalDate from, LocalDate to) {
        if (goals.isEmpty()) return List.of();
        return progressRepository.findByGoalIdInAndSessionDateBetween(
                goals.stream().map(IEPGoal::getId).toList(), from, to);
    }

    private static boolean matchesDomain(IEPGoal goal, String domainFilter) {
        if (domainFilter == null || domainFilter.isBlank()) return true;
        return goal != null && goal.getDomain() != null
                && goal.getDomain().name().equalsIgnoreCase(domainFilter.trim());
    }

    /** A completed session counts as "logged" once the therapist has written anything into it. */
    private static boolean hasTherapistInput(TherapySession s) {
        return notBlank(s.getNotes())
                || notBlank(s.getProgressReport())
                || notBlank(s.getFeedback())
                || s.getPerformanceScore() != null;
    }

    private static boolean notBlank(String v) { return v != null && !v.isBlank(); }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    /**
     * Ratio of sums as a percentage, to one decimal. Null when the denominator is zero —
     * "nothing was logged" must never serialise as 0%, which would read as a total regression.
     */
    private static Double pct(int numerator, int denominator) {
        if (denominator <= 0) return null;
        return Math.round((double) numerator / denominator * 1000.0) / 10.0;
    }

    private static Double mean(int sum, int count) {
        if (count <= 0) return null;
        return Math.round((double) sum / count * 10.0) / 10.0;
    }

    /** Movement from the first populated period to the last. Null unless both ends have data. */
    private static Double deltaAcross(List<Double> series) {
        Double first = null, last = null;
        for (Double v : series) {
            if (v == null) continue;
            if (first == null) first = v;
            last = v;
        }
        if (first == null || last == null || first.equals(last)) return first == null ? null : 0.0;
        return Math.round((last - first) * 10.0) / 10.0;
    }

    private static int populatedBuckets(List<Bucket> buckets) {
        return (int) buckets.stream().filter(b -> b.masteryPct() != null).count();
    }

    /** Flat despite having enough periods to judge — the signal a review meeting should open with. */
    private static boolean isPlateau(Double deltaPts, int populated) {
        return deltaPts != null
                && populated >= DomainSeries.PLATEAU_MIN_BUCKETS
                && Math.abs(deltaPts) < DomainSeries.PLATEAU_THRESHOLD_PTS;
    }

    /** Present so callers can resolve an optional therapist without a second repository. */
    public Optional<User> findStaff(UUID orgId, UUID userId) {
        return userRepository.findById(userId).filter(u -> orgId.equals(u.getOrgId()));
    }
}
