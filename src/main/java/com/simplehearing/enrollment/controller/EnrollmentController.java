package com.simplehearing.enrollment.controller;

import com.simplehearing.enrollment.dto.ChangeTherapistRequest;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.dto.AvailableTherapistResponse;
import com.simplehearing.enrollment.dto.CreateEnrollmentRequest;
import com.simplehearing.enrollment.dto.EnrollmentResponse;
import com.simplehearing.enrollment.dto.TherapistSignoffRequest;
import com.simplehearing.enrollment.dto.UpdateCareStatusRequest;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import java.time.Instant;
import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.repository.LeaveRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.entity.TherapistPatient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.review.service.ReviewMeetingService;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.service.SessionGenerationService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Enrollments", description = "Session enrollment and therapist availability")
@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final LeaveRepository leaveRepository;
    private final SessionGenerationService sessionGenerationService;
    private final TherapistPatientRepository therapistPatientRepository;
    private final ReviewMeetingService reviewMeetingService;
    private final TherapySessionRepository therapySessionRepository;
    private final ReviewMeetingRepository reviewMeetingRepository;

    public EnrollmentController(
            EnrollmentRepository enrollmentRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            ClinicRepository clinicRepository,
            LeaveRepository leaveRepository,
            SessionGenerationService sessionGenerationService,
            TherapistPatientRepository therapistPatientRepository,
            ReviewMeetingService reviewMeetingService,
            TherapySessionRepository therapySessionRepository,
            ReviewMeetingRepository reviewMeetingRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
        this.leaveRepository = leaveRepository;
        this.sessionGenerationService = sessionGenerationService;
        this.therapistPatientRepository = therapistPatientRepository;
        this.reviewMeetingService = reviewMeetingService;
        this.therapySessionRepository = therapySessionRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
    }

    // ── Available therapists for a given slot ─────────────────────────────────

    @Operation(summary = "Find therapists available for the given time/duration (any day)")
    @GetMapping("/available-therapists")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<AvailableTherapistResponse>>> availableTherapists(
            @RequestParam LocalTime startTime,
            @RequestParam int durationMinutes,
            @RequestParam LocalDate startDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 1. All therapists in the org
        List<User> therapists = userRepository.findByOrgIdAndRoleIn(
                principal.getOrgId(), List.of(Role.THERAPIST));

        if (therapists.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        // 2. Exclude those on approved leave on startDate
        Set<UUID> onLeave = leaveRepository
                .findByOrgIdAndLeaveDateAndStatus(principal.getOrgId(), startDate, LeaveStatus.APPROVED)
                .stream()
                .map(Leave::getTherapistId)
                .collect(Collectors.toSet());

        List<User> available = therapists.stream()
                .filter(u -> !onLeave.contains(u.getId()))
                .toList();

        if (available.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        // 3. Fetch clinic names
        Set<UUID> clinicIds = available.stream()
                .map(User::getClinicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> clinicNames = clinicRepository.findAllById(clinicIds).stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));

        List<AvailableTherapistResponse> result = available.stream()
                .map(u -> {
                    UUID clinicId = u.getClinicId();
                    String clinicName = clinicId != null ? clinicNames.getOrDefault(clinicId, "") : "";
                    return new AvailableTherapistResponse(u.getId(), u.getFirstName(), u.getLastName(), clinicId, clinicName);
                })
                .sorted(Comparator.comparing(AvailableTherapistResponse::firstName))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── List enrollments for a patient ────────────────────────────────────────

    @Operation(summary = "List enrollments for a patient")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> list(
            @RequestParam UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Enrollment> enrollments = enrollmentRepository
                .findByOrgIdAndPatientIdOrderByCreatedAtDesc(principal.getOrgId(), patientId);

        List<EnrollmentResponse> result = enrichEnrollments(enrollments);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create enrollment ─────────────────────────────────────────────────────

    @Operation(summary = "Create an enrollment for a subscription")
    @PostMapping
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> create(
            @Valid @RequestBody CreateEnrollmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Validate subscription belongs to org and is paid
        Subscription sub = subscriptionRepository.findById(request.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!sub.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Validate therapist exists and belongs to org
        User therapist = userRepository.findById(request.therapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        if (!therapist.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Therapist does not belong to this organisation");
        }

        // Create enrollment — day_of_week derived from start date (sessions are daily)
        Enrollment enrollment = new Enrollment();
        enrollment.setOrgId(principal.getOrgId());
        enrollment.setSubscriptionId(request.subscriptionId());
        enrollment.setPatientId(request.patientId());
        enrollment.setTherapistId(request.therapistId());
        enrollment.setSessionDurationMinutes(request.sessionDurationMinutes());
        enrollment.setStartDate(request.startDate());
        enrollment.setDayOfWeek(request.startDate().getDayOfWeek());
        enrollment.setStartTime(request.startTime());
        enrollment.setCreatedBy(principal.getId());

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Auto-link therapist to patient so they appear in the therapist's patient list
        therapistPatientRepository.findByPatientIdAndTherapistId(request.patientId(), request.therapistId())
                .ifPresentOrElse(tp -> {
                    if (!tp.isActive()) {
                        tp.setActive(true);
                        therapistPatientRepository.save(tp);
                    }
                }, () -> {
                    TherapistPatient link = new TherapistPatient();
                    link.setPatientId(request.patientId());
                    link.setTherapistId(request.therapistId());
                    link.setAssignedBy(principal.getId());
                    therapistPatientRepository.save(link);
                });

        // Advance patient stage to ENROLLED if currently at ENROLLMENT — or if they were previously
        // discharged and are now starting a new episode of care, so the funnel stays accurate.
        patientRepository.findById(request.patientId()).ifPresent(patient -> {
            if (patient.getStage() == PatientStage.ENROLLMENT || patient.getStage() == PatientStage.DISCHARGED) {
                patient.setStage(PatientStage.ENROLLED);
                patientRepository.save(patient);
            }
        });

        // Generate individual session records
        List<TherapySession> sessions = sessionGenerationService.generateSessions(saved, sub.getNumSessions());

        // The plan's end is whatever the caller gave us, else the date the last session lands on
        LocalDate endDate = request.endDate();
        if (endDate == null && !sessions.isEmpty()) {
            endDate = sessions.get(sessions.size() - 1).getSessionDate();
        }
        if (endDate != null) {
            saved.setEndDate(endDate);
            saved = enrollmentRepository.save(saved);
        }

        // Review meetings are opt-in — only generated when the setup flow asked for them
        if (request.reviewSchedule() != null) {
            reviewMeetingService.generateForEnrollment(saved, request.reviewSchedule(), principal.getId());
        }

        // Build response with enriched names
        Program program = programRepository.findById(sub.getProgramId()).orElse(null);
        String programName = program != null ? program.getName() : "Unknown Program";

        EnrollmentResponse response = EnrollmentResponse.from(
                saved,
                therapist.getFirstName(),
                therapist.getLastName(),
                programName,
                0, // freshly created — no sessions can be completed yet
                sub.getNumSessions());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── Change the assigned therapist ─────────────────────────────────────────

    @Operation(
        summary = "Hand an ongoing plan to a different therapist",
        description = "Moves the enrollment and everything still ahead of it — scheduled sessions and "
                    + "upcoming review meetings — to the new therapist. Sessions already completed, "
                    + "cancelled or marked no-show keep the therapist who actually took them, so the "
                    + "clinical history stays accurate."
    )
    @PatchMapping("/{id}/therapist")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> changeTherapist(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTherapistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This plan is cancelled — reassigning a therapist would have no effect");
        }
        if (enrollment.getTherapistId().equals(request.therapistId())) {
            throw new ApiException(HttpStatus.CONFLICT, "That therapist is already assigned to this plan");
        }

        User therapist = userRepository.findById(request.therapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        if (!therapist.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Therapist belongs to another organisation");
        }
        if (!therapist.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "That therapist is deactivated");
        }
        if (!therapist.hasRole(Role.THERAPIST)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That user is not a therapist");
        }

        UUID previousTherapistId = enrollment.getTherapistId();
        enrollment.setTherapistId(therapist.getId());
        Enrollment saved = enrollmentRepository.save(enrollment);

        // Only what is still ahead moves across; the past keeps its own record.
        LocalDate today = LocalDate.now();
        List<TherapySession> sessions = therapySessionRepository.findByEnrollmentIdOrderBySessionNumberAsc(id);
        int movedSessions = 0;
        for (TherapySession session : sessions) {
            if (session.getStatus() == TherapySessionStatus.SCHEDULED
                    && !session.getSessionDate().isBefore(today)) {
                session.setTherapistId(therapist.getId());
                therapySessionRepository.save(session);
                movedSessions++;
            }
        }

        int movedMeetings = 0;
        for (ReviewMeeting meeting : reviewMeetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(id)) {
            if (meeting.getStatus() == ReviewMeetingStatus.SCHEDULED
                    && !meeting.getMeetingDate().isBefore(today)) {
                meeting.setTherapistId(therapist.getId());
                reviewMeetingRepository.save(meeting);
                movedMeetings++;
            }
        }

        // Keep the caseload link in step so the new therapist sees the patient.
        therapistPatientRepository
                .findByPatientIdAndTherapistId(enrollment.getPatientId(), therapist.getId())
                .ifPresentOrElse(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        therapistPatientRepository.save(existing);
                    }
                }, () -> {
                    TherapistPatient link = new TherapistPatient();
                    link.setPatientId(enrollment.getPatientId());
                    link.setTherapistId(therapist.getId());
                    link.setAssignedBy(principal.getId());
                    therapistPatientRepository.save(link);
                });

        log.info("Enrollment {} moved from therapist {} to {} — {} session(s), {} review meeting(s)",
                id, previousTherapistId, therapist.getId(), movedSessions, movedMeetings);

        List<EnrollmentResponse> enriched = enrichEnrollments(List.of(saved));
        return ResponseEntity.ok(ApiResponse.success(enriched.get(0)));
    }

    // ── Cancel enrollment ─────────────────────────────────────────────────────

    @Operation(summary = "Cancel an enrollment")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Enrollment is already cancelled");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> enriched = enrichEnrollments(List.of(saved));
        return ResponseEntity.ok(ApiResponse.success(enriched.get(0)));
    }

    // ── Update care status ────────────────────────────────────────────────────

    @Operation(
        summary = "Set the clinical-health signal on an active enrollment",
        description = "PROGRAM_COMPLETED also flips the enrollment's status to COMPLETED. "
                    + "Does not touch the patient's own stage — discharge is a patient-level event, not a program one."
    )
    @PatchMapping("/{id}/care-status")
    @PreAuthorize("hasAnyRole('THERAPIST', 'CLINIC_HEAD', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> updateCareStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCareStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        boolean isAdminTier = principal.getUser().hasRole(Role.CLINIC_HEAD)
                || principal.getUser().hasRole(Role.BUSINESS_OWNER)
                || principal.getUser().hasRole(Role.CLINIC_HEAD);
        if (!isAdminTier && !enrollment.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not the assigned therapist for this plan");
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Care status can only be set on an active enrollment");
        }

        enrollment.setCareStatus(request.careStatus());
        enrollment.setCareStatusNote(request.note());
        enrollment.setCareStatusUpdatedBy(principal.getId());
        enrollment.setCareStatusUpdatedAt(Instant.now());
        if (request.careStatus() == EnrollmentCareStatus.PROGRAM_COMPLETED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> enriched = enrichEnrollments(List.of(saved));
        return ResponseEntity.ok(ApiResponse.success(enriched.get(0)));
    }

    // ── Therapist sign-off ────────────────────────────────────────────────────

    @Operation(
        summary = "Assigned therapist confirms the program's goals were met",
        description = "One of the three discharge success criteria. Only available once the "
                    + "enrollment's care status is REVIEW or PROGRAM_COMPLETED."
    )
    @PatchMapping("/{id}/therapist-signoff")
    @PreAuthorize("hasAnyRole('THERAPIST')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> therapistSignoff(
            @PathVariable UUID id,
            @RequestBody(required = false) TherapistSignoffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (!enrollment.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not the assigned therapist for this plan");
        }
        if (enrollment.getCareStatus() != EnrollmentCareStatus.REVIEW
                && enrollment.getCareStatus() != EnrollmentCareStatus.PROGRAM_COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sign-off is only available once this program's care status is Review or Program Completed");
        }

        enrollment.setTherapistSignedOff(true);
        enrollment.setTherapistSignoffBy(principal.getId());
        enrollment.setTherapistSignoffAt(Instant.now());
        enrollment.setTherapistSignoffNotes(request != null ? request.notes() : null);

        Enrollment saved = enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> enriched = enrichEnrollments(List.of(saved));
        return ResponseEntity.ok(ApiResponse.success(enriched.get(0)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<EnrollmentResponse> enrichEnrollments(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) return List.of();

        Set<UUID> therapistIds = enrollments.stream().map(Enrollment::getTherapistId).collect(Collectors.toSet());
        Set<UUID> subscriptionIds = enrollments.stream().map(Enrollment::getSubscriptionId).collect(Collectors.toSet());

        Map<UUID, User> userMap = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, String> programNames = new HashMap<>();
        for (UUID subId : subscriptionIds) {
            subscriptionRepository.findById(subId).ifPresent(sub ->
                    programRepository.findById(sub.getProgramId()).ifPresent(prog ->
                            programNames.put(subId, prog.getName())));
        }

        // total sessions per subscription
        Map<UUID, Integer> totalSessions = new HashMap<>();
        for (UUID subId : subscriptionIds) {
            subscriptionRepository.findById(subId).ifPresent(sub ->
                    totalSessions.put(subId, sub.getNumSessions()));
        }

        return enrollments.stream().map(e -> {
            User therapist = userMap.get(e.getTherapistId());
            String fn = therapist != null ? therapist.getFirstName() : "";
            String ln = therapist != null ? therapist.getLastName() : "";
            String prog = programNames.getOrDefault(e.getSubscriptionId(), "Unknown Program");
            int total = totalSessions.getOrDefault(e.getSubscriptionId(), 0);
            int completed = therapySessionRepository.countByEnrollmentIdAndStatusAndCountsTowardPlanTrue(
                    e.getId(), TherapySessionStatus.COMPLETED);
            return EnrollmentResponse.from(e, fn, ln, prog, completed, total);
        }).toList();
    }
}
