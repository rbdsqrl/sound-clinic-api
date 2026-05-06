package com.simplehearing.inquiry.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.inquiry.dto.*;
import com.simplehearing.inquiry.entity.Inquiry;
import com.simplehearing.inquiry.entity.InquiryLog;
import com.simplehearing.inquiry.enums.InquiryActionOutcome;
import com.simplehearing.inquiry.enums.InquiryLogType;
import com.simplehearing.inquiry.enums.InquiryStatus;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.inquiry.repository.InquiryLogRepository;
import com.simplehearing.inquiry.repository.InquiryRepository;
import com.simplehearing.invitation.service.InvitationService;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Inquiries", description = "Public inquiry submission and admin management")
@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryRepository inquiryRepository;
    private final InquiryLogRepository inquiryLogRepository;
    private final OrganisationRepository organisationRepository;
    private final PatientRepository patientRepository;
    private final InvitationService invitationService;

    @Value("${app.org-id:}")
    private String defaultOrgId;

    public InquiryController(InquiryRepository inquiryRepository,
                             InquiryLogRepository inquiryLogRepository,
                             OrganisationRepository organisationRepository,
                             PatientRepository patientRepository,
                             InvitationService invitationService) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryLogRepository = inquiryLogRepository;
        this.organisationRepository = organisationRepository;
        this.patientRepository = patientRepository;
        this.invitationService = invitationService;
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Operation(summary = "Aggregated inquiry analytics for the organisation")
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryAnalyticsResponse>> analytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID orgId = principal.getOrgId();
        List<Inquiry> all = orgId != null
                ? inquiryRepository.findByOrgIdOrderByCreatedAtDesc(orgId)
                : inquiryRepository.findAllByOrderByCreatedAtDesc();

        int total     = all.size();
        int converted = (int) all.stream().filter(i -> i.getStatus() == InquiryStatus.CONVERTED).count();
        double conversionRate = total > 0 ? Math.round((double) converted / total * 1000.0) / 10.0 : 0.0;

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int overdue = (int) all.stream()
                .filter(i -> i.getStatus() == InquiryStatus.NEW && i.getCreatedAt().isBefore(cutoff))
                .count();

        int readyToConvert = (int) all.stream()
                .filter(i -> i.getStatus() == InquiryStatus.VISITED)
                .count();

        // Count by status
        Map<String, Integer> byStatus = all.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getStatus().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Avg response time: createdAt → first activity log entry
        Double avgResponseHours = null;
        if (!all.isEmpty()) {
            List<UUID> ids = all.stream().map(Inquiry::getId).toList();
            List<InquiryLog> logs = inquiryLogRepository.findByInquiryIdInOrderByCreatedAtAsc(ids);

            // First log timestamp per inquiry
            Map<UUID, Instant> firstLogTime = new LinkedHashMap<>();
            for (InquiryLog log : logs) {
                firstLogTime.putIfAbsent(log.getInquiryId(), log.getCreatedAt());
            }

            Map<UUID, Instant> inquiryCreatedAt = all.stream()
                    .collect(Collectors.toMap(Inquiry::getId, Inquiry::getCreatedAt));

            OptionalDouble avg = firstLogTime.entrySet().stream()
                    .filter(e -> inquiryCreatedAt.containsKey(e.getKey()))
                    .mapToDouble(e -> {
                        Duration d = Duration.between(inquiryCreatedAt.get(e.getKey()), e.getValue());
                        return d.toMinutes() / 60.0;
                    })
                    .average();

            if (avg.isPresent()) {
                avgResponseHours = Math.round(avg.getAsDouble() * 10.0) / 10.0;
            }
        }

        return ResponseEntity.ok(ApiResponse.success(new InquiryAnalyticsResponse(
                total, converted, conversionRate, avgResponseHours,
                overdue, readyToConvert, byStatus
        )));
    }

    // ── Submit inquiry (public — no auth) ─────────────────────────────────────

    @Operation(summary = "Submit a new inquiry from the public website")
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> submit(
            @Valid @RequestBody CreateInquiryRequest request) {

        Inquiry inquiry = new Inquiry();
        inquiry.setName(request.name());
        inquiry.setEmail(request.email());
        inquiry.setPhone(request.phone());
        inquiry.setReason(request.reason());
        inquiry.setPreferredTime(request.preferredTime());
        inquiry.setStatus(InquiryStatus.NEW);

        // Resolve org: 1) from request body, 2) from app config, 3) first org in DB
        UUID resolvedOrgId = request.orgId();
        if (resolvedOrgId == null && defaultOrgId != null && !defaultOrgId.isBlank()) {
            try { resolvedOrgId = UUID.fromString(defaultOrgId); } catch (IllegalArgumentException ignored) {}
        }
        if (resolvedOrgId == null) {
            resolvedOrgId = organisationRepository.findAll().stream()
                    .findFirst().map(org -> org.getId()).orElse(null);
        }
        inquiry.setOrgId(resolvedOrgId);

        Inquiry saved = inquiryRepository.save(inquiry);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(InquiryResponse.from(saved)));
    }

    // ── List inquiries (admin roles) ──────────────────────────────────────────

    @Operation(summary = "List all inquiries for the organisation")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> list(
            @RequestParam(required = false) InquiryStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Inquiry> inquiries;
        UUID orgId = principal.getOrgId();

        if (orgId != null) {
            inquiries = (status != null)
                    ? inquiryRepository.findByOrgIdAndStatusOrderByCreatedAtDesc(orgId, status)
                    : inquiryRepository.findByOrgIdOrderByCreatedAtDesc(orgId);
        } else {
            inquiries = (status != null)
                    ? inquiryRepository.findAllByStatusOrderByCreatedAtDesc(status)
                    : inquiryRepository.findAllByOrderByCreatedAtDesc();
        }

        return ResponseEntity.ok(ApiResponse.success(
                inquiries.stream().map(InquiryResponse::from).toList()));
    }

    // ── Update inquiry status / notes / appointment ───────────────────────────

    @Operation(summary = "Update inquiry status, admin notes, and appointment")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateInquiryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));

        String actorName = principal.getUser().getFirstName() + " " + principal.getUser().getLastName();

        // Status change → auto-log
        if (request.status() != null && request.status() != inquiry.getStatus()) {
            InquiryStatus oldStatus = inquiry.getStatus();
            inquiry.setStatus(request.status());
            addLog(inquiry.getId(), InquiryLogType.STATUS_CHANGED,
                    "Status changed from " + oldStatus + " to " + request.status(),
                    principal.getId(), actorName);
        }

        if (request.adminNotes() != null) {
            inquiry.setAdminNotes(request.adminNotes());
        }

        // Appointment: explicit clear takes priority, then a new date value
        if (Boolean.TRUE.equals(request.clearAppointment())) {
            inquiry.setAppointmentDate(null);
            inquiry.setAppointmentNotes(null);
            addLog(inquiry.getId(), InquiryLogType.APPOINTMENT_CANCELLED,
                    "Pre-appointment cancelled", principal.getId(), actorName);
        } else if (request.appointmentDate() != null) {
            inquiry.setAppointmentDate(request.appointmentDate());
            String apptNotes = request.appointmentNotes() != null ? " — " + request.appointmentNotes() : "";
            addLog(inquiry.getId(), InquiryLogType.APPOINTMENT_SCHEDULED,
                    "Pre-appointment scheduled for " + request.appointmentDate() + apptNotes,
                    principal.getId(), actorName);
        }

        if (request.appointmentNotes() != null && !Boolean.TRUE.equals(request.clearAppointment())) {
            inquiry.setAppointmentNotes(request.appointmentNotes());
        }

        Inquiry saved = inquiryRepository.save(inquiry);
        return ResponseEntity.ok(ApiResponse.success(InquiryResponse.from(saved)));
    }

    // ── Activity log ──────────────────────────────────────────────────────────

    @Operation(summary = "Get all activity logs for an inquiry")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<InquiryLogResponse>>> getLogs(
            @PathVariable UUID id) {

        if (!inquiryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inquiry not found");
        }

        List<InquiryLogResponse> logs = inquiryLogRepository
                .findByInquiryIdOrderByCreatedAtAsc(id)
                .stream()
                .map(InquiryLogResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Add an activity log entry to an inquiry")
    @PostMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryLogResponse>> addLogEntry(
            @PathVariable UUID id,
            @Valid @RequestBody CreateInquiryLogRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!inquiryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inquiry not found");
        }

        String actorName = principal.getUser().getFirstName() + " " + principal.getUser().getLastName();
        InquiryLog log = addLog(id, request.logType(), request.notes(), principal.getId(), actorName);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(InquiryLogResponse.from(log)));
    }

    // ── Convert inquiry to patient ────────────────────────────────────────────

    @Operation(summary = "Convert an inquiry into a patient record")
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ConvertInquiryResponse>> convert(
            @PathVariable UUID id,
            @Valid @RequestBody ConvertInquiryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));

        // Create the patient record
        Patient patient = new Patient();
        patient.setOrgId(principal.getOrgId());
        patient.setClinicId(request.clinicId());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setStage(PatientStage.INQUIRY_CONVERTED);
        patient.setActive(true);

        // Carry over notes from inquiry reason if present
        if (inquiry.getReason() != null && !inquiry.getReason().isBlank()) {
            patient.setNotes("Inquiry reason: " + inquiry.getReason());
        }

        Patient savedPatient = patientRepository.save(patient);

        // Optionally invite the inquiry submitter and auto-link them to the new patient
        String linkedUserInviteLink = null;
        if (request.linkedUserEmail() != null && !request.linkedUserEmail().isBlank()) {
            Role linkedRole = request.linkedUserRole() != null ? request.linkedUserRole() : Role.PARENT;
            linkedUserInviteLink = invitationService.createLinkedInvitation(
                    request.linkedUserEmail().trim(),
                    linkedRole,
                    request.clinicId(),
                    savedPatient.getId(),
                    principal.getOrgId(),
                    principal.getId()
            );
        }

        // Mark inquiry as CONVERTED
        inquiry.setStatus(InquiryStatus.CONVERTED);
        inquiryRepository.save(inquiry);

        // Auto-log the conversion
        String actorName = principal.getUser().getFirstName() + " " + principal.getUser().getLastName();
        addLog(inquiry.getId(), InquiryLogType.CONVERTED,
                "Converted to patient: " + request.firstName() + " " + request.lastName()
                        + " (patient ID: " + savedPatient.getId() + ")",
                principal.getId(), actorName);

        ConvertInquiryResponse response = new ConvertInquiryResponse(
                savedPatient.getId(),
                savedPatient.getFirstName() + " " + savedPatient.getLastName(),
                linkedUserInviteLink
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── Next action (state machine) ───────────────────────────────────────────

    @Operation(summary = "Execute a next-action transition on an inquiry")
    @PostMapping("/{id}/next-action")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryResponse>> nextAction(
            @PathVariable UUID id,
            @Valid @RequestBody NextActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));

        String actorName = principal.getUser().getFirstName() + " " + principal.getUser().getLastName();
        String suffix    = request.notes() != null && !request.notes().isBlank()
                           ? ". " + request.notes() : "";

        InquiryStatus oldStatus = inquiry.getStatus();
        InquiryStatus newStatus;
        InquiryLogType logType;
        String logMessage;

        switch (request.outcome()) {

            case NO_ANSWER -> {
                newStatus  = InquiryStatus.ATTEMPTED_CONTACT;
                logType    = InquiryLogType.CALL;
                logMessage = "Called — no answer" + suffix;
            }
            case SPOKE_NO_PROGRESS -> {
                newStatus  = InquiryStatus.CONTACTED;
                logType    = InquiryLogType.CALL;
                logMessage = "Called — spoke to patient, follow-up needed" + suffix;
            }
            case APPOINTMENT_BOOKED -> {
                if (request.appointmentDate() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "appointmentDate is required for APPOINTMENT_BOOKED");
                }
                newStatus  = InquiryStatus.CONSULTATION_SCHEDULED;
                logType    = InquiryLogType.CALL;
                logMessage = "Called — appointment booked" + suffix;
                inquiry.setAppointmentDate(request.appointmentDate());
                if (request.appointmentNotes() != null) inquiry.setAppointmentNotes(request.appointmentNotes());
                addLog(inquiry.getId(), InquiryLogType.APPOINTMENT_SCHEDULED,
                        "Pre-appointment scheduled for " + request.appointmentDate() + suffix,
                        principal.getId(), actorName);
            }
            case REMINDER_SENT -> {
                newStatus  = oldStatus; // stays CONSULTATION_SCHEDULED
                logType    = InquiryLogType.NOTE;
                logMessage = "Reminder sent to patient" + suffix;
            }
            case VISITED -> {
                newStatus  = InquiryStatus.VISITED;
                logType    = InquiryLogType.NOTE;
                logMessage = "Patient attended consultation" + suffix;
            }
            case NO_SHOW -> {
                newStatus  = InquiryStatus.CONTACTED;
                logType    = InquiryLogType.NOTE;
                logMessage = "No show — patient did not attend appointment" + suffix;
            }
            case CANCELLED -> {
                newStatus  = InquiryStatus.DROPPED;
                logType    = InquiryLogType.NOTE;
                logMessage = "Appointment cancelled" + suffix;
            }
            case SCHEDULE_FOLLOWUP -> {
                if (request.appointmentDate() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "appointmentDate is required for SCHEDULE_FOLLOWUP");
                }
                newStatus  = InquiryStatus.CONSULTATION_SCHEDULED;
                logType    = InquiryLogType.NOTE;
                logMessage = "Follow-up consultation scheduled" + suffix;
                inquiry.setAppointmentDate(request.appointmentDate());
                if (request.appointmentNotes() != null) inquiry.setAppointmentNotes(request.appointmentNotes());
                addLog(inquiry.getId(), InquiryLogType.APPOINTMENT_SCHEDULED,
                        "Follow-up appointment scheduled for " + request.appointmentDate() + suffix,
                        principal.getId(), actorName);
            }
            case DROPPED -> {
                newStatus  = InquiryStatus.DROPPED;
                logType    = InquiryLogType.NOTE;
                logMessage = "Marked as dropped" + suffix;
            }
            case REOPEN -> {
                newStatus  = InquiryStatus.NEW;
                logType    = InquiryLogType.NOTE;
                logMessage = "Reopened" + suffix;
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown outcome: " + request.outcome());
        }

        // Apply status change
        inquiry.setStatus(newStatus);

        // Log the main activity
        addLog(inquiry.getId(), logType, logMessage, principal.getId(), actorName);

        // Auto-log the status transition (if changed)
        if (newStatus != oldStatus) {
            addLog(inquiry.getId(), InquiryLogType.STATUS_CHANGED,
                    "Status: " + oldStatus + " → " + newStatus,
                    principal.getId(), actorName);
        }

        Inquiry saved = inquiryRepository.save(inquiry);
        return ResponseEntity.ok(ApiResponse.success(InquiryResponse.from(saved)));
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private InquiryLog addLog(UUID inquiryId, InquiryLogType type, String notes,
                               UUID createdBy, String createdByName) {
        InquiryLog log = new InquiryLog();
        log.setInquiryId(inquiryId);
        log.setLogType(type);
        log.setNotes(notes);
        log.setCreatedBy(createdBy);
        log.setCreatedByName(createdByName);
        return inquiryLogRepository.save(log);
    }
}
