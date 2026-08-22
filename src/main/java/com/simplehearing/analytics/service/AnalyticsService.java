package com.simplehearing.analytics.service;

import com.simplehearing.analytics.dto.CaseloadResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse;
import com.simplehearing.analytics.dto.TimeSeriesResponse.Bucket;
import com.simplehearing.analytics.dto.TimeSeriesResponse.DomainSeries;
import com.simplehearing.analytics.dto.TimeSeriesResponse.SubjectType;
import com.simplehearing.analytics.dto.TimeSeriesResponse.Totals;
import com.simplehearing.analytics.enums.Granularity;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
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
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public AnalyticsService(TherapySessionRepository sessionRepository,
                            IEPGoalProgressRepository progressRepository,
                            IEPGoalRepository goalRepository,
                            IEPPlanRepository planRepository,
                            ReviewMeetingRepository reviewMeetingRepository,
                            PatientRepository patientRepository,
                            UserRepository userRepository,
                            OrganisationRepository organisationRepository) {
        this.sessionRepository       = sessionRepository;
        this.progressRepository      = progressRepository;
        this.goalRepository          = goalRepository;
        this.planRepository          = planRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.patientRepository       = patientRepository;
        this.userRepository          = userRepository;
        this.organisationRepository  = organisationRepository;
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
        for (ReviewMeeting m : in.meetings()) {
            if (m.getParentRating() == null) continue;
            if (m.getMeetingDate().isBefore(from) || m.getMeetingDate().isAfter(to)) continue;

            Integer i = indexOf.get(g.bucketStart(m.getMeetingDate()));
            if (i == null) continue;

            ratingCount[i]++;
            ratingSum[i] += m.getParentRating();
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
                    mean(ratingSum[i], ratingCount[i])));
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
                mean(java.util.Arrays.stream(ratingSum).sum(), java.util.Arrays.stream(ratingCount).sum()));

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
