package com.simplehearing.enrollment.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.dto.AvailableTherapistResponse;
import com.simplehearing.enrollment.dto.CreateEnrollmentRequest;
import com.simplehearing.enrollment.dto.EnrollmentResponse;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.repository.LeaveRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
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

    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final LeaveRepository leaveRepository;
    private final SessionGenerationService sessionGenerationService;

    public EnrollmentController(
            EnrollmentRepository enrollmentRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            ClinicRepository clinicRepository,
            LeaveRepository leaveRepository,
            SessionGenerationService sessionGenerationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
        this.leaveRepository = leaveRepository;
        this.sessionGenerationService = sessionGenerationService;
    }

    // ── Available therapists for a given slot ─────────────────────────────────

    @Operation(summary = "Find therapists available for the given time/duration (any day)")
    @GetMapping("/available-therapists")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<List<AvailableTherapistResponse>>> availableTherapists(
            @RequestParam LocalTime startTime,
            @RequestParam int durationMinutes,
            @RequestParam LocalDate startDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 1. All therapists and doctors in the org
        List<User> therapists = userRepository.findByOrgIdAndRoleIn(
                principal.getOrgId(), List.of(Role.THERAPIST, Role.DOCTOR));

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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
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
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'ADMIN', 'BUSINESS_OWNER')")
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

        // Advance patient stage to ENROLLED if currently at ENROLLMENT
        patientRepository.findById(request.patientId()).ifPresent(patient -> {
            if (patient.getStage() == PatientStage.ENROLLMENT) {
                patient.setStage(PatientStage.ENROLLED);
                patientRepository.save(patient);
            }
        });

        // Generate individual session records
        sessionGenerationService.generateSessions(saved, sub.getNumSessions());

        // Build response with enriched names
        Program program = programRepository.findById(sub.getProgramId()).orElse(null);
        String programName = program != null ? program.getName() : "Unknown Program";

        EnrollmentResponse response = EnrollmentResponse.from(
                saved,
                therapist.getFirstName(),
                therapist.getLastName(),
                programName,
                sub.getNumSessions());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── Cancel enrollment ─────────────────────────────────────────────────────

    @Operation(summary = "Cancel an enrollment")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'ADMIN', 'BUSINESS_OWNER')")
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
            return EnrollmentResponse.from(e, fn, ln, prog, total);
        }).toList();
    }
}
