package com.simplehearing.iep.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.iep.dto.*;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPGoalProgress;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;
import com.simplehearing.iep.enums.IEPPlanStatus;
import com.simplehearing.iep.repository.IEPGoalProgressRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IEPService {

    private final IEPPlanRepository planRepository;
    private final IEPGoalRepository goalRepository;
    private final IEPGoalProgressRepository progressRepository;
    private final UserRepository userRepository;

    public IEPService(IEPPlanRepository planRepository,
                      IEPGoalRepository goalRepository,
                      IEPGoalProgressRepository progressRepository,
                      UserRepository userRepository) {
        this.planRepository = planRepository;
        this.goalRepository = goalRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    // ── List plans for a patient ──────────────────────────────────────────────

    public List<IEPPlanResponse> listPlans(UUID patientId, UserPrincipal principal) {
        List<IEPPlan> plans = planRepository.findByOrgIdAndPatientIdOrderByCreatedAtDesc(
                principal.getOrgId(), patientId);

        Set<UUID> therapistIds = plans.stream()
                .map(IEPPlan::getTherapistId)
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = therapistIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(therapistIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return plans.stream()
                .map(plan -> buildPlanResponse(plan, userMap))
                .collect(Collectors.toList());
    }

    // ── Create plan (with optional inline goals) ──────────────────────────────

    @Transactional
    public IEPPlanResponse createPlan(UUID patientId, CreateIEPPlanRequest req, UserPrincipal principal) {
        IEPPlan plan = new IEPPlan();
        plan.setOrgId(principal.getOrgId());
        plan.setPatientId(patientId);
        plan.setTherapistId(principal.getId());
        plan.setTitle(req.title());
        plan.setStartDate(req.startDate());
        plan.setEndDate(req.endDate());
        plan.setStatus(IEPPlanStatus.ACTIVE);
        plan.setTags(joinTags(req.tags()));

        IEPPlan saved = planRepository.save(plan);

        if (req.goals() != null) {
            for (CreateIEPGoalRequest gr : req.goals()) {
                IEPGoal goal = buildGoalFromRequest(gr, saved.getId(), principal.getOrgId());
                goalRepository.save(goal);
            }
        }

        String therapistName = fullName(principal.getUser());
        List<IEPGoal> goals = goalRepository.findByPlanIdOrderByCreatedAtAsc(saved.getId());
        List<IEPGoalResponse> goalResponses = goals.stream()
                .map(g -> IEPGoalResponse.from(g, null, 0))
                .collect(Collectors.toList());
        int completedGoals = (int) goals.stream()
                .filter(g -> g.getStatus() == IEPGoalStatus.COMPLETED)
                .count();

        return IEPPlanResponse.from(saved, therapistName, goalResponses, completedGoals);
    }

    // ── Update plan ───────────────────────────────────────────────────────────

    @Transactional
    public IEPPlanResponse updatePlan(UUID planId, UpdateIEPPlanRequest req, UserPrincipal principal) {
        IEPPlan plan = planRepository.findByIdAndOrgId(planId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP plan not found"));

        if (req.title() != null) plan.setTitle(req.title());
        if (req.startDate() != null) plan.setStartDate(req.startDate());
        if (req.endDate() != null) plan.setEndDate(req.endDate());
        if (req.tags() != null) plan.setTags(joinTags(req.tags()));
        if (req.status() != null) plan.setStatus(req.status());

        IEPPlan saved = planRepository.save(plan);

        User therapist = userRepository.findById(saved.getTherapistId()).orElse(null);
        String therapistName = therapist != null ? fullName(therapist) : null;

        List<IEPGoal> goals = goalRepository.findByPlanIdOrderByCreatedAtAsc(saved.getId());
        Map<UUID, Integer> progressCounts = buildProgressCountMap(goals);
        Map<UUID, User> therapistMap = buildTherapistMapForGoals(goals);

        List<IEPGoalResponse> goalResponses = goals.stream()
                .map(g -> IEPGoalResponse.from(g,
                        therapistNameForGoal(g, therapistMap),
                        progressCounts.getOrDefault(g.getId(), 0)))
                .collect(Collectors.toList());
        int completedGoals = (int) goals.stream()
                .filter(g -> g.getStatus() == IEPGoalStatus.COMPLETED)
                .count();

        return IEPPlanResponse.from(saved, therapistName, goalResponses, completedGoals);
    }

    // ── Delete plan ───────────────────────────────────────────────────────────

    @Transactional
    public void deletePlan(UUID planId, UserPrincipal principal) {
        IEPPlan plan = planRepository.findByIdAndOrgId(planId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP plan not found"));

        // Delete progress entries for all goals in this plan
        List<IEPGoal> goals = goalRepository.findByPlanIdOrderByCreatedAtAsc(planId);
        for (IEPGoal goal : goals) {
            List<IEPGoalProgress> progressList = progressRepository.findByGoalIdOrderBySessionDateDesc(goal.getId());
            progressRepository.deleteAll(progressList);
        }
        goalRepository.deleteAll(goals);
        planRepository.delete(plan);
    }

    // ── Add goal to plan ──────────────────────────────────────────────────────

    @Transactional
    public IEPGoalResponse addGoal(UUID planId, CreateIEPGoalRequest req, UserPrincipal principal) {
        IEPPlan plan = planRepository.findByIdAndOrgId(planId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP plan not found"));

        IEPGoal goal = buildGoalFromRequest(req, plan.getId(), principal.getOrgId());
        IEPGoal saved = goalRepository.save(goal);

        return IEPGoalResponse.from(saved, null, 0);
    }

    // ── Update goal ───────────────────────────────────────────────────────────

    @Transactional
    public IEPGoalResponse updateGoal(UUID goalId, UpdateIEPGoalRequest req, UserPrincipal principal) {
        IEPGoal goal = goalRepository.findByIdAndOrgId(goalId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP goal not found"));

        if (req.title() != null) goal.setTitle(req.title());
        if (req.goalStatement() != null) goal.setGoalStatement(req.goalStatement());
        if (req.domain() != null) goal.setDomain(req.domain());
        if (req.baseline() != null) goal.setBaseline(req.baseline());
        if (req.targetCriteria() != null) goal.setTargetCriteria(req.targetCriteria());
        if (req.targetDate() != null) goal.setTargetDate(req.targetDate());
        if (req.status() != null) goal.setStatus(req.status());
        // empty string clears the tag; null means "don't change"
        if (req.progressTag() != null) goal.setProgressTag(req.progressTag().isBlank() ? null : req.progressTag());

        IEPGoal saved = goalRepository.save(goal);

        String therapistName = null;
        if (saved.getAssignedTherapistId() != null) {
            User t = userRepository.findById(saved.getAssignedTherapistId()).orElse(null);
            if (t != null) therapistName = fullName(t);
        }

        int progressCount = progressRepository.countByGoalId(saved.getId());
        return IEPGoalResponse.from(saved, therapistName, progressCount);
    }

    // ── Delete goal ───────────────────────────────────────────────────────────

    @Transactional
    public void deleteGoal(UUID goalId, UserPrincipal principal) {
        IEPGoal goal = goalRepository.findByIdAndOrgId(goalId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP goal not found"));

        List<IEPGoalProgress> progressList = progressRepository.findByGoalIdOrderBySessionDateDesc(goalId);
        progressRepository.deleteAll(progressList);
        goalRepository.delete(goal);
    }

    // ── Add progress entry ────────────────────────────────────────────────────

    @Transactional
    public IEPGoalResponse addProgress(UUID goalId, AddProgressRequest req, UserPrincipal principal) {
        IEPGoal goal = goalRepository.findByIdAndOrgId(goalId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP goal not found"));

        IEPGoalProgress progress = new IEPGoalProgress();
        progress.setOrgId(principal.getOrgId());
        progress.setGoalId(goalId);
        progress.setTherapistId(principal.getId());
        progress.setSessionDate(req.sessionDate());
        progress.setNote(req.note());
        progress.setTrialsPassed(req.trialsPassed());
        progress.setTrialsTotal(req.trialsTotal());

        progressRepository.save(progress);

        String therapistName = null;
        if (goal.getAssignedTherapistId() != null) {
            User t = userRepository.findById(goal.getAssignedTherapistId()).orElse(null);
            if (t != null) therapistName = fullName(t);
        }

        int progressCount = progressRepository.countByGoalId(goalId);
        return IEPGoalResponse.from(goal, therapistName, progressCount);
    }

    // ── Import from CSV ───────────────────────────────────────────────────────

    @Transactional
    public ImportResultResponse importCsv(UUID patientId, String csvContent, UserPrincipal principal) {
        List<String> errors = new ArrayList<>();
        int plansCreated = 0;
        int goalsCreated = 0;

        String[] lines = csvContent.split("\r?\n");
        if (lines.length <= 1) {
            return new ImportResultResponse(0, 0, List.of("CSV is empty or contains only a header row"));
        }

        // planTitle -> IEPPlan (accumulated during import)
        Map<String, IEPPlan> plansByTitle = new LinkedHashMap<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            int rowNum = i + 1;
            List<String> cols;
            try {
                cols = parseCsvRow(line);
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": failed to parse CSV — " + e.getMessage());
                continue;
            }

            if (cols.size() < 10) {
                errors.add("Row " + rowNum + ": expected 10 columns, got " + cols.size());
                continue;
            }

            String planTitle     = cols.get(0).trim();
            String planStartRaw  = cols.get(1).trim();
            String planEndRaw    = cols.get(2).trim();
            String planTagsRaw   = cols.get(3).trim();
            String goalTitle     = cols.get(4).trim();
            String goalStatement = cols.get(5).trim();
            String goalDomainRaw = cols.get(6).trim();
            String goalBaseline  = cols.get(7).trim();
            String goalTarget    = cols.get(8).trim();
            String goalDateRaw   = cols.get(9).trim();

            if (planTitle.isEmpty()) {
                errors.add("Row " + rowNum + ": plan_title is required");
                continue;
            }
            if (goalTitle.isEmpty()) {
                errors.add("Row " + rowNum + ": goal_title is required");
                continue;
            }

            // Parse goal domain
            IEPGoalDomain domain;
            try {
                domain = IEPGoalDomain.valueOf(goalDomainRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Row " + rowNum + ": unknown goal_domain '" + goalDomainRaw + "'. " +
                        "Valid values: " + Arrays.toString(IEPGoalDomain.values()));
                continue;
            }

            // Parse dates
            LocalDate planStart = null;
            LocalDate planEnd   = null;
            LocalDate goalDate  = null;

            if (!planStartRaw.isEmpty()) {
                try {
                    planStart = LocalDate.parse(planStartRaw);
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": invalid plan_start_date '" + planStartRaw + "' — use YYYY-MM-DD");
                    continue;
                }
            }
            if (!planEndRaw.isEmpty()) {
                try {
                    planEnd = LocalDate.parse(planEndRaw);
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": invalid plan_end_date '" + planEndRaw + "' — use YYYY-MM-DD");
                    continue;
                }
            }
            if (!goalDateRaw.isEmpty()) {
                try {
                    goalDate = LocalDate.parse(goalDateRaw);
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": invalid goal_target_date '" + goalDateRaw + "' — use YYYY-MM-DD");
                    continue;
                }
            }

            // Create or reuse plan
            IEPPlan plan = plansByTitle.get(planTitle);
            if (plan == null) {
                plan = new IEPPlan();
                plan.setOrgId(principal.getOrgId());
                plan.setPatientId(patientId);
                plan.setTherapistId(principal.getId());
                plan.setTitle(planTitle);
                plan.setStartDate(planStart);
                plan.setEndDate(planEnd);
                plan.setStatus(IEPPlanStatus.ACTIVE);
                if (!planTagsRaw.isEmpty()) plan.setTags(planTagsRaw);
                plan = planRepository.save(plan);
                plansByTitle.put(planTitle, plan);
                plansCreated++;
            }

            // Create goal
            IEPGoal goal = new IEPGoal();
            goal.setOrgId(principal.getOrgId());
            goal.setPlanId(plan.getId());
            goal.setTitle(goalTitle);
            goal.setGoalStatement(goalStatement.isEmpty() ? null : goalStatement);
            goal.setDomain(domain);
            goal.setBaseline(goalBaseline.isEmpty() ? null : goalBaseline);
            goal.setTargetCriteria(goalTarget.isEmpty() ? null : goalTarget);
            goal.setTargetDate(goalDate);
            goal.setStatus(IEPGoalStatus.IN_PROGRESS);
            goalRepository.save(goal);
            goalsCreated++;
        }

        return new ImportResultResponse(plansCreated, goalsCreated, errors);
    }

    // ── Sample CSV ────────────────────────────────────────────────────────────

    public String sampleCsv() {
        return "plan_title,plan_start_date,plan_end_date,plan_tags,goal_title,goal_statement,goal_domain," +
                "goal_baseline,goal_target_criteria,goal_target_date\n" +
                "Annual IEP 2025,2025-01-01,2025-12-31,\"speech,language\",Improve phoneme discrimination," +
                "\"Student will identify minimal pairs with 80% accuracy\",AUDITORY," +
                "\"Currently at 50% accuracy on /p/ vs /b/\",\"80% accuracy across 3 sessions\",2025-06-30\n" +
                "Annual IEP 2025,2025-01-01,2025-12-31,\"speech,language\",Expand expressive vocabulary," +
                "\"Student will use 50 new action words spontaneously\",LANGUAGE," +
                "\"Uses approximately 20 action words consistently\",\"50 novel action words in conversation\",2025-09-30\n" +
                "Motor Skills Plan,2025-03-01,2025-12-31,motor,Fine motor pincer grasp," +
                "\"Student will pick up small objects using pincer grasp independently\",MOTOR," +
                "\"Requires hand-over-hand assistance for small objects\",\"Independent in 4/5 trials\",2025-12-01\n";
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private IEPPlanResponse buildPlanResponse(IEPPlan plan, Map<UUID, User> userMap) {
        User therapist = userMap.get(plan.getTherapistId());
        String therapistName = therapist != null ? fullName(therapist) : null;

        List<IEPGoal> goals = goalRepository.findByPlanIdOrderByCreatedAtAsc(plan.getId());
        Map<UUID, Integer> progressCounts = buildProgressCountMap(goals);
        Map<UUID, User> goalTherapistMap = buildTherapistMapForGoals(goals);

        List<IEPGoalResponse> goalResponses = goals.stream()
                .map(g -> IEPGoalResponse.from(g,
                        therapistNameForGoal(g, goalTherapistMap),
                        progressCounts.getOrDefault(g.getId(), 0)))
                .collect(Collectors.toList());
        int completedGoals = (int) goals.stream()
                .filter(g -> g.getStatus() == IEPGoalStatus.COMPLETED)
                .count();

        return IEPPlanResponse.from(plan, therapistName, goalResponses, completedGoals);
    }

    private Map<UUID, Integer> buildProgressCountMap(List<IEPGoal> goals) {
        Map<UUID, Integer> map = new HashMap<>();
        for (IEPGoal goal : goals) {
            map.put(goal.getId(), progressRepository.countByGoalId(goal.getId()));
        }
        return map;
    }

    private Map<UUID, User> buildTherapistMapForGoals(List<IEPGoal> goals) {
        Set<UUID> ids = goals.stream()
                .filter(g -> g.getAssignedTherapistId() != null)
                .map(IEPGoal::getAssignedTherapistId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private String therapistNameForGoal(IEPGoal goal, Map<UUID, User> map) {
        if (goal.getAssignedTherapistId() == null) return null;
        User u = map.get(goal.getAssignedTherapistId());
        return u != null ? fullName(u) : null;
    }

    private IEPGoal buildGoalFromRequest(CreateIEPGoalRequest req, UUID planId, UUID orgId) {
        IEPGoal goal = new IEPGoal();
        goal.setOrgId(orgId);
        goal.setPlanId(planId);
        goal.setTitle(req.title());
        goal.setGoalStatement(req.goalStatement());
        goal.setDomain(req.domain());
        goal.setBaseline(req.baseline());
        goal.setTargetCriteria(req.targetCriteria());
        goal.setTargetDate(req.targetDate());
        goal.setStatus(IEPGoalStatus.IN_PROGRESS);
        return goal;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.joining(","));
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    /**
     * Parses a single CSV row, handling double-quoted fields that may contain commas.
     */
    private List<String> parseCsvRow(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Peek ahead for escaped double-quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip second quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        fields.add(current.toString());
        return fields;
    }
}
