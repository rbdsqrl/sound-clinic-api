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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        } else if (isClinician(principal)) {
            meetings = meetingRepository.findByOrgIdAndTherapistIdOrderByMeetingDateAsc(
                    principal.getOrgId(), principal.getId());
        } else {
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

    @Operation(summary = "Add a review meeting to an existing therapy plan")
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

        ReviewMeeting saved = meetingService.createSingle(
                enrollment, request.meetingDate(), request.startTime(),
                request.durationMinutes(), principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(List.of(saved), principal).get(0)));
    }

    @Operation(summary = "Generate a recurring review schedule for an existing plan",
               description = "Meetings repeat on the given interval until the end date, skipping public holidays.")
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

        List<ReviewMeeting> created = meetingService.generateForEnrollment(
                enrollment, request, principal.getId());

        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No meetings could be scheduled — check the plan has an end date and the dates leave room for at least one meeting");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(created, principal)));
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

    @Operation(summary = "Therapist shares feedback on the sessions under review")
    @PutMapping("/{id}/therapist-feedback")
    @PreAuthorize("hasAnyRole('THERAPIST', 'CLINIC_HEAD', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<ReviewMeetingResponse>> submitTherapistFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody TherapistFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ReviewMeeting meeting = findInOrg(id, principal);

        if (isClinician(principal) && !meeting.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your meeting");
        }
        if (meeting.getStatus() == ReviewMeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting was cancelled");
        }

        boolean firstTime = !meeting.hasTherapistFeedback();

        meeting.setTherapistSummary(request.summary());
        meeting.setTherapistProgressNotes(request.progressNotes());
        meeting.setTherapistFeedbackAt(Instant.now());

        ReviewMeeting saved = meetingRepository.save(meeting);
        if (firstTime) {
            meetingService.notifyFeedbackSubmitted(saved, false);
        }

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
     * Admins and Office Admin see everything (Office Admin schedules these meetings but
     * never sees feedback content — that's decided separately in enrich()); therapists see
     * their own meetings; parents their own children's.
     */
    private boolean canView(ReviewMeeting meeting, UserPrincipal principal) {
        if (isManager(principal) || isOfficeAdmin(principal)) return true;
        if (isClinician(principal)) return meeting.getTherapistId().equals(principal.getId());
        if (isParent(principal)) return isLinkedParent(meeting.getPatientId(), principal.getId());
        return false;
    }

    private boolean isLinkedParent(UUID patientId, UUID parentId) {
        return patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(parentId));
    }

    private static boolean isManager(UserPrincipal principal) {
        return principal.getUser().hasRole(Role.BUSINESS_OWNER)
            || principal.getUser().hasRole(Role.CLINIC_HEAD)
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
     * Fills in patient and therapist names, and decides which half of the feedback the
     * viewer is allowed to read.
     *
     * Review feedback is confidential between the submitter and staff: a parent only ever
     * sees their own answer (to review/edit it), never the therapist's; a therapist only
     * ever sees their own, never the parent's. Only BUSINESS_OWNER/CLINIC_HEAD see both
     * sides. Parent ratings are still folded into that therapist's analytics (aggregate,
     * staff-only) so the signal isn't lost.
     */
    private List<ReviewMeetingResponse> enrich(List<ReviewMeeting> meetings, UserPrincipal principal) {
        if (meetings.isEmpty()) return List.of();

        Set<UUID> patientIds = meetings.stream().map(ReviewMeeting::getPatientId).collect(Collectors.toSet());
        Set<UUID> therapistIds = meetings.stream().map(ReviewMeeting::getTherapistId).collect(Collectors.toSet());

        Map<UUID, String> patientNames = new HashMap<>();
        patientRepository.findAllById(patientIds).forEach(p ->
                patientNames.put(p.getId(), p.getFirstName() + " " + p.getLastName()));

        Map<UUID, String> therapistNames = new HashMap<>();
        Map<UUID, User> therapistsById = new HashMap<>();
        userRepository.findAllById(therapistIds).forEach(u -> {
            therapistNames.put(u.getId(), u.getFirstName() + " " + u.getLastName());
            therapistsById.put(u.getId(), u);
        });

        // A review meeting's attendees are implicit: the assigned therapist plus every
        // parent linked to the patient — the same people the invite emails go to.
        Map<UUID, List<UUID>> parentIdsByPatient = new HashMap<>();
        for (UUID patientId : patientIds) {
            parentIdsByPatient.put(patientId,
                    patientParentRepository.findById_PatientId(patientId).stream()
                            .map(pp -> pp.getId().getParentId())
                            .toList());
        }
        Set<UUID> allParentIds = parentIdsByPatient.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        Map<UUID, User> parentsById = new HashMap<>();
        userRepository.findAllById(allParentIds).forEach(u -> parentsById.put(u.getId(), u));

        boolean staff = isManager(principal);
        boolean clinician = isClinician(principal);
        boolean parent = isParent(principal);

        return meetings.stream().map(m -> {
            boolean seeParentSide;
            boolean seeTherapistSide;

            if (staff) {
                seeParentSide = true;
                seeTherapistSide = true;
            } else if (clinician) {
                seeTherapistSide = true;   // their own answer, to review/edit it
                seeParentSide = false;     // never the parent's
            } else if (parent) {
                seeParentSide = true;      // their own answer, to review/edit it
                seeTherapistSide = false;  // never the therapist's
            } else {
                seeParentSide = false;
                seeTherapistSide = false;
            }

            List<ParticipantResponse> participants = new ArrayList<>();
            User therapist = therapistsById.get(m.getTherapistId());
            if (therapist != null) {
                participants.add(ParticipantResponse.from(therapist, true));
            }
            for (UUID parentId : parentIdsByPatient.getOrDefault(m.getPatientId(), List.of())) {
                User parentUser = parentsById.get(parentId);
                if (parentUser != null) participants.add(ParticipantResponse.from(parentUser, false));
            }

            return ReviewMeetingResponse.from(
                    m,
                    patientNames.getOrDefault(m.getPatientId(), ""),
                    therapistNames.getOrDefault(m.getTherapistId(), ""),
                    participants,
                    seeParentSide,
                    seeTherapistSide);
        }).toList();
    }
}
