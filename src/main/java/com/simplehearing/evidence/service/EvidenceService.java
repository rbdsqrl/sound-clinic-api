package com.simplehearing.evidence.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.evidence.dto.EvidenceResponse;
import com.simplehearing.evidence.dto.EvidenceSettings;
import com.simplehearing.evidence.entity.GoalEvidence;
import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.repository.GoalEvidenceRepository;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceService {

    private final GoalEvidenceRepository evidenceRepository;
    private final OrganisationRepository organisationRepository;
    private final IEPGoalRepository goalRepository;
    private final IEPPlanRepository planRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public EvidenceService(GoalEvidenceRepository evidenceRepository,
                           OrganisationRepository organisationRepository,
                           IEPGoalRepository goalRepository,
                           IEPPlanRepository planRepository,
                           UserRepository userRepository,
                           StorageService storageService) {
        this.evidenceRepository = evidenceRepository;
        this.organisationRepository = organisationRepository;
        this.goalRepository = goalRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    // ── Org settings ─────────────────────────────────────────────────────────

    public EvidenceSettings settings(UUID orgId) {
        Organisation org = org(orgId);
        return new EvidenceSettings(org.getEvidenceVideosRequired(), org.getEvidenceMaxVideoMb(), org.getEvidenceMaxVideoSeconds());
    }

    @Transactional
    public EvidenceSettings updateSettings(UUID orgId, EvidenceSettings s) {
        Organisation org = org(orgId);
        org.setEvidenceVideosRequired(s.videosRequired());
        org.setEvidenceMaxVideoMb(s.maxVideoMb());
        org.setEvidenceMaxVideoSeconds(s.maxVideoSeconds());
        organisationRepository.save(org);
        return settings(orgId);
    }

    private Organisation org(UUID orgId) {
        return organisationRepository.findById(orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organisation not found"));
    }

    // ── Completion rule ──────────────────────────────────────────────────────

    /**
     * A goal can be completed once it has the org's required number of videos, or the therapist has
     * recorded why they can't upload. Does nothing when the org doesn't require evidence.
     */
    public void assertGoalSatisfied(IEPGoal goal, UUID orgId) {
        int required = org(orgId).getEvidenceVideosRequired();
        if (required <= 0) return;

        long videos = evidenceRepository.countByGoalIdAndKind(goal.getId(), EvidenceKind.VIDEO);
        if (videos >= required) return;
        if (evidenceRepository.existsByGoalIdAndKind(goal.getId(), EvidenceKind.CANNOT_UPLOAD)) return;

        throw new ApiException(HttpStatus.CONFLICT,
                "Add " + (required - videos) + " more video" + (required - videos == 1 ? "" : "s")
                        + " for this goal, or record why you can't upload, before completing it.");
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    public List<EvidenceResponse> toResponses(List<GoalEvidence> items) {
        if (items.isEmpty()) return List.of();

        Map<UUID, IEPGoal> goals = goalRepository.findAllById(
                        items.stream().map(GoalEvidence::getGoalId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(IEPGoal::getId, g -> g));
        Set<UUID> planIds = new HashSet<>();
        goals.values().forEach(g -> planIds.add(g.getPlanId()));
        Map<UUID, IEPPlan> plans = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(IEPPlan::getId, p -> p));
        Map<UUID, User> users = userRepository.findAllById(
                        items.stream().map(GoalEvidence::getTherapistId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return items.stream().map(e -> {
            IEPGoal goal = e.getGoalId() != null ? goals.get(e.getGoalId()) : null;
            IEPPlan plan = goal != null ? plans.get(goal.getPlanId()) : null;
            User u = users.get(e.getTherapistId());
            String name = u != null ? (u.getFirstName() + " " + u.getLastName()).trim() : "Unknown";
            String url = e.getFileUrl() != null ? storageService.presign(e.getFileUrl(), Duration.ofHours(1)) : null;
            return EvidenceResponse.from(e, plan != null ? plan.getTitle() : null,
                    goal != null ? goal.getTitle() : null, name, url);
        }).toList();
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    /** Removes a goal's evidence rows and their stored video files (the rows alone would cascade, leaving orphaned files). */
    @Transactional
    public void deleteForGoal(UUID goalId) {
        List<GoalEvidence> items = evidenceRepository.findByGoalIdIn(List.of(goalId));
        items.stream().map(GoalEvidence::getFileUrl).filter(java.util.Objects::nonNull).forEach(storageService::delete);
        evidenceRepository.deleteAll(items);
    }

    public GoalEvidence requireOwned(UUID id, UUID orgId, UUID patientId) {
        return evidenceRepository.findByIdAndOrgIdAndPatientId(id, orgId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found"));
    }
}
