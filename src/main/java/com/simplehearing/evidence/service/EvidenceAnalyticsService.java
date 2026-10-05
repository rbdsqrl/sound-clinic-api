package com.simplehearing.evidence.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse.ReasonCount;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse.Row;
import com.simplehearing.evidence.dto.EvidenceBreakdownResponses.ByChild;
import com.simplehearing.evidence.dto.EvidenceBreakdownResponses.ChildRow;
import com.simplehearing.evidence.dto.EvidenceBreakdownResponses.MonthCell;
import com.simplehearing.evidence.dto.EvidenceBreakdownResponses.Monthly;
import com.simplehearing.evidence.dto.EvidenceBreakdownResponses.MonthlyRow;
import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.enums.EvidenceReason;
import com.simplehearing.evidence.repository.GoalEvidenceRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Goal completion and video-evidence compliance — by therapist, by month, and by child. */
@Service
public class EvidenceAnalyticsService {

    private static final int MAX_WINDOW_DAYS = 731;

    private final GoalEvidenceRepository evidenceRepository;
    private final IEPGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final EvidenceService evidenceService;
    private final OrganisationRepository organisationRepository;
    private final PatientRepository patientRepository;
    private final TherapySessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;

    public EvidenceAnalyticsService(GoalEvidenceRepository evidenceRepository, IEPGoalRepository goalRepository,
                                    UserRepository userRepository, EvidenceService evidenceService,
                                    OrganisationRepository organisationRepository, PatientRepository patientRepository,
                                    TherapySessionRepository sessionRepository, EnrollmentRepository enrollmentRepository,
                                    SubscriptionRepository subscriptionRepository, ProgramRepository programRepository) {
        this.evidenceRepository = evidenceRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.evidenceService = evidenceService;
        this.organisationRepository = organisationRepository;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
    }

    // ── Shared: completed goals in a window, and how each was evidenced ───────

    private enum Outcome { WITH_VIDEO, CANNOT_UPLOAD, NONE }

