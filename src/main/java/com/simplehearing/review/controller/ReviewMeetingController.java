package com.simplehearing.review.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.dto.ParticipantResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.review.dto.*;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.review.service.ReviewMeetingService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Review Meetings", description = "Feedback and review meetings for therapy plans")
@RestController
@RequestMapping("/api/v1/review-meetings")
public class ReviewMeetingController {

    private final ReviewMeetingRepository meetingRepository;
    private final ReviewMeetingService meetingService;
    private final EnrollmentRepository enrollmentRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;

    public ReviewMeetingController(ReviewMeetingRepository meetingRepository,
                                   ReviewMeetingService meetingService,
                                   EnrollmentRepository enrollmentRepository,
                                   PatientRepository patientRepository,
                                   PatientParentRepository patientParentRepository,
                                   UserRepository userRepository) {
        this.meetingRepository = meetingRepository;
        this.meetingService = meetingService;
        this.enrollmentRepository = enrollmentRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @Operation(summary = "List review meetings",
               description = "Filter by enrollment or patient. Parents only ever see meetings for their own children.")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ReviewMeetingResponse>>> list(
            @RequestParam(required = false) UUID enrollmentId,
            @RequestParam(required = false) UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ReviewMeeting> meetings;
        if (enrollmentId != null) {
            meetings = meetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(enrollmentId);
        } else if (patientId != null) {
            meetings = meetingRepository.findByOrgIdAndPatientIdOrderByMeetingDateAsc(principal.getOrgId(), patientId);
        } else if (isManager(principal) || isOfficeAdmin(principal)) {
            // Unfiltered: admins (and Office Admin, who schedules but never sees feedback
            // content — see enrich()) get the whole organisation — this is what the calendar asks for
            meetings = meetingRepository.findByOrgIdOrderByMeetingDateAsc(principal.getOrgId());
        } else if (isParent(principal)) {
            meetings = meetingRepository.findForParent(principal.getOrgId(), principal.getId());
        } else {
            // Includes THERAPIST: review meetings are between the Clinic Head and the parent —
            // the therapist is deliberately not a participant, so they get nothing here, even
            // for their own caseload. therapistId on the entity is attribution-only (see
            // AnalyticsService), never a visibility grant.
            meetings = List.of();
        }

        meetings = meetings.stream()
                .filter(m -> m.getOrgId().equals(principal.getOrgId()))
                .filter(m -> canView(m, principal))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(enrich(meetings, principal)));
    }

