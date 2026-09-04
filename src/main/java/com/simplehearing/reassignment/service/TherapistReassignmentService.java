package com.simplehearing.reassignment.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.enums.IEPPlanStatus;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.entity.TherapistPatient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.reassignment.dto.CreateReassignmentRequest;
import com.simplehearing.reassignment.dto.ReassignmentCaseSummary;
import com.simplehearing.reassignment.dto.ReassignmentResponse;
import com.simplehearing.reassignment.entity.TherapistReassignment;
import com.simplehearing.reassignment.entity.TherapistReassignmentCase;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;
import com.simplehearing.reassignment.repository.TherapistReassignmentCaseRepository;
import com.simplehearing.reassignment.repository.TherapistReassignmentRepository;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.RescheduleReason;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bulk-reassigns a therapist's selected cases to another therapist — permanently, or for a
 * window that hands back automatically. Generalizes {@code EnrollmentController.changeTherapist}
 * from one enrollment at a time to N selected patients, with an optional auto-reverting window.
 */
@Service
public class TherapistReassignmentService {

    private static final Logger log = LoggerFactory.getLogger(TherapistReassignmentService.class);

    private final TherapistReassignmentRepository reassignmentRepository;
    private final TherapistReassignmentCaseRepository reassignmentCaseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TherapySessionRepository therapySessionRepository;
    private final ReviewMeetingRepository reviewMeetingRepository;
    private final IEPPlanRepository iepPlanRepository;
    private final TherapistPatientRepository therapistPatientRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public TherapistReassignmentService(TherapistReassignmentRepository reassignmentRepository,
                                        TherapistReassignmentCaseRepository reassignmentCaseRepository,
                                        EnrollmentRepository enrollmentRepository,
                                        TherapySessionRepository therapySessionRepository,
                                        ReviewMeetingRepository reviewMeetingRepository,
                                        IEPPlanRepository iepPlanRepository,
                                        TherapistPatientRepository therapistPatientRepository,
                                        PatientRepository patientRepository,
                                        UserRepository userRepository) {
        this.reassignmentRepository = reassignmentRepository;
        this.reassignmentCaseRepository = reassignmentCaseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.therapySessionRepository = therapySessionRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.iepPlanRepository = iepPlanRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TherapistReassignment create(CreateReassignmentRequest request, UserPrincipal principal) {
        if (request.fromTherapistId().equals(request.toTherapistId())) {
            throw new ApiException(HttpStatus.CONFLICT, "That therapist already has these cases");
        }

        User toTherapist = userRepository.findById(request.toTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));
        if (!toTherapist.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Therapist belongs to another organisation");
        }
        if (!toTherapist.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "That therapist is deactivated");
        }
        if (!toTherapist.hasRole(Role.THERAPIST)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That user is not a therapist");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = request.startDate() != null ? request.startDate() : today;

        if (request.type() == ReassignmentType.TEMPORARY) {
            if (request.endDate() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A temporary reassignment needs an end date");
            }
            if (request.endDate().isBefore(startDate)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be on or after the start date");
            }
            if (!request.endDate().isAfter(today)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be in the future");
            }
        } else if (request.endDate() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A permanent reassignment cannot have an end date");
        }

        TherapistReassignment newBatch = new TherapistReassignment();
        newBatch.setOrgId(principal.getOrgId());
        newBatch.setFromTherapistId(request.fromTherapistId());
        newBatch.setToTherapistId(request.toTherapistId());
        newBatch.setType(request.type());
        newBatch.setStartDate(startDate);
        newBatch.setEndDate(request.endDate());
        newBatch.setReason(request.reason());
        newBatch.setCreatedBy(principal.getId());
        final TherapistReassignment batch = reassignmentRepository.save(newBatch);

        int movedSessions = 0;
        int movedMeetings = 0;
        int movedPlans = 0;

        for (UUID patientId : request.patientIds()) {
            TherapistPatient link = therapistPatientRepository
                    .findByPatientIdAndTherapistId(patientId, request.fromTherapistId())
                    .filter(TherapistPatient::isActive)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "One of the selected cases is not currently assigned to that therapist"));

