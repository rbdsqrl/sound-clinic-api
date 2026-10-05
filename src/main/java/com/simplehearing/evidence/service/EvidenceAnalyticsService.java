package com.simplehearing.evidence.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse.ReasonCount;
import com.simplehearing.evidence.dto.EvidenceAnalyticsResponse.Row;
import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.enums.EvidenceReason;
import com.simplehearing.evidence.repository.GoalEvidenceRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/** Per-therapist goal completion and evidence compliance — the "Evidence" report on the Members analytics tab. */
@Service
public class EvidenceAnalyticsService {

    private static final int MAX_WINDOW_DAYS = 731;

    private final GoalEvidenceRepository evidenceRepository;
    private final IEPGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final EvidenceService evidenceService;

    public EvidenceAnalyticsService(GoalEvidenceRepository evidenceRepository, IEPGoalRepository goalRepository,
                                    UserRepository userRepository, EvidenceService evidenceService) {
        this.evidenceRepository = evidenceRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.evidenceService = evidenceService;
    }

    public EvidenceAnalyticsResponse report(UUID orgId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || java.time.temporal.ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide a valid date window of at most " + MAX_WINDOW_DAYS + " days");
        }
        var start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        var end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        int required = evidenceService.settings(orgId).videosRequired();
        int videosNeeded = Math.max(1, required);

        // Completed goals in the window → who they count for (the plan's therapist, else the goal's).
        Map<UUID, UUID> therapistByGoal = new HashMap<>();
        for (Object[] r : goalRepository.findCompletedBetween(orgId, start, end)) {
            UUID therapist = r[1] != null ? (UUID) r[1] : (UUID) r[2];
            if (therapist != null) therapistByGoal.put((UUID) r[0], therapist);
        }

        Map<UUID, int[]> counts = new HashMap<>();   // goalId -> {videos, cannotUpload}
        if (!therapistByGoal.isEmpty()) {
            for (Object[] r : evidenceRepository.countByGoalAndKind(therapistByGoal.keySet())) {
                int[] c = counts.computeIfAbsent((UUID) r[0], k -> new int[2]);
                c[r[1] == EvidenceKind.VIDEO ? 0 : 1] = ((Long) r[2]).intValue();
            }
        }

        Map<UUID, int[]> perTherapist = new HashMap<>();  // {completed, withVideo, cannotUpload, none}
        therapistByGoal.forEach((goalId, therapistId) -> {
            int[] t = perTherapist.computeIfAbsent(therapistId, k -> new int[4]);
            int[] c = counts.getOrDefault(goalId, new int[2]);
            t[0]++;
            if (c[0] >= videosNeeded) t[1]++;
            else if (c[1] > 0) t[2]++;
            else t[3]++;
        });

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
        ids.addAll(perTherapist.keySet()); ids.addAll(videos.keySet()); ids.addAll(reasons.keySet());
        userRepository.findAllById(ids).forEach(u -> people.putIfAbsent(u.getId(), u));

        List<Row> rows = ids.stream().map(id -> {
            User u = people.get(id);
            int[] t = perTherapist.getOrDefault(id, new int[4]);
            Map<EvidenceReason, Integer> rs = reasons.getOrDefault(id, Map.of());
            List<ReasonCount> reasonCounts = rs.entrySet().stream()
                    .sorted(Map.Entry.<EvidenceReason, Integer>comparingByValue().reversed())
                    .map(e -> new ReasonCount(e.getKey(), e.getValue())).toList();
            Double pct = t[0] == 0 ? null : Math.round((double) t[1] / t[0] * 1000.0) / 10.0;
            return new Row(id, u != null ? (u.getFirstName() + " " + u.getLastName()).trim() : "Unknown",
                    t[0], t[1], t[2], t[3], pct, videos.getOrDefault(id, 0),
                    rs.values().stream().mapToInt(Integer::intValue).sum(), reasonCounts);
        }).sorted(Comparator.comparing(Row::therapistName, String.CASE_INSENSITIVE_ORDER)).toList();

        return new EvidenceAnalyticsResponse(required, rows);
    }
}