    @Operation(summary = "Get a single review meeting")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findViewable(id, principal);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(meeting), principal).get(0)));
    }

    // ── Schedule ─────────────────────────────────────────────────────────────

    @Operation(summary = "Add a review meeting to an existing therapy plan",
               description = "Invites the patient's linked parents plus the given Clinic Head(s) — "
                           + "the therapist is not a participant under this model.")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> create(
            @Valid @RequestBody CreateReviewMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(request.enrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Set<UUID> clinicHeadIds = meetingService.requireClinicHeads(request.participantIds(), principal);

        ReviewMeeting saved = meetingService.createSingle(
                enrollment, request.meetingDate(), request.startTime(),
                request.durationMinutes(), clinicHeadIds, principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Generate a recurring review schedule for an existing plan",
               description = "Meetings repeat on the given interval until the end date, skipping public "
                           + "holidays. Invites the patient's linked parents plus the given Clinic Head(s).")
    @PostMapping("/schedule/{enrollmentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ReviewMeetingResponse>>> generateSchedule(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody ReviewScheduleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Guard against a double submit quietly creating a second set of meetings
        if (meetingRepository.countByEnrollmentId(enrollmentId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This plan already has review meetings. Cancel them first, or add a single meeting instead.");
        }

        Set<UUID> clinicHeadIds = meetingService.requireClinicHeads(request.participantIds(), principal);

        List<ReviewMeeting> created = meetingService.generateForEnrollment(
                enrollment, request, clinicHeadIds, principal.getId());

        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No meetings could be scheduled — check the plan has an end date and the dates leave room for at least one meeting");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(created, principal)));
    }

    @Operation(summary = "Edit a review meeting's participants",
               description = "Full replacement of the attendee list — Admin Roles only, not restricted "
                           + "to Clinic Heads (unlike the picker shown at scheduling time).")
    @PatchMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> updateParticipants(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewParticipantsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);
        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting was cancelled — schedule a new one instead");
        }

        Set<UUID> ids = new LinkedHashSet<>(request.participantIds());
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more participants could not be found");
        }
        boolean foreign = users.stream().anyMatch(u -> !principal.getOrgId().equals(u.getOrgId()));
        if (foreign) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Participants must belong to your organisation");
        }

        ReviewMeeting saved = meetingService.updateParticipants(meeting, ids);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Reschedule a review meeting",
               description = "Resends the calendar invite so attendees' calendars move with it.")
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);

        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting was cancelled — schedule a new one instead");
        }

        ReviewMeeting saved = meetingService.reschedule(
                meeting, request.meetingDate(), request.startTime(), request.durationMinutes());

        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Cancel a review meeting")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);
        String reason = body != null ? body.get("reason") : null;

        ReviewMeeting saved = meetingService.cancel(meeting, reason);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Mark a review meeting as completed")
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);

        if (isClinician(principal) && !meeting.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your meeting");
        }
        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "A cancelled meeting cannot be completed");
        }

        meeting.setStatus(ReviewMeetingStatus.COMPLETED);
        ReviewMeeting saved = meetingRepository.save(meeting);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    @Operation(summary = "Parent submits feedback about the therapist",
               description = "One submission per meeting — resubmitting overwrites the previous answer.")
    @PutMapping("/{id}/parent-feedback")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> submitParentFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody ParentFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);

        if (!isLinkedParent(meeting.getPatientId(), principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
        }
        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting was cancelled");
        }

        boolean firstTime = !meeting.hasParentFeedback();

        meeting.setCommunicationRating(request.communicationRating());
        meeting.setProgressRatingPct(request.progressRatingPct());
        meeting.setParentComments(request.comments());
        meeting.setParentFeedbackBy(principal.getId());
        meeting.setParentFeedbackAt(Instant.now());

        ReviewMeeting saved = meetingRepository.save(meeting);
        if (firstTime) {
            meetingService.notifyFeedbackSubmitted(saved, true);
        }

        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Clinic Head (or Business Owner) writes confidential remarks on the period under review",
               description = "Admin-only note — never visible to the Therapist or Parent, even for a Clinic "
                           + "Head/Business Owner writing about their own work as the treating therapist.")
    @PutMapping("/{id}/clinic-head-remarks")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> updateClinicHeadRemarks(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicHeadRemarksRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);
        if (isSelfReview(meeting, principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can't write remarks on your own review");
        }
        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting was cancelled");
        }

        meeting.setClinicHeadRemarks(request.remarks());
        meeting.setClinicHeadRemarksAt(Instant.now());
        meeting.setClinicHeadRemarksBy(principal.getId());

        ReviewMeeting saved = meetingRepository.save(meeting);
        // Deliberately no notification — this is an internal admin note, and notifying the
        // treating therapist would defeat the point of it being confidential from them.
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ReviewMeeting findInOrg(UUID id, UserPrincipal principal) {
        ReviewMeeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review meeting not found"));
        if (!meeting.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return meeting;
    }

    private ReviewMeeting findViewable(UUID id, UserPrincipal principal) {
        ReviewMeeting meeting = findInOrg(id, principal);
        if (!canView(meeting, principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return meeting;
    }

    /**
     * Admins and Office Admin see everything, EXCEPT a meeting reviewing their own work as
     * the treating therapist (isSelfReview — see below); therapists see none of their own
     * meetings either way; parents always see their own linked child's meeting.
     *
     * The parent check runs first and short-circuits the self-review exclusion: a staff
     * member who is also the parent of the patient being reviewed (staff can be parents too,
     * including of a patient they themselves treat) must still be able to see and edit their
     * own parent feedback regardless of their other role(s) — self-review only ever blocks
     * the *staff* side of the view (Clinic Head Remarks, and general admin visibility into a
     * review of their own work), never a person's standing as that patient's parent.
     */
    private boolean canView(ReviewMeeting meeting, UserPrincipal principal) {
        if (isParent(principal) && isLinkedParent(meeting.getPatientId(), principal.getId())) return true;
        if (isSelfReview(meeting, principal)) return false;
        if (isManager(principal) || isOfficeAdmin(principal)) return true;
        // THERAPIST (with no other qualifying role) falls through to false — see list() above.
        return false;
    }

    /**
     * True when the viewer is personally the therapist this meeting is reviewing — a Clinic
     * Head or Business Owner is otherwise treated as staff and sees everything, but must not
     * see (or even know about) a review of their own work just because they also happen to
     * hold an Admin role. Only ever narrows the staff/admin view (see canView) — never
     * overrides a person's access as the patient's parent.
     */
    private boolean isSelfReview(ReviewMeeting meeting, UserPrincipal principal) {
        return meeting.getTherapistId().equals(principal.getId());
    }

    private boolean isLinkedParent(UUID patientId, UUID parentId) {
        return patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(parentId));
    }

    private static boolean isManager(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.BUSINESS_OWNER)
            || principal.getUser().hasRole(Role.CLINIC_HEAD);
    }

    private static boolean isClinician(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.THERAPIST);
    }

    private static boolean isParent(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.PARENT);
    }

    private static boolean isOfficeAdmin(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.OFFICE_ADMIN);
    }

    /**
     * Fills in patient/therapist/remarks-author names, and decides what each side of
     * confidential content the viewer is allowed to read.
     *
     * A parent only ever sees their own feedback (to review/edit it). Clinic Head Remarks
     * are Admin-only (BUSINESS_OWNER/CLINIC_HEAD, never OFFICE_ADMIN) and never shown to a
     * Therapist or Parent — including an Admin viewer who is themselves the treating
     * therapist on this meeting (isSelfReview). Parent ratings are still folded into that
     * therapist's analytics (aggregate, staff-only) so the signal isn't lost.
     */
    private List<ReviewMeetingResponse> enrich(List<ReviewMeeting> meetings, UserPrincipal principal) {
        if (meetings.isEmpty()) return List.of();

        Set<UUID> patientIds = meetings.stream().map(ReviewMeeting::getPatientId).collect(Collectors.toSet());
        Set<UUID> therapistIds = meetings.stream().map(ReviewMeeting::getTherapistId).collect(Collectors.toSet());

        Map<UUID, String> patientNames = new HashMap<>();
        patientRepository.findAllById(patientIds).forEach(p ->
                patientNames.put(p.getId(), p.getFirstName() + " " + p.getLastName()));

        Map<UUID, String> therapistNames = new HashMap<>();
        userRepository.findAllById(therapistIds).forEach(u ->
                therapistNames.put(u.getId(), u.getFirstName() + " " + u.getLastName()));

        // A review meeting's attendees are now explicit — the patient's linked parents plus
        // whichever Clinic Head(s) were chosen at scheduling time. The assigned therapist is
        // not a participant under this model; therapistId is kept purely for attribution.
        Set<UUID> allParticipantIds = meetings.stream()
                .flatMap(m -> m.getParticipantIds().stream())
                .collect(Collectors.toSet());
        Map<UUID, User> participantsById = new HashMap<>();
        userRepository.findAllById(allParticipantIds).forEach(u -> participantsById.put(u.getId(), u));

        Set<UUID> remarksAuthorIds = meetings.stream()
                .map(ReviewMeeting::getClinicHeadRemarksBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> remarksAuthorNames = new HashMap<>();
        userRepository.findAllById(remarksAuthorIds).forEach(u ->
                remarksAuthorNames.put(u.getId(), u.getFirstName() + " " + u.getLastName()));

        boolean staff = isManager(principal);
        // Batched, not a per-meeting isLinkedParent() query — and deliberately checked against
        // each meeting's actual patient rather than a bare isParent(principal) role check: an
        // Office Admin (or any non-CLINIC_HEAD/BUSINESS_OWNER staff) who happens to be a parent
        // of some unrelated patient must not see that OTHER patient's feedback just because they
        // hold the PARENT role somewhere in the org.
        Set<UUID> linkedPatientIds = isParent(principal)
                ? patientParentRepository.findById_PatientIdIn(patientIds).stream()
                        .filter(pp -> pp.getId().getParentId().equals(principal.getId()))
                        .map(pp -> pp.getId().getPatientId())
                        .collect(Collectors.toSet())
                : Set.of();

        return meetings.stream().map(m -> {
            // Admin (Clinic Head/Business Owner) sees both sides — except never Clinic Head
            // Remarks on their own work as the treating therapist (isSelfReview), where they're
            // otherwise excluded from the meeting entirely upstream (list()/findViewable()) but
            // enrich() is also called directly from create/update/reschedule/etc. on a meeting
            // the caller isn't necessarily the therapist for, so the guard is repeated here too.
            boolean seeParentSide = staff || linkedPatientIds.contains(m.getPatientId());
            boolean seeClinicHeadRemarks = staff && !isSelfReview(m, principal);

            List<ParticipantResponse> participants = m.getParticipantIds().stream()
                    .map(participantsById::get)
                    .filter(Objects::nonNull)
                    .map(u -> ParticipantResponse.from(u, false))
                    .toList();

            String remarksByName = remarksAuthorNames.get(m.getClinicHeadRemarksBy());

            return ReviewMeetingResponse.from(
                    m,
                    patientNames.getOrDefault(m.getPatientId(), ""),
                    therapistNames.getOrDefault(m.getTherapistId(), ""),
                    remarksByName,
                    participants,
                    seeParentSide,
                    seeClinicHeadRemarks);
        }).toList();
    }
}