            if (reassignmentCaseRepository.existsByPatientIdAndReassignmentStatus(patientId, ReassignmentStatus.ACTIVE)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "One of the selected cases already has an active reassignment in progress");
            }

            List<Enrollment> enrollments = enrollmentRepository.findByOrgIdAndPatientIdAndTherapistIdAndStatus(
                    principal.getOrgId(), patientId, request.fromTherapistId(), EnrollmentStatus.ACTIVE);

            UUID movedEnrollmentId = null;
            for (Enrollment enrollment : enrollments) {
                movedEnrollmentId = enrollment.getId();
                enrollment.setTherapistId(request.toTherapistId());
                enrollmentRepository.save(enrollment);

                for (TherapySession session : therapySessionRepository
                        .findByEnrollmentIdOrderBySessionNumberAsc(enrollment.getId())) {
                    boolean inWindow = !session.getSessionDate().isBefore(startDate)
                            && (request.type() == ReassignmentType.PERMANENT
                                || !session.getSessionDate().isAfter(request.endDate()));
                    // A session already flagged PENDING_RESCHEDULE because of the *old*
                    // therapist's leave is exactly what this reassignment is meant to resolve —
                    // the substitute keeps the original slot, so it un-flags back to SCHEDULED.
                    // Any other reschedule reason (a public holiday, a parent's own request)
                    // isn't fixed by a different therapist, so those stay flagged as they were.
                    boolean isLeaveFlagged = session.getStatus() == TherapySessionStatus.PENDING_RESCHEDULE
                            && session.getRescheduleReason() == RescheduleReason.THERAPIST_LEAVE;
                    if ((session.getStatus() == TherapySessionStatus.SCHEDULED || isLeaveFlagged) && inWindow) {
                        session.setTherapistId(request.toTherapistId());
                        session.setReassignmentId(batch.getId());
                        if (isLeaveFlagged) {
                            session.setStatus(TherapySessionStatus.SCHEDULED);
                            session.setRescheduleReason(null);
                            session.setRescheduleLeaveStartDate(null);
                            session.setRescheduleLeaveEndDate(null);
                        }
                        therapySessionRepository.save(session);
                        movedSessions++;
                    }
                }

                for (ReviewMeeting meeting : reviewMeetingRepository
                        .findByEnrollmentIdOrderByMeetingNumberAsc(enrollment.getId())) {
                    boolean inWindow = !meeting.getMeetingDate().isBefore(startDate)
                            && (request.type() == ReassignmentType.PERMANENT
                                || !meeting.getMeetingDate().isAfter(request.endDate()));
                    if (meeting.getStatus() == ReviewMeetingStatus.SCHEDULED && inWindow) {
                        meeting.setTherapistId(request.toTherapistId());
                        meeting.setReassignmentId(batch.getId());
                        reviewMeetingRepository.save(meeting);
                        movedMeetings++;
                    }
                }

                for (IEPPlan plan : iepPlanRepository.findByEnrollmentId(enrollment.getId())) {
                    if (plan.getStatus() == IEPPlanStatus.ACTIVE
                            && request.fromTherapistId().equals(plan.getTherapistId())) {
                        plan.setTherapistId(request.toTherapistId());
                        plan.setReassignmentId(batch.getId());
                        iepPlanRepository.save(plan);
                        movedPlans++;
                    }
                }
            }

            TherapistReassignmentCase caseRow = new TherapistReassignmentCase();
            caseRow.setReassignment(batch);
            caseRow.setPatientId(patientId);
            caseRow.setEnrollmentId(movedEnrollmentId);
            reassignmentCaseRepository.save(caseRow);

            // Caseload flip — deactivate the old link, activate/create the new one.
            link.setActive(false);
            link.setReassignmentId(batch.getId());
            therapistPatientRepository.save(link);

            therapistPatientRepository.findByPatientIdAndTherapistId(patientId, request.toTherapistId())
                    .ifPresentOrElse(existing -> {
                        if (!existing.isActive()) {
                            existing.setActive(true);
                            existing.setReassignmentId(batch.getId());
                            therapistPatientRepository.save(existing);
                        }
                        // Already active for some other reason — leave untouched, no marker.
                    }, () -> {
                        TherapistPatient newLink = new TherapistPatient();
                        newLink.setPatientId(patientId);
                        newLink.setTherapistId(request.toTherapistId());
                        newLink.setAssignedBy(principal.getId());
                        newLink.setReassignmentId(batch.getId());
                        therapistPatientRepository.save(newLink);
                    });
        }

        log.info("Reassignment {} moved {} case(s) from therapist {} to {} — {} session(s), "
                        + "{} review meeting(s), {} IEP plan(s)",
                batch.getId(), request.patientIds().size(), request.fromTherapistId(),
                request.toTherapistId(), movedSessions, movedMeetings, movedPlans);

        return batch;
    }

    /** Shared by the nightly job (automatic) and the early-cancel endpoint (manual). */
    @Transactional
    public TherapistReassignment revert(TherapistReassignment batch, boolean early, UUID revertedBy) {
        for (TherapySession session : therapySessionRepository.findByReassignmentId(batch.getId())) {
            if (session.getStatus() == TherapySessionStatus.SCHEDULED) {
                session.setTherapistId(batch.getFromTherapistId());
                session.setReassignmentId(null);
                therapySessionRepository.save(session);
            }
        }

        for (ReviewMeeting meeting : reviewMeetingRepository.findByReassignmentId(batch.getId())) {
            if (meeting.getStatus() == ReviewMeetingStatus.SCHEDULED) {
                meeting.setTherapistId(batch.getFromTherapistId());
                meeting.setReassignmentId(null);
                reviewMeetingRepository.save(meeting);
            }
        }

        for (IEPPlan plan : iepPlanRepository.findByReassignmentId(batch.getId())) {
            if (plan.getStatus() == IEPPlanStatus.ACTIVE) {
                plan.setTherapistId(batch.getFromTherapistId());
                plan.setReassignmentId(null);
                iepPlanRepository.save(plan);
            }
        }

        for (TherapistReassignmentCase caseRow : reassignmentCaseRepository.findByReassignment_Id(batch.getId())) {
            if (caseRow.getEnrollmentId() != null) {
                enrollmentRepository.findById(caseRow.getEnrollmentId()).ifPresent(enrollment -> {
                    if (batch.getToTherapistId().equals(enrollment.getTherapistId())) {
                        enrollment.setTherapistId(batch.getFromTherapistId());
                        enrollmentRepository.save(enrollment);
                    }
                });
            }

            therapistPatientRepository.findByPatientIdAndTherapistId(caseRow.getPatientId(), batch.getFromTherapistId())
                    .ifPresent(link -> {
                        if (!link.isActive()) {
                            link.setActive(true);
                            link.setReassignmentId(null);
                            therapistPatientRepository.save(link);
                        }
                    });
            therapistPatientRepository.findByPatientIdAndTherapistId(caseRow.getPatientId(), batch.getToTherapistId())
                    .ifPresent(link -> {
                        if (batch.getId().equals(link.getReassignmentId())) {
                            link.setActive(false);
                            link.setReassignmentId(null);
                            therapistPatientRepository.save(link);
                        }
                    });
        }

        batch.setStatus(early ? ReassignmentStatus.CANCELLED : ReassignmentStatus.REVERTED);
        batch.setRevertedAt(Instant.now());
        batch.setRevertedBy(early ? revertedBy : null);

        log.info("Reassignment {} reverted ({})", batch.getId(), early ? "early cancel" : "window ended");

        return reassignmentRepository.save(batch);
    }

    public List<TherapistReassignment> findForTherapist(UUID orgId, UUID therapistId) {
        return reassignmentRepository.findForTherapist(orgId, therapistId);
    }

    public ReassignmentResponse enrich(TherapistReassignment batch) {
        return enrich(List.of(batch)).get(0);
    }

    public List<ReassignmentResponse> enrich(List<TherapistReassignment> batches) {
        if (batches.isEmpty()) return List.of();

        Set<UUID> therapistIds = new HashSet<>();
        batches.forEach(b -> { therapistIds.add(b.getFromTherapistId()); therapistIds.add(b.getToTherapistId()); });
        Map<UUID, User> usersById = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UUID> batchIds = batches.stream().map(TherapistReassignment::getId).toList();
        Map<UUID, List<TherapistReassignmentCase>> casesByBatch = new HashMap<>();
        for (UUID id : batchIds) {
            casesByBatch.put(id, reassignmentCaseRepository.findByReassignment_Id(id));
        }

        Set<UUID> patientIds = casesByBatch.values().stream()
                .flatMap(List::stream).map(TherapistReassignmentCase::getPatientId)
                .collect(Collectors.toSet());
        Map<UUID, String> patientNames = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p.getFirstName() + " " + p.getLastName()));

        List<ReassignmentResponse> result = new ArrayList<>();
        for (TherapistReassignment batch : batches) {
            User from = usersById.get(batch.getFromTherapistId());
            User to = usersById.get(batch.getToTherapistId());
            List<ReassignmentCaseSummary> cases = casesByBatch.getOrDefault(batch.getId(), List.of()).stream()
                    .map(c -> new ReassignmentCaseSummary(
                            c.getPatientId(), patientNames.getOrDefault(c.getPatientId(), ""), c.getEnrollmentId()))
                    .toList();
            result.add(ReassignmentResponse.from(
                    batch,
                    from != null ? from.getFirstName() + " " + from.getLastName() : "",
                    to != null ? to.getFirstName() + " " + to.getLastName() : "",
                    cases));
        }
        return result;
    }
}
