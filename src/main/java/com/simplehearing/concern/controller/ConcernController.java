package com.simplehearing.concern.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.concern.dto.ConcernResponse;
import com.simplehearing.concern.dto.RaiseConcernRequest;
import com.simplehearing.concern.dto.ResolveConcernRequest;
import com.simplehearing.concern.entity.EnrollmentConcern;
import com.simplehearing.concern.enums.ConcernStatus;
import com.simplehearing.concern.repository.ConcernRepository;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Enrollment Concerns", description = "Parent-raised concerns about an ongoing program")
@RestController
@RequestMapping("/api/v1/enrollment-concerns")
public class ConcernController {

    private final ConcernRepository concernRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final OrganisationRepository organisationRepository;
    private final EmailService emailService;

    public ConcernController(
            ConcernRepository concernRepository,
            EnrollmentRepository enrollmentRepository,
            PatientRepository patientRepository,
            PatientParentRepository patientParentRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            OrganisationRepository organisationRepository,
            EmailService emailService) {
        this.concernRepository = concernRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.organisationRepository = organisationRepository;
        this.emailService = emailService;
    }

    // ── Raise ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Parent raises a concern about an active program")
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ConcernResponse>> raise(
            @Valid @RequestBody RaiseConcernRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(request.enrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (!isLinkedParent(enrollment.getPatientId(), principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "A concern can only be raised on an active program");
        }

        EnrollmentConcern concern = new EnrollmentConcern();
        concern.setOrgId(enrollment.getOrgId());
        concern.setEnrollmentId(enrollment.getId());
        concern.setPatientId(enrollment.getPatientId());
        concern.setTherapistId(enrollment.getTherapistId());
        concern.setRaisedBy(principal.getId());
        concern.setRaisedAt(Instant.now());
        concern.setDescription(request.description());
        concern.setStatus(ConcernStatus.OPEN);

        EnrollmentConcern saved = concernRepository.save(concern);

        notifyRaised(saved, enrollment);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── List ───────────────────────────────────────────────────────────────────

    @Operation(summary = "List concerns, filterable by enrollment, patient, or org-wide status")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ConcernResponse>>> list(
            @RequestParam(required = false) UUID enrollmentId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) ConcernStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean parent = isParent(principal);
        List<EnrollmentConcern> concerns;

        if (enrollmentId != null) {
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
            if (!enrollment.getOrgId().equals(principal.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
            }
            if (parent && !isLinkedParent(enrollment.getPatientId(), principal.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
            }
            concerns = concernRepository.findByOrgIdAndEnrollmentIdOrderByRaisedAtDesc(principal.getOrgId(), enrollmentId);
        } else if (patientId != null) {
            if (parent && !isLinkedParent(patientId, principal.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
            }
            concerns = concernRepository.findByOrgIdAndPatientIdOrderByRaisedAtDesc(principal.getOrgId(), patientId);
        } else {
            if (parent) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "patientId or enrollmentId is required");
            }
            if (isClinicianOnly(principal)) {
                concerns = status != null
                        ? concernRepository.findByOrgIdAndTherapistIdAndStatusOrderByRaisedAtDesc(principal.getOrgId(), principal.getId(), status)
                        : concernRepository.findByOrgIdAndTherapistIdOrderByRaisedAtDesc(principal.getOrgId(), principal.getId());
            } else {
                concerns = status != null
                        ? concernRepository.findByOrgIdAndStatusOrderByRaisedAtDesc(principal.getOrgId(), status)
                        : concernRepository.findByOrgIdOrderByRaisedAtDesc(principal.getOrgId());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(enrich(concerns)));
    }

    @Operation(summary = "Count of open concerns — for a dashboard badge")
    @GetMapping("/open-count")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> openCount(@AuthenticationPrincipal UserPrincipal principal) {
        int count = isClinicianOnly(principal)
                ? concernRepository.countByOrgIdAndTherapistIdAndStatus(principal.getOrgId(), principal.getId(), ConcernStatus.OPEN)
                : concernRepository.countByOrgIdAndStatus(principal.getOrgId(), ConcernStatus.OPEN);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ── Acknowledge / resolve ─────────────────────────────────────────────────

    @Operation(summary = "Acknowledge a concern")
    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<ConcernResponse>> acknowledge(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        EnrollmentConcern concern = findInOrg(id, principal);
        requireStaffAccess(concern, principal);

        if (concern.getStatus() != ConcernStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an open concern can be acknowledged");
        }
        concern.setStatus(ConcernStatus.ACKNOWLEDGED);
        concern.setAcknowledgedBy(principal.getId());
        concern.setAcknowledgedAt(Instant.now());

        EnrollmentConcern saved = concernRepository.save(concern);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    @Operation(summary = "Resolve a concern")
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<ConcernResponse>> resolve(
            @PathVariable UUID id,
            @RequestBody(required = false) ResolveConcernRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        EnrollmentConcern concern = findInOrg(id, principal);
        requireStaffAccess(concern, principal);

        if (concern.getStatus() == ConcernStatus.RESOLVED) {
            throw new ApiException(HttpStatus.CONFLICT, "This concern is already resolved");
        }
        concern.setStatus(ConcernStatus.RESOLVED);
        concern.setResolutionNotes(request != null ? request.resolutionNotes() : null);
        concern.setResolvedBy(principal.getId());
        concern.setResolvedAt(Instant.now());

        EnrollmentConcern saved = concernRepository.save(concern);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private EnrollmentConcern findInOrg(UUID id, UserPrincipal principal) {
        EnrollmentConcern concern = concernRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concern not found"));
        if (!concern.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return concern;
    }

    private void requireStaffAccess(EnrollmentConcern concern, UserPrincipal principal) {
        if (isClinicianOnly(principal) && !concern.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your patient's concern");
        }
    }

    private void notifyRaised(EnrollmentConcern concern, Enrollment enrollment) {
        Patient patient = patientRepository.findById(concern.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getFirstName() + " " + patient.getLastName() : "A patient";
        String programName = subscriptionRepository.findById(enrollment.getSubscriptionId())
                .map(Subscription::getProgramId)
                .flatMap(programRepository::findById)
                .map(p -> p.getName())
                .orElse("their program");
        String orgName = organisationRepository.findById(concern.getOrgId()).map(o -> o.getName()).orElse("Simple Hearing");

        Set<UUID> recipientIds = userRepository.findByOrgIdAndRoleIn(
                        concern.getOrgId(), List.of(Role.BUSINESS_OWNER, Role.CLINIC_HEAD))
                .stream().map(User::getId).collect(Collectors.toSet());
        List<String> recipients = userRepository.findAllById(
                        java.util.stream.Stream.concat(recipientIds.stream(), java.util.stream.Stream.of(concern.getTherapistId()))
                                .collect(Collectors.toSet()))
                .stream().map(User::getEmail).filter(e -> e != null && !e.isBlank()).toList();

        if (!recipients.isEmpty()) {
            emailService.sendNewConcernNotification(recipients, patientName, programName, concern.getDescription(), orgName);
        }
    }

    private boolean isLinkedParent(UUID patientId, UUID parentId) {
        return patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(parentId));
    }

    private static boolean isParent(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.PARENT);
    }

    /** A THERAPIST/DOCTOR who is not also an admin-tier role — scoped to their own assigned patients. */
    private static boolean isClinicianOnly(UserPrincipal principal) {
        User user = principal.getUser();
        boolean clinician = user.hasRole(Role.THERAPIST) || user.hasRole(Role.DOCTOR);
        boolean adminTier = user.hasRole(Role.BUSINESS_OWNER) || user.hasRole(Role.CLINIC_HEAD);
        return clinician && !adminTier;
    }

    private List<ConcernResponse> enrich(List<EnrollmentConcern> concerns) {
        if (concerns.isEmpty()) return List.of();

        Set<UUID> enrollmentIds = concerns.stream().map(EnrollmentConcern::getEnrollmentId).collect(Collectors.toSet());
        Map<UUID, Enrollment> enrollmentMap = enrollmentRepository.findAllById(enrollmentIds).stream()
                .collect(Collectors.toMap(Enrollment::getId, e -> e));

        Set<UUID> patientIds = concerns.stream().map(EnrollmentConcern::getPatientId).collect(Collectors.toSet());
        Map<UUID, Patient> patientMap = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));

        Set<UUID> therapistIds = concerns.stream().map(EnrollmentConcern::getTherapistId).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, String> programNames = new HashMap<>();
        for (Enrollment e : enrollmentMap.values()) {
            subscriptionRepository.findById(e.getSubscriptionId()).ifPresent(sub ->
                    programRepository.findById(sub.getProgramId()).ifPresent(prog ->
                            programNames.put(e.getId(), prog.getName())));
        }

        return concerns.stream().map(c -> {
            Patient patient = patientMap.get(c.getPatientId());
            User therapist = userMap.get(c.getTherapistId());
            return ConcernResponse.from(
                    c,
                    programNames.getOrDefault(c.getEnrollmentId(), "Unknown Program"),
                    patient != null ? patient.getFirstName() : "",
                    patient != null ? patient.getLastName() : "",
                    therapist != null ? therapist.getFirstName() : "",
                    therapist != null ? therapist.getLastName() : "");
        }).toList();
    }
}
