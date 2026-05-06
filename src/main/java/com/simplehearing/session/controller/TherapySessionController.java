package com.simplehearing.session.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.dto.TherapySessionResponse;
import com.simplehearing.session.dto.UpdateSessionStatusRequest;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Therapy Sessions", description = "Individual therapy session records")
@RestController
@RequestMapping("/api/v1/therapy-sessions")
public class TherapySessionController {

    private final TherapySessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public TherapySessionController(
            TherapySessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository,
            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    // ── List sessions (calendar / patient view) ────────────────────────────────

    @Operation(summary = "List therapy sessions, optionally filtered by date range and patient or therapist")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<TherapySessionResponse>>> list(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID therapistId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : start.plusMonths(1).minusDays(1);

        User caller = principal.getUser();
        Role role = caller.getRole();

        List<TherapySession> sessions;

        if (role == Role.THERAPIST || role == Role.DOCTOR) {
            // Therapists always see only their own sessions
            sessions = sessionRepository
                    .findByOrgIdAndTherapistIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                            principal.getOrgId(), principal.getId(), start, end);
        } else if (patientId != null) {
            sessions = sessionRepository
                    .findByOrgIdAndPatientIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                            principal.getOrgId(), patientId, start, end);
        } else if (therapistId != null) {
            sessions = sessionRepository
                    .findByOrgIdAndTherapistIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                            principal.getOrgId(), therapistId, start, end);
        } else {
            sessions = sessionRepository
                    .findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                            principal.getOrgId(), start, end);
        }

        return ResponseEntity.ok(ApiResponse.success(enrich(sessions)));
    }

    // ── Sessions for a specific enrollment ─────────────────────────────────────

    @Operation(summary = "List all sessions for a specific enrollment")
    @GetMapping("/by-enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<TherapySessionResponse>>> byEnrollment(
            @PathVariable UUID enrollmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<TherapySession> sessions = sessionRepository.findByEnrollmentIdOrderBySessionNumberAsc(enrollmentId);

        // Security: ensure the enrollment belongs to this org
        if (!sessions.isEmpty() && !sessions.get(0).getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(ApiResponse.success(enrich(sessions)));
    }

    // ── Update session status ──────────────────────────────────────────────────

    @Operation(summary = "Update therapy session status (COMPLETED / CANCELLED / NO_SHOW)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSessionStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy session not found"));

        if (!session.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Therapists can only update their own sessions
        User caller = principal.getUser();
        if ((caller.getRole() == Role.THERAPIST || caller.getRole() == Role.DOCTOR)
                && !session.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only update your own sessions");
        }

        session.setStatus(request.status());
        if (request.notes() != null) {
            session.setNotes(request.notes());
        }
        if (request.status() == TherapySessionStatus.COMPLETED) {
            session.setCompletedBy(principal.getId());
            session.setCompletedAt(Instant.now());

            // Increment sessionsCompleted on the enrollment
            enrollmentRepository.findById(session.getEnrollmentId()).ifPresent(enrollment -> {
                enrollment.setSessionsCompleted(enrollment.getSessionsCompleted() + 1);
                enrollmentRepository.save(enrollment);
            });
        }

        TherapySession saved = sessionRepository.save(session);
        List<TherapySessionResponse> enriched = enrich(List.of(saved));
        return ResponseEntity.ok(ApiResponse.success(enriched.get(0)));
    }

    // ── Enrichment helper ─────────────────────────────────────────────────────

    private List<TherapySessionResponse> enrich(List<TherapySession> sessions) {
        if (sessions.isEmpty()) return List.of();

        Set<UUID> patientIds    = sessions.stream().map(TherapySession::getPatientId).collect(Collectors.toSet());
        Set<UUID> therapistIds  = sessions.stream().map(TherapySession::getTherapistId).collect(Collectors.toSet());
        Set<UUID> enrollmentIds = sessions.stream().map(TherapySession::getEnrollmentId).collect(Collectors.toSet());

        Map<UUID, Patient> patientMap = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));
        Map<UUID, User> therapistMap = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // enrollment → subscription → program
        Map<UUID, Integer> totalSessionsMap = new HashMap<>();
        Map<UUID, String> programNameMap = new HashMap<>();

        for (UUID eid : enrollmentIds) {
            enrollmentRepository.findById(eid).ifPresent(enrollment -> {
                subscriptionRepository.findById(enrollment.getSubscriptionId()).ifPresent(sub -> {
                    totalSessionsMap.put(eid, sub.getNumSessions());
                    programRepository.findById(sub.getProgramId()).ifPresent(prog ->
                            programNameMap.put(eid, prog.getName()));
                });
            });
        }

        return sessions.stream().map(s -> {
            Patient patient = patientMap.get(s.getPatientId());
            User therapist  = therapistMap.get(s.getTherapistId());
            return TherapySessionResponse.from(
                    s,
                    patient  != null ? patient.getFirstName()   : "",
                    patient  != null ? patient.getLastName()    : "",
                    therapist != null ? therapist.getFirstName() : "",
                    therapist != null ? therapist.getLastName()  : "",
                    programNameMap.getOrDefault(s.getEnrollmentId(), "Unknown Program"),
                    totalSessionsMap.getOrDefault(s.getEnrollmentId(), 0));
        }).toList();
    }
}