    private record Completed(UUID goalId, UUID therapistId, Instant completedAt, UUID patientId, UUID enrollmentId, Outcome outcome) {}

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide a valid date window of at most " + MAX_WINDOW_DAYS + " days");
        }
    }

    private List<Completed> loadCompleted(UUID orgId, Instant start, Instant end) {
        int videosNeeded = Math.max(1, evidenceService.settings(orgId).videosRequired());

        List<Object[]> rows = goalRepository.findCompletedBetween(orgId, start, end);
        Set<UUID> goalIds = rows.stream().map(r -> (UUID) r[0]).collect(Collectors.toSet());

        Map<UUID, int[]> counts = new HashMap<>();   // goalId -> {videos, cannotUpload}
        if (!goalIds.isEmpty()) {
            for (Object[] r : evidenceRepository.countByGoalAndKind(goalIds)) {
                int[] c = counts.computeIfAbsent((UUID) r[0], k -> new int[2]);
                c[r[1] == EvidenceKind.VIDEO ? 0 : 1] = ((Long) r[2]).intValue();
            }
        }

        List<Completed> out = new ArrayList<>();
        for (Object[] r : rows) {
            UUID therapist = r[1] != null ? (UUID) r[1] : (UUID) r[2];   // plan's therapist, else the goal's
            int[] c = counts.getOrDefault((UUID) r[0], new int[2]);
            // A goal is "with video" once it has the required number; otherwise a recorded reason counts.
            Outcome outcome = c[0] >= videosNeeded ? Outcome.WITH_VIDEO : c[1] > 0 ? Outcome.CANNOT_UPLOAD : Outcome.NONE;
            out.add(new Completed((UUID) r[0], therapist, (Instant) r[3], (UUID) r[4], (UUID) r[5], outcome));
        }
        return out;
    }

    private static int[] bucketCounts(Collection<Completed> goals) {
        int[] t = new int[4];  // {completed, withVideo, cannotUpload, none}
        for (Completed g : goals) {
            t[0]++;
            switch (g.outcome()) { case WITH_VIDEO -> t[1]++; case CANNOT_UPLOAD -> t[2]++; default -> t[3]++; }
        }
        return t;
    }

    private Map<UUID, User> namesFor(Collection<UUID> ids) {
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private static String name(User u) {
        return u != null ? (u.getFirstName() + " " + u.getLastName()).trim() : "Unknown";
    }

    // ── By therapist, for the whole window ───────────────────────────────────

    public EvidenceAnalyticsResponse report(UUID orgId, LocalDate from, LocalDate to) {
        validate(from, to);
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        int required = evidenceService.settings(orgId).videosRequired();

        Map<UUID, List<Completed>> byTherapist = loadCompleted(orgId, start, end).stream()
                .filter(g -> g.therapistId() != null)
                .collect(Collectors.groupingBy(Completed::therapistId));

        Map<UUID, Integer> videos = new HashMap<>();
        for (Object[] r : evidenceRepository.countVideosByTherapist(orgId, start, end)) {
            videos.put((UUID) r[0], ((Long) r[1]).intValue());
        }
        Map<UUID, Map<EvidenceReason, Integer>> reasons = new HashMap<>();
        for (Object[] r : evidenceRepository.countCannotUploadByTherapistAndReason(orgId, start, end)) {
            reasons.computeIfAbsent((UUID) r[0], k -> new EnumMap<>(EvidenceReason.class))
                    .put((EvidenceReason) r[1], ((Long) r[2]).intValue());
        }

        // Every therapist gets a row (zeros are information), plus anyone else who has activity.
        Map<UUID, User> people = userRepository.findByOrgIdAndRoleIn(orgId, List.of(Role.THERAPIST)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Set<UUID> ids = new LinkedHashSet<>(people.keySet());
        ids.addAll(byTherapist.keySet()); ids.addAll(videos.keySet()); ids.addAll(reasons.keySet());
        namesFor(ids).forEach(people::putIfAbsent);

        List<Row> rows = ids.stream().map(id -> {
            int[] t = bucketCounts(byTherapist.getOrDefault(id, List.of()));
            Map<EvidenceReason, Integer> rs = reasons.getOrDefault(id, Map.of());
            List<ReasonCount> reasonCounts = rs.entrySet().stream()
                    .sorted(Map.Entry.<EvidenceReason, Integer>comparingByValue().reversed())
                    .map(e -> new ReasonCount(e.getKey(), e.getValue())).toList();
            Double pct = t[0] == 0 ? null : Math.round((double) t[1] / t[0] * 1000.0) / 10.0;
            return new Row(id, name(people.get(id)), t[0], t[1], t[2], t[3], pct, videos.getOrDefault(id, 0),
                    rs.values().stream().mapToInt(Integer::intValue).sum(), reasonCounts);
        }).sorted(Comparator.comparing(Row::therapistName, String.CASE_INSENSITIVE_ORDER)).toList();

        return new EvidenceAnalyticsResponse(required, rows);
    }

    // ── By therapist, month by month ─────────────────────────────────────────

    /** Months are calendar months in the organisation's own timezone, so a goal completed at 11pm on
     *  the 31st counts for that month locally, not for the next one in UTC. */
    public Monthly monthly(UUID orgId, LocalDate from, LocalDate to) {
        validate(from, to);
        ZoneId zone = orgZone(orgId);
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        int required = evidenceService.settings(orgId).videosRequired();

        List<String> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) months.add(m.toString());

        Map<UUID, Map<String, List<Completed>>> grouped = new HashMap<>();
        for (Completed g : loadCompleted(orgId, start, end)) {
            if (g.therapistId() == null || g.completedAt() == null) continue;
            String month = YearMonth.from(g.completedAt().atZone(zone)).toString();
            grouped.computeIfAbsent(g.therapistId(), k -> new HashMap<>()).computeIfAbsent(month, k -> new ArrayList<>()).add(g);
        }

        Map<UUID, User> people = userRepository.findByOrgIdAndRoleIn(orgId, List.of(Role.THERAPIST)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Set<UUID> ids = new LinkedHashSet<>(people.keySet());
        ids.addAll(grouped.keySet());
        namesFor(ids).forEach(people::putIfAbsent);

        List<MonthlyRow> rows = ids.stream().map(id -> {
            Map<String, List<Completed>> byMonth = grouped.getOrDefault(id, Map.of());
            List<MonthCell> cells = months.stream().map(m -> {
                int[] t = bucketCounts(byMonth.getOrDefault(m, List.of()));
                return new MonthCell(m, t[0], t[1], t[2], t[3]);
            }).toList();
            return new MonthlyRow(id, name(people.get(id)), cells);
        }).sorted(Comparator.comparing(MonthlyRow::therapistName, String.CASE_INSENSITIVE_ORDER)).toList();

        return new Monthly(required, months, rows);
    }

    private ZoneId orgZone(UUID orgId) {
        try {
            return organisationRepository.findById(orgId).map(o -> ZoneId.of(o.getTimezone())).orElse(ZoneOffset.UTC);
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    // ── By child ─────────────────────────────────────────────────────────────

    public ByChild byChild(UUID orgId, LocalDate from, LocalDate to) {
        validate(from, to);
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        int required = evidenceService.settings(orgId).videosRequired();

        Map<UUID, List<Completed>> completed = loadCompleted(orgId, start, end).stream()
                .collect(Collectors.groupingBy(Completed::patientId));

        Map<UUID, Integer> activeGoals = new HashMap<>();
        for (Object[] r : goalRepository.countActiveByPatient(orgId)) activeGoals.put((UUID) r[0], ((Long) r[1]).intValue());
        Map<UUID, Integer> videos = new HashMap<>();
        for (Object[] r : evidenceRepository.countVideosByPatient(orgId, start, end)) videos.put((UUID) r[0], ((Long) r[1]).intValue());
        Map<UUID, Integer> sessionsCompleted = new HashMap<>();
        for (Object[] r : sessionRepository.countByPatientAndStatus(orgId, from, to)) {
            if (r[1] == TherapySessionStatus.COMPLETED) sessionsCompleted.put((UUID) r[0], ((Long) r[2]).intValue());
        }

        Set<UUID> childIds = new LinkedHashSet<>(completed.keySet());
        childIds.addAll(videos.keySet());
        childIds.addAll(activeGoals.keySet());
        Map<UUID, Patient> patients = patientRepository.findAllById(childIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));

        Map<UUID, String> programByEnrollment = programNames(completed.values().stream()
                .flatMap(List::stream).map(Completed::enrollmentId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<ChildRow> rows = childIds.stream()
                .filter(patients::containsKey)
                .map(id -> {
                    List<Completed> goals = completed.getOrDefault(id, List.of());
                    int[] t = bucketCounts(goals);
                    List<String> therapies = goals.stream().map(Completed::enrollmentId).filter(Objects::nonNull)
                            .map(programByEnrollment::get).filter(Objects::nonNull).distinct().sorted().toList();
                    Patient p = patients.get(id);
                    return new ChildRow(id, (p.getFirstName() + " " + p.getLastName()).trim(), therapies,
                            activeGoals.getOrDefault(id, 0), t[0], t[1], t[2], t[3],
                            videos.getOrDefault(id, 0), sessionsCompleted.getOrDefault(id, 0));
                })
                .sorted(Comparator.comparing(ChildRow::patientName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new ByChild(required, rows);
    }

    /** enrollmentId -> program name, resolved through subscription -> program in bulk. */
    private Map<UUID, String> programNames(Set<UUID> enrollmentIds) {
        if (enrollmentIds.isEmpty()) return Map.of();
        Map<UUID, Enrollment> enrollments = enrollmentRepository.findAllById(enrollmentIds).stream()
                .collect(Collectors.toMap(Enrollment::getId, e -> e));
        Map<UUID, UUID> programIdBySubscription = subscriptionRepository.findAllById(
                        enrollments.values().stream().map(Enrollment::getSubscriptionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subscription::getId, Subscription::getProgramId));
        Map<UUID, String> names = programRepository.findAllById(new HashSet<>(programIdBySubscription.values())).stream()
                .collect(Collectors.toMap(Program::getId, Program::getName));
        Map<UUID, String> out = new HashMap<>();
        enrollments.forEach((id, e) -> {
            UUID programId = programIdBySubscription.get(e.getSubscriptionId());
            if (programId != null && names.containsKey(programId)) out.put(id, names.get(programId));
        });
        return out;
    }
}
