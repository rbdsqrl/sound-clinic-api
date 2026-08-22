package com.simplehearing.session.controller;

import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.notification.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.simplehearing.patient.entity.TherapistPatient;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.session.dto.CreateAdHocSessionRequest;
import com.simplehearing.session.dto.RescheduleSessionRequest;
import com.simplehearing.session.dto.SessionAttachmentResponse;
import com.simplehearing.session.dto.TherapySessionResponse;
import com.simplehearing.session.dto.UpdateSessionNotesRequest;
import com.simplehearing.session.dto.UpdateSessionStatusRequest;
import com.simplehearing.session.entity.SessionAttachment;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.SessionAttachmentRepository;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.storage.StorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
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
    private final SessionAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final TherapistPatientRepository therapistPatientRepository;
    private final PatientParentRepository patientParentRepository;
    private final OrganisationRepository organisationRepository;
    private final EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(TherapySessionController.class);
    private static final DateTimeFormatter WHEN_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter WHEN_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Sessions per therapy plan a parent may ask to move. */
    public static final int PARENT_RESCHEDULE_LIMIT = 3;

    public TherapySessionController(
            TherapySessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            SessionAttachmentRepository attachmentRepository,
            StorageService storageService,
            TherapistPatientRepository therapistPatientRepository,
            PatientParentRepository patientParentRepository,
            OrganisationRepository organisationRepository,
            EmailService emailService) {
        this.sessionRepository    = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository    = programRepository;
        this.patientRepository    = patientRepository;
        this.userRepository       = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageService       = storageService;
        this.therapistPatientRepository = therapistPatientRepository;
        this.patientParentRepository = patientParentRepository;
        this.organisationRepository = organisationRepository;
        this.emailService = emailService;
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
            @RequestParam(required = false) TherapySessionStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Status-only query (e.g. fetch all PENDING_RESCHEDULE for the dashboard)
        if (status != null && from == null && to == null && patientId == null && therapistId == null) {
            List<TherapySession> byStatus = (status == TherapySessionStatus.PENDING_RESCHEDULE)
                    ? sessionRepository.findAllPendingReschedule(principal.getOrgId())
                    : sessionRepository.findByOrgIdAndStatus(principal.getOrgId(), status);
            return ResponseEntity.ok(ApiResponse.success(enrich(byStatus)));
        }

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : start.plusMonths(1).minusDays(1);

        User caller = principal.getUser();
        Role role   = caller.getRole();

        List<TherapySession> sessions;
        if (role == Role.THERAPIST || role == Role.DOCTOR) {
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

        if (!sessions.isEmpty() && !sessions.get(0).getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Therapists and doctors can only see sessions for enrollments assigned to them
        Role role = principal.getUser().getRole();
        if (role == Role.THERAPIST || role == Role.DOCTOR) {
            enrollmentRepository.findById(enrollmentId).ifPresent(enrollment -> {
                if (!enrollment.getTherapistId().equals(principal.getId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
                }
            });
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

        TherapySession session = findOwned(id, principal);
        requireTherapistOwnership(session, principal);

        Role callerRole = principal.getUser().getRole();
        if (request.status() == TherapySessionStatus.CANCELLED
                && (callerRole == Role.THERAPIST || callerRole == Role.DOCTOR)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Therapists cannot cancel sessions directly — use cancellation-request instead");
        }

        session.setStatus(request.status());
        if (request.notes() != null) session.setNotes(request.notes());

        if (request.status() == TherapySessionStatus.COMPLETED) {
            session.setCompletedBy(principal.getId());
            session.setCompletedAt(Instant.now());
            enrollmentRepository.findById(session.getEnrollmentId()).ifPresent(enrollment -> {
                enrollment.setSessionsCompleted(enrollment.getSessionsCompleted() + 1);
                enrollmentRepository.save(enrollment);
            });
        }

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Update session notes / feedback / progress report ─────────────────────

    @Operation(summary = "Update session feedback, progress report, and notes")
    @PatchMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> updateNotes(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSessionNotesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);
        requireTherapistOwnership(session, principal);

        if (request.feedback()          != null) session.setFeedback(request.feedback());
        if (request.progressReport()    != null) session.setProgressReport(request.progressReport());
        if (request.notes()             != null) session.setNotes(request.notes());
        if (request.performanceScore()  != null) session.setPerformanceScore(request.performanceScore());

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Reschedule a session (new date and/or substitute therapist) ───────────

    @Operation(summary = "Reschedule a PENDING_RESCHEDULE session — set a new date and/or substitute therapist")
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> reschedule(
            @PathVariable UUID id,
            @RequestBody RescheduleSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);

        // Only a session that is still ahead can be moved. A completed or cancelled one
        // is a record of what happened, not a plan.
        if (session.getStatus() != TherapySessionStatus.SCHEDULED
                && session.getStatus() != TherapySessionStatus.PENDING_RESCHEDULE) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only a scheduled session can be rescheduled — this one is "
                            + session.getStatus().name().toLowerCase().replace('_', ' '));
        }

        if (request.newDate() == null && request.newStartTime() == null
                && request.substituteTherapistId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Provide a new date, a new time, or a substitute therapist");
        }

        // Captured before the change so the email can show what moved.
        LocalDate oldDate = session.getSessionDate();
        LocalTime oldStart = session.getStartTime();
        UUID previousTherapistId = session.getTherapistId();

        if (request.newDate() != null) {
            session.setSessionDate(request.newDate());
        }

        if (request.newStartTime() != null) {
            // Keep the session's length; only the start moves.
            long minutes = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            session.setStartTime(request.newStartTime());
            session.setEndTime(request.newStartTime().plusMinutes(minutes));
        }

        if (request.substituteTherapistId() != null) {
            User sub = userRepository.findById(request.substituteTherapistId())
                    .filter(u -> u.getOrgId().equals(principal.getOrgId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Therapist not found in your organisation"));
            UUID oldTherapistId = session.getTherapistId();
            session.setTherapistId(sub.getId());

            // Update enrollment to point to the substitute therapist
            enrollmentRepository.findById(session.getEnrollmentId()).ifPresent(enrollment -> {
                enrollment.setTherapistId(sub.getId());
                enrollmentRepository.save(enrollment);
            });

            // Ensure the substitute is linked to the patient
            therapistPatientRepository.findByPatientIdAndTherapistId(session.getPatientId(), sub.getId())
                    .ifPresentOrElse(tp -> {
                        if (!tp.isActive()) { tp.setActive(true); therapistPatientRepository.save(tp); }
                    }, () -> {
                        TherapistPatient link = new TherapistPatient();
                        link.setPatientId(session.getPatientId());
                        link.setTherapistId(sub.getId());
                        link.setAssignedBy(principal.getId());
                        therapistPatientRepository.save(link);
                    });
        }

        session.setStatus(TherapySessionStatus.SCHEDULED);
        session.setRescheduleReason(null);
        session.setRescheduleRequestedBy(null);
        // Counted here rather than derived from status, which is cleared on the next line.
        session.setRescheduleCount(session.getRescheduleCount() + 1);
        TherapySession saved = sessionRepository.save(session);

        notifyRescheduled(saved, oldDate, oldStart, previousTherapistId, request.reason());

        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    /**
     * Emails everyone the change affects: each parent linked to the patient, the therapist
     * now taking it, and — when the session changed hands — the therapist who no longer is.
     */
    private void notifyRescheduled(TherapySession session, LocalDate oldDate, LocalTime oldStart,
                                   UUID previousTherapistId, String reason) {
        try {
            String patientName = patientRepository.findById(session.getPatientId())
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .orElse("your child");
            String therapistName = userRepository.findById(session.getTherapistId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("your therapist");
            String orgName = organisationRepository.findById(session.getOrgId())
                    .map(o -> o.getName()).orElse("Simple Hearing");

            String oldWhen = oldDate.format(WHEN_DATE) + " at " + oldStart.format(WHEN_TIME);
            String newWhen = session.getSessionDate().format(WHEN_DATE) + " at "
                           + session.getStartTime().format(WHEN_TIME);
            String url = "/patients/" + session.getPatientId();

            String intro = (reason != null && !reason.isBlank())
                    ? "This therapy session has been moved. " + reason.trim()
                    : "This therapy session has been moved to a new time.";

            List<User> recipients = new ArrayList<>();

            patientParentRepository.findById_PatientId(session.getPatientId()).stream()
                    .map(pp -> pp.getId().getParentId())
                    .forEach(parentId -> userRepository.findById(parentId).ifPresent(recipients::add));

            userRepository.findById(session.getTherapistId()).ifPresent(recipients::add);
            if (previousTherapistId != null && !previousTherapistId.equals(session.getTherapistId())) {
                userRepository.findById(previousTherapistId).ifPresent(recipients::add);
            }

            for (User r : recipients) {
                if (r.getEmail() == null || r.getEmail().isBlank()) continue;
                emailService.sendSessionRescheduledEmail(
                        r.getEmail(), r.getFirstName(), patientName, therapistName,
                        oldWhen, newWhen, orgName, url, intro);
            }
            log.info("Session {} rescheduled — notified {} recipient(s)", session.getId(), recipients.size());
        } catch (Exception e) {
            // A notification failure must not undo a reschedule the user has already been told succeeded.
            log.error("Session {} rescheduled but notifications failed: {}", session.getId(), e.getMessage());
        }
    }

    // ── Ad-hoc session booked from the calendar ────────────────────────────────

    @Operation(
        summary = "Book a one-off therapy session",
        description = "For a catch-up or an extra visit scheduled by hand from the calendar. "
                    + "The session belongs to the patient's plan; countsTowardPlan decides whether "
                    + "it consumes one of the sessions the family paid for."
    )
    @PostMapping("/ad-hoc")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> createAdHoc(
            @Valid @RequestBody CreateAdHocSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after the start time");
        }

        Enrollment enrollment = enrollmentRepository.findById(request.enrollmentId())
                .filter(e -> e.getOrgId().equals(principal.getOrgId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Therapy plan not found"));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "That therapy plan is cancelled");
        }

        UUID therapistId = request.therapistId() != null ? request.therapistId() : enrollment.getTherapistId();
        User therapist = userRepository.findById(therapistId)
                .filter(u -> u.getOrgId().equals(principal.getOrgId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Therapist not found"));

        // Sequence continues from the plan's existing sessions; the number is an ordering
        // key, so an extra session still gets the next one.
        int nextNumber = sessionRepository.findByEnrollmentIdOrderBySessionNumberAsc(enrollment.getId())
                .stream()
                .mapToInt(TherapySession::getSessionNumber)
                .max().orElse(0) + 1;

        TherapySession session = new TherapySession();
        session.setOrgId(enrollment.getOrgId());
        session.setEnrollmentId(enrollment.getId());
        session.setPatientId(enrollment.getPatientId());
        session.setTherapistId(therapist.getId());
        session.setSessionNumber(nextNumber);
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus(TherapySessionStatus.SCHEDULED);
        session.setAdHoc(true);
        session.setCountsTowardPlan(request.countsTowardPlan());
        session.setNotes(request.notes());

        TherapySession saved = sessionRepository.save(session);
        log.info("Ad-hoc session {} booked for patient {} — countsTowardPlan={}",
                saved.getId(), saved.getPatientId(), request.countsTowardPlan());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Parent reschedule request ──────────────────────────────────────────────

    @Operation(summary = "Request reschedule of a SCHEDULED session (parent only)")
    @PostMapping("/{id}/reschedule-request")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> rescheduleRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);

        if (session.getStatus() != TherapySessionStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only SCHEDULED sessions can be requested for reschedule");
        }

        // The allowance is counted in sessions, not requests: asking again about a session
        // already on the list does not spend another one.
        if (!session.isParentRescheduleRequested()) {
            int used = sessionRepository
                    .countByEnrollmentIdAndParentRescheduleRequestedTrue(session.getEnrollmentId());
            if (used >= PARENT_RESCHEDULE_LIMIT) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "You have already rescheduled " + PARENT_RESCHEDULE_LIMIT
                                + " sessions on this therapy plan, which is the limit. "
                                + "Please contact the clinic if you need to move another.");
            }
        }

        session.setParentRescheduleRequested(true);
        session.setStatus(TherapySessionStatus.PENDING_RESCHEDULE);
        session.setRescheduleReason(com.simplehearing.session.enums.RescheduleReason.PARENT_REQUEST);
        session.setRescheduleRequestedBy(principal.getId());

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Cancellation request ──────────────────────────────────────────────────

    @Operation(summary = "Request cancellation of a SCHEDULED session — requires admin approval")
    @PostMapping("/{id}/cancellation-request")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'ADMIN', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> cancellationRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);
        requireTherapistOwnership(session, principal);

        if (session.getStatus() != TherapySessionStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only SCHEDULED sessions can be requested for cancellation");
        }

        session.setStatus(TherapySessionStatus.CANCELLATION_REQUESTED);
        session.setRescheduleRequestedBy(principal.getId());

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Approve cancellation ──────────────────────────────────────────────────

    @Operation(summary = "Approve a cancellation request — sets session to CANCELLED")
    @PostMapping("/{id}/approve-cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> approveCancellation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);

        if (session.getStatus() != TherapySessionStatus.CANCELLATION_REQUESTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Session is not pending cancellation approval");
        }

        session.setStatus(TherapySessionStatus.CANCELLED);
        session.setRescheduleRequestedBy(null);

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Reject cancellation ───────────────────────────────────────────────────

    @Operation(summary = "Reject a cancellation request — reverts session to SCHEDULED")
    @PostMapping("/{id}/reject-cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<TherapySessionResponse>> rejectCancellation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);

        if (session.getStatus() != TherapySessionStatus.CANCELLATION_REQUESTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Session is not pending cancellation approval");
        }

        session.setStatus(TherapySessionStatus.SCHEDULED);
        session.setRescheduleRequestedBy(null);

        TherapySession saved = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Upload attachment ──────────────────────────────────────────────────────

    @Operation(summary = "Upload a file attachment to a session")
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<SessionAttachmentResponse>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        TherapySession session = findOwned(id, principal);
        requireTherapistOwnership(session, principal);

        String url = storageService.store(file, "sessions/" + id);

        SessionAttachment att = new SessionAttachment();
        att.setOrgId(session.getOrgId());
        att.setSessionId(id);
        att.setTherapistId(principal.getId());
        att.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        att.setFileUrl(url);
        att.setContentType(file.getContentType());
        att.setFileSizeBytes(file.getSize());

        SessionAttachment saved = attachmentRepository.save(att);
        String presignedUrl = storageService.presign(saved.getFileUrl(), Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(SessionAttachmentResponse.from(saved, presignedUrl)));
    }

    // ── List attachments ───────────────────────────────────────────────────────

    @Operation(summary = "List all attachments for a session")
    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<SessionAttachmentResponse>>> listAttachments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapySession session = findOwned(id, principal);

        List<SessionAttachmentResponse> result = attachmentRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream().map(a -> SessionAttachmentResponse.from(
                        a, storageService.presign(a.getFileUrl(), Duration.ofHours(1)))).toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Delete attachment ──────────────────────────────────────────────────────

    @Operation(summary = "Delete a session attachment")
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        SessionAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (!att.getSessionId().equals(id) || !att.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        User caller = principal.getUser();
        if ((caller.getRole() == Role.THERAPIST || caller.getRole() == Role.DOCTOR)
                && !att.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own attachments");
        }

        storageService.delete(att.getFileUrl());
        attachmentRepository.delete(att);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private TherapySession findOwned(UUID id, UserPrincipal principal) {
        TherapySession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy session not found"));
        if (!session.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return session;
    }

    private void requireTherapistOwnership(TherapySession session, UserPrincipal principal) {
        User caller = principal.getUser();
        if ((caller.getRole() == Role.THERAPIST || caller.getRole() == Role.DOCTOR)
                && !session.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only modify your own sessions");
        }
    }

    private List<TherapySessionResponse> enrich(List<TherapySession> sessions) {
        if (sessions.isEmpty()) return List.of();

        Set<UUID> patientIds    = sessions.stream().map(TherapySession::getPatientId).collect(Collectors.toSet());
        Set<UUID> therapistIds  = sessions.stream().map(TherapySession::getTherapistId).collect(Collectors.toSet());
        Set<UUID> enrollmentIds = sessions.stream().map(TherapySession::getEnrollmentId).collect(Collectors.toSet());

        Map<UUID, Patient> patientMap   = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));
        Map<UUID, User>    therapistMap = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, Integer> totalSessionsMap = new HashMap<>();
        Map<UUID, String>  programNameMap   = new HashMap<>();

        // One count per plan rather than per session — the allowance is a plan-level figure.
        Map<UUID, Integer> parentReschedulesLeft = new HashMap<>();
        for (UUID eid : enrollmentIds) {
            int used = sessionRepository.countByEnrollmentIdAndParentRescheduleRequestedTrue(eid);
            parentReschedulesLeft.put(eid, Math.max(0, PARENT_RESCHEDULE_LIMIT - used));
        }

        for (UUID eid : enrollmentIds) {
            enrollmentRepository.findById(eid).ifPresent(enrollment ->
                subscriptionRepository.findById(enrollment.getSubscriptionId()).ifPresent(sub -> {
                    totalSessionsMap.put(eid, sub.getNumSessions());
                    programRepository.findById(sub.getProgramId()).ifPresent(prog ->
                            programNameMap.put(eid, prog.getName()));
                })
            );
        }

        return sessions.stream().map(s -> {
            Patient patient   = patientMap.get(s.getPatientId());
            User    therapist = therapistMap.get(s.getTherapistId());
            return TherapySessionResponse.from(
                    s,
                    patient   != null ? patient.getFirstName()   : "",
                    patient   != null ? patient.getLastName()     : "",
                    therapist != null ? therapist.getFirstName() : "",
                    therapist != null ? therapist.getLastName()  : "",
                    programNameMap.getOrDefault(s.getEnrollmentId(), "Unknown Program"),
                    totalSessionsMap.getOrDefault(s.getEnrollmentId(), 0),
                    parentReschedulesLeft.getOrDefault(s.getEnrollmentId(), PARENT_RESCHEDULE_LIMIT));
        }).toList();
    }
}
