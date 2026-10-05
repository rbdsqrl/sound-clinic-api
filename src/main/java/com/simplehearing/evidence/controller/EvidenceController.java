package com.simplehearing.evidence.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.evidence.dto.CannotUploadRequest;
import com.simplehearing.evidence.dto.EvidenceResponse;
import com.simplehearing.evidence.dto.EvidenceSettings;
import com.simplehearing.evidence.entity.GoalEvidence;
import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.enums.EvidenceReason;
import com.simplehearing.evidence.repository.GoalEvidenceRepository;
import com.simplehearing.evidence.service.EvidenceService;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Goal Evidence", description = "Video evidence for IEP goals, and the reasons a video couldn't be uploaded")
@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private static final long MB = 1024L * 1024;

    private final GoalEvidenceRepository evidenceRepository;
    private final EvidenceService evidenceService;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final TherapistPatientRepository therapistPatientRepository;
    private final IEPGoalRepository goalRepository;
    private final IEPPlanRepository planRepository;
    private final TherapySessionRepository sessionRepository;
    private final StorageService storageService;

    public EvidenceController(GoalEvidenceRepository evidenceRepository,
                              EvidenceService evidenceService,
                              PatientRepository patientRepository,
                              PatientParentRepository patientParentRepository,
                              TherapistPatientRepository therapistPatientRepository,
                              IEPGoalRepository goalRepository,
                              IEPPlanRepository planRepository,
                              TherapySessionRepository sessionRepository,
                              StorageService storageService) {
        this.evidenceRepository = evidenceRepository;
        this.evidenceService = evidenceService;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.goalRepository = goalRepository;
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.storageService = storageService;
    }

    // ── Org rules ────────────────────────────────────────────────────────────

    @Operation(summary = "The organisation's goal video evidence rules — readable by all staff, so the app can enforce them")
    @GetMapping("/organisation/evidence-settings")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<EvidenceSettings>> getSettings(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.settings(principal.getOrgId())));
    }

    @Operation(summary = "Update the goal video evidence rules (videos required, max size, max length)")
    @PutMapping("/organisation/evidence-settings")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<EvidenceSettings>> updateSettings(
            @Valid @RequestBody EvidenceSettings body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.updateSettings(principal.getOrgId(), body)));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @Operation(summary = "A child's evidence, newest first; optionally for one goal. Parents see videos only.")
    @GetMapping("/patients/{patientId}/evidence")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<List<EvidenceResponse>>> list(
            @PathVariable UUID patientId,
            @RequestParam(required = false) UUID goalId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireAccessible(patientId, principal);

        List<GoalEvidence> items = goalId != null
                ? evidenceRepository.findByOrgIdAndPatientIdAndGoalIdOrderByCreatedAtDesc(principal.getOrgId(), patientId, goalId)
                : evidenceRepository.findByOrgIdAndPatientIdOrderByCreatedAtDesc(principal.getOrgId(), patientId);
        // "Couldn't upload" records are internal — a family sees the videos only.
        if (principal.getActiveRole() == Role.PARENT) {
            items = items.stream().filter(e -> e.getKind() == EvidenceKind.VIDEO).toList();
        }
        return ResponseEntity.ok(ApiResponse.success(evidenceService.toResponses(items)));
    }

    // ── Upload a video ───────────────────────────────────────────────────────

    @Operation(summary = "Upload a video as evidence — for a goal, a session, both, or neither (ad hoc)")
    @PostMapping("/patients/{patientId}/evidence")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> upload(
            @PathVariable UUID patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "goalId", required = false) UUID goalId,
            @RequestParam(value = "sessionId", required = false) UUID sessionId,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        Patient patient = requireAccessible(patientId, principal);
        EvidenceSettings rules = evidenceService.settings(principal.getOrgId());

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a video to upload");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("video/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Evidence must be a video file");
        }
        if (file.getSize() > rules.maxVideoMb() * MB) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Video is too large — the limit is " + rules.maxVideoMb() + " MB");
        }
        if (durationSeconds != null && durationSeconds > rules.maxVideoSeconds()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Video is too long — the limit is " + rules.maxVideoSeconds() + " seconds");
        }

        GoalEvidence e = newEvidence(patient, goalId, sessionId, principal);
        e.setKind(EvidenceKind.VIDEO);
        e.setFileUrl(storageService.store(file, "goal-evidence/" + patientId));
        e.setFileName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "video");
        e.setContentType(file.getContentType());
        e.setFileSizeBytes(file.getSize());
        e.setDurationSeconds(durationSeconds);
        e.setNote(StringUtils.hasText(note) ? note.trim() : null);

        GoalEvidence saved = evidenceRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(evidenceService.toResponses(List.of(saved)).get(0)));
    }

    // ── Record "couldn't upload" ─────────────────────────────────────────────

    @Operation(summary = "Record why a video couldn't be uploaded — allows completing the goal without one")
    @PostMapping("/patients/{patientId}/evidence/cannot-upload")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<ApiResponse<EvidenceResponse>> cannotUpload(
            @PathVariable UUID patientId,
            @Valid @RequestBody CannotUploadRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {

        Patient patient = requireAccessible(patientId, principal);
        if (body.reasonCode() == EvidenceReason.OTHER && !StringUtils.hasText(body.reasonText())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please describe the reason");
        }

        GoalEvidence e = newEvidence(patient, body.goalId(), body.sessionId(), principal);
        e.setKind(EvidenceKind.CANNOT_UPLOAD);
        e.setReasonCode(body.reasonCode());
        e.setReasonText(StringUtils.hasText(body.reasonText()) ? body.reasonText().trim() : null);
        e.setNote(StringUtils.hasText(body.note()) ? body.note().trim() : null);

        GoalEvidence saved = evidenceRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(evidenceService.toResponses(List.of(saved)).get(0)));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Operation(summary = "Delete evidence — the person who recorded it, or a Business Owner / Clinic Head")
    @DeleteMapping("/patients/{patientId}/evidence/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID patientId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireAccessible(patientId, principal);
        GoalEvidence e = evidenceService.requireOwned(id, principal.getOrgId(), patientId);

        boolean isOwner = e.getTherapistId().equals(principal.getId());
        boolean isAdmin = principal.getActiveRole() == Role.BUSINESS_OWNER || principal.getActiveRole() == Role.CLINIC_HEAD;
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete evidence you recorded");
        }

        if (e.getFileUrl() != null) storageService.delete(e.getFileUrl());
        evidenceRepository.delete(e);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Builds the common fields, checking the goal and session really belong to this child. */
    private GoalEvidence newEvidence(Patient patient, UUID goalId, UUID sessionId, UserPrincipal principal) {
        GoalEvidence e = new GoalEvidence();
        e.setOrgId(principal.getOrgId());
        e.setPatientId(patient.getId());
        e.setTherapistId(principal.getId());

        if (goalId != null) {
            IEPGoal goal = goalRepository.findByIdAndOrgId(goalId, principal.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("IEP goal not found"));
            IEPPlan plan = planRepository.findByIdAndOrgId(goal.getPlanId(), principal.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("IEP plan not found"));
            if (!plan.getPatientId().equals(patient.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "That goal belongs to a different child");
            }
            e.setGoalId(goal.getId());
            e.setPlanId(plan.getId());
            e.setEnrollmentId(plan.getEnrollmentId());
        }
        if (sessionId != null) {
            TherapySession session = sessionRepository.findById(sessionId)
                    .filter(s -> principal.getOrgId().equals(s.getOrgId()) && patient.getId().equals(s.getPatientId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That session belongs to a different child"));
            e.setSessionId(session.getId());
        }
        return e;
    }

    /** Org scoping plus the same child-level rules as shared media: linked parent, assigned therapist, or an admin. */
    private Patient requireAccessible(UUID patientId, UserPrincipal principal) {
        Patient patient = patientRepository.findByIdAndOrgId(patientId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Judged by the role the person is acting as — someone who is both a therapist and a parent
        // gets the rules of whichever hat they're wearing, not the strictest of both.
        Role acting = principal.getActiveRole();
        if (acting == Role.PARENT) {
            boolean linked = patientParentRepository.findById_PatientId(patientId).stream()
                    .anyMatch(pp -> pp.getId().getParentId().equals(principal.getId()));
            if (!linked) throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
        } else if (acting == Role.THERAPIST) {
            boolean assigned = therapistPatientRepository.findByPatientIdAndTherapistId(patientId, principal.getId())
                    .map(tp -> tp.isActive()).orElse(false);
            if (!assigned) throw new ApiException(HttpStatus.FORBIDDEN, "You are not assigned to this patient");
        }
        return patient;
    }
}
