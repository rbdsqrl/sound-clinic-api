package com.simplehearing.parentfeedback.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.parentfeedback.dto.ParentFeedbackRequest;
import com.simplehearing.parentfeedback.dto.ParentFeedbackResponse;
import com.simplehearing.parentfeedback.entity.ParentSessionFeedback;
import com.simplehearing.parentfeedback.repository.ParentSessionFeedbackRepository;
import com.simplehearing.parentfeedback.service.ParentFeedbackService;
import com.simplehearing.patient.entity.PatientParent;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Parent Feedback", description = "A parent's feedback on a completed session — admin roles first, shared with the therapist on request")
@RestController
@RequestMapping("/api/v1/parent-feedback")
public class ParentFeedbackController {

    private final ParentSessionFeedbackRepository repository;
    private final TherapySessionRepository sessionRepository;
    private final PatientParentRepository patientParentRepository;
    private final ParentFeedbackService service;

    public ParentFeedbackController(ParentSessionFeedbackRepository repository,
                                    TherapySessionRepository sessionRepository,
                                    PatientParentRepository patientParentRepository,
                                    ParentFeedbackService service) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.patientParentRepository = patientParentRepository;
        this.service = service;
    }

    // ── Parent ────────────────────────────────────────────────────────────────

    @Operation(summary = "Give (or edit) feedback on a completed session of your own child")
    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('PARENT')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentFeedbackResponse>> give(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ParentFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy session not found"));
        if (!session.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        boolean isTheirChild = patientParentRepository.findById_PatientId(session.getPatientId()).stream()
                .map(PatientParent::getId).anyMatch(id -> id.getParentId().equals(principal.getId()));
        if (!isTheirChild) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only give feedback on your own child's sessions");
        }
        if (session.getStatus() != TherapySessionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Feedback can be given once the session has been completed");
        }

        ParentSessionFeedback feedback = repository.findBySessionIdAndParentId(sessionId, principal.getId())
                .orElseGet(() -> {
                    ParentSessionFeedback f = new ParentSessionFeedback();
                    f.setOrgId(principal.getOrgId());
                    f.setSessionId(sessionId);
                    f.setPatientId(session.getPatientId());
                    f.setParentId(principal.getId());
                    return f;
                });
        boolean changed = !request.message().trim().equals(feedback.getMessage());
        feedback.setMessage(request.message().trim());
        if (changed) {
            // An edited feedback goes back to the admins' list; if it had been shared, the therapist sees the update too.
            feedback.setAdminReviewedAt(null);
            feedback.setAdminReviewedBy(null);
            feedback.setTherapistSeenAt(null);
        }
        ParentSessionFeedback saved = repository.save(feedback);
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(List.of(saved)).get(0)));
    }

    @Operation(
        summary = "The feedback on one session that the caller is allowed to see",
        description = "Admin roles see all of it; a therapist only what was shared with them on their own session; a "
                    + "parent only their own feedback on their own child's session. Anyone else gets an empty list."
    )
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ParentFeedbackResponse>>> forSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy session not found"));
        if (!session.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        // Judged on the role being worn, like the rest of the app — someone who is a parent and an admin sees
        // what that hat allows.
        Role role = principal.getActiveRole();
        List<ParentSessionFeedback> all = repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<ParentSessionFeedback> visible;
        if (role == Role.CLINIC_HEAD || role == Role.BUSINESS_OWNER || role == Role.OFFICE_ADMIN) {
            visible = all;
        } else if (role == Role.THERAPIST) {
            visible = session.getTherapistId().equals(principal.getId())
                    ? all.stream().filter(ParentSessionFeedback::isSharedWithTherapist).toList() : List.of();
        } else if (role == Role.PARENT) {
            visible = all.stream().filter(f -> f.getParentId().equals(principal.getId())).toList();
        } else {
            visible = List.of();
        }
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(visible)));
    }

    @Operation(summary = "The caller's own feedback on one plan's sessions")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('PARENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ParentFeedbackResponse>>> mine(
            @RequestParam UUID enrollmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.toResponses(repository.findMineForEnrollment(principal.getId(), enrollmentId))));
    }

    // ── Admin roles ───────────────────────────────────────────────────────────

    @Operation(summary = "Parent feedback no admin has read yet, newest first")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ParentFeedbackResponse>>> unreviewed(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(
                repository.findByOrgIdAndAdminReviewedAtIsNullOrderByCreatedAtDesc(principal.getOrgId()))));
    }

    @Operation(summary = "Share a parent's feedback with the session's therapist — also marks it read")
    @PatchMapping("/{id}/share")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentFeedbackResponse>> share(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        ParentSessionFeedback f = findInOrg(id, principal);
        Instant now = Instant.now();
        if (!f.isSharedWithTherapist()) {
            f.setSharedWithTherapist(true);
            f.setSharedAt(now);
            f.setSharedBy(principal.getId());
            f.setTherapistSeenAt(null);
        }
        if (f.getAdminReviewedAt() == null) {
            f.setAdminReviewedAt(now);
            f.setAdminReviewedBy(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(List.of(repository.save(f))).get(0)));
    }

    @Operation(summary = "Mark a parent's feedback as read without sharing it — takes it off the dashboard")
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentFeedbackResponse>> review(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        ParentSessionFeedback f = findInOrg(id, principal);
        if (f.getAdminReviewedAt() == null) {
            f.setAdminReviewedAt(Instant.now());
            f.setAdminReviewedBy(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(List.of(repository.save(f))).get(0)));
    }

    // ── Therapist ─────────────────────────────────────────────────────────────

    @Operation(summary = "Feedback an admin shared with the calling therapist that they haven't marked seen")
    @GetMapping("/shared")
    @PreAuthorize("hasRole('THERAPIST')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ParentFeedbackResponse>>> shared(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.toResponses(repository.findSharedUnseenForTherapist(principal.getId()))));
    }

    @Operation(summary = "Mark shared feedback as seen — takes it off the therapist's dashboard")
    @PatchMapping("/{id}/seen")
    @PreAuthorize("hasRole('THERAPIST')")
    @Transactional
    public ResponseEntity<ApiResponse<ParentFeedbackResponse>> seen(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        ParentSessionFeedback f = findInOrg(id, principal);
        TherapySession session = sessionRepository.findById(f.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapy session not found"));
        if (!f.isSharedWithTherapist() || !session.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This feedback has not been shared with you");
        }
        if (f.getTherapistSeenAt() == null) f.setTherapistSeenAt(Instant.now());
        return ResponseEntity.ok(ApiResponse.success(service.toResponses(List.of(repository.save(f))).get(0)));
    }

    private ParentSessionFeedback findInOrg(UUID id, UserPrincipal principal) {
        ParentSessionFeedback f = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        if (!f.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return f;
    }
}
