package com.simplehearing.patient.service;

import com.simplehearing.appointment.repository.AppointmentRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.dto.PagedResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.condition.entity.Condition;
import com.simplehearing.condition.repository.ConditionRepository;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.iep.repository.IEPGoalProgressRepository;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.invitation.repository.InvitationRepository;
import com.simplehearing.invitation.service.InvitationService;
import com.simplehearing.patient.dto.*;
import com.simplehearing.patient.entity.*;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.*;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.repository.SessionAttachmentRepository;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.enums.SubscriptionStatus;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientConditionRepository patientConditionRepository;
    private final PatientParentRepository patientParentRepository;
    private final TherapistPatientRepository therapistPatientRepository;
    private final ConditionRepository conditionRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final AppointmentRepository appointmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TherapySessionRepository therapySessionRepository;
    private final SessionAttachmentRepository sessionAttachmentRepository;
    private final IEPPlanRepository iepPlanRepository;
    private final IEPGoalRepository iepGoalRepository;
    private final IEPGoalProgressRepository iepGoalProgressRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;

    public PatientService(PatientRepository patientRepository,
                          PatientConditionRepository patientConditionRepository,
                          PatientParentRepository patientParentRepository,
                          TherapistPatientRepository therapistPatientRepository,
                          ConditionRepository conditionRepository,
                          UserRepository userRepository,
                          ClinicRepository clinicRepository,
                          AppointmentRepository appointmentRepository,
                          SubscriptionRepository subscriptionRepository,
                          ProgramRepository programRepository,
                          EnrollmentRepository enrollmentRepository,
                          TherapySessionRepository therapySessionRepository,
                          SessionAttachmentRepository sessionAttachmentRepository,
                          IEPPlanRepository iepPlanRepository,
                          IEPGoalRepository iepGoalRepository,
                          IEPGoalProgressRepository iepGoalProgressRepository,
                          InvitationRepository invitationRepository,
                          InvitationService invitationService) {
        this.patientRepository = patientRepository;
        this.patientConditionRepository = patientConditionRepository;
        this.patientParentRepository = patientParentRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.conditionRepository = conditionRepository;
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
        this.appointmentRepository = appointmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.therapySessionRepository = therapySessionRepository;
        this.sessionAttachmentRepository = sessionAttachmentRepository;
        this.iepPlanRepository = iepPlanRepository;
        this.iepGoalRepository = iepGoalRepository;
        this.iepGoalProgressRepository = iepGoalProgressRepository;
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public PatientResponse create(CreatePatientRequest request, UserPrincipal principal) {
        clinicRepository.findByIdAndOrgId(request.clinicId(), principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found in your organisation"));

        Patient patient = new Patient();
        patient.setOrgId(principal.getOrgId());
        patient.setClinicId(request.clinicId());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName() == null ? "" : request.lastName().trim());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setNotes(request.notes());
        patientRepository.save(patient);

        return buildResponse(patient);
    }

    /**
     * Paginated, filtered Cases list. {@code status} is a comma-separated subset of
     * ACTIVE/NOT_INVITED/INACTIVE (matching the UI's filter pills); omitted or blank defaults to
     * ACTIVE+NOT_INVITED, and an explicitly empty selection shows every status (mirrors the
     * frontend's prior client-side behaviour, where clearing all pills showed everything rather
     * than nothing). A THERAPIST is always scoped to their own assigned patients regardless of
     * {@code mine} — that param only matters for admin-tier roles filtering to their own caseload.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PatientResponse> listForOrg(String search, boolean mine, String status,
                                                       Pageable pageable, UserPrincipal principal) {
        Role role = principal.getUser().getRole();
        boolean onlyMine = mine || role == Role.THERAPIST;

        String q = (search == null || search.isBlank()) ? "" : search.trim();

        Set<String> statuses = status == null
                ? Set.of("ACTIVE", "NOT_INVITED")
                : Arrays.stream(status.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
        // An explicit but empty selection (every pill toggled off) shows everything, not nothing.
        boolean anyStatus = statuses.isEmpty();

        Page<Patient> page = patientRepository.search(
                principal.getOrgId(), q, onlyMine, principal.getId(),
                anyStatus || statuses.contains("ACTIVE"),
                anyStatus || statuses.contains("NOT_INVITED"),
                anyStatus || statuses.contains("INACTIVE"),
                pageable);

        return PagedResponse.from(page, this::buildResponse);
    }

    /** Returns patients where the calling user is a linked parent. */
    @Transactional(readOnly = true)
    public List<PatientResponse> listMyChildren(UserPrincipal principal) {
        List<PatientParent> links = patientParentRepository.findById_ParentId(principal.getId());
        List<UUID> patientIds = links.stream().map(pp -> pp.getId().getPatientId()).toList();
        return patientIds.isEmpty() ? List.of()
                : patientRepository.findAllById(patientIds).stream()
                        .map(this::buildResponse)
                        .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse get(UUID patientId, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());
        return buildResponse(patient);
    }

    public PatientResponse update(UUID patientId, CreatePatientRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        if (request.firstName() != null)   patient.setFirstName(request.firstName());
        if (request.lastName() != null)    patient.setLastName(request.lastName());
        if (request.dateOfBirth() != null) patient.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null)      patient.setGender(request.gender());
        if (request.notes() != null)       patient.setNotes(request.notes());

        return buildResponse(patientRepository.save(patient));
    }

    public PatientResponse updateStage(UUID patientId, UpdatePatientStageRequest request, UserPrincipal principal) {
        if (request.stage() == PatientStage.DISCHARGED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Use POST /patients/{id}/discharge to discharge a patient — it snapshots the episode's success criteria");
        }
        Patient patient = findPatient(patientId, principal.getOrgId());
        patient.setStage(request.stage());
        return buildResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<UpcomingBirthdayResponse> upcomingBirthdays(UserPrincipal principal) {
        Role role = principal.getUser().getRole();
        List<Patient> patients;

        if (role == Role.THERAPIST) {
            List<UUID> patientIds = therapistPatientRepository
                    .findByTherapistIdAndIsActive(principal.getId(), true)
                    .stream().map(TherapistPatient::getPatientId).toList();
            if (patientIds.isEmpty()) return List.of();
            patients = patientRepository.findAllById(patientIds).stream()
                    .filter(p -> p.getOrgId().equals(principal.getOrgId()) && p.getDateOfBirth() != null)
                    .toList();
        } else {
            patients = patientRepository.findByOrgId(principal.getOrgId()).stream()
                    .filter(p -> p.getDateOfBirth() != null)
                    .toList();
        }

        LocalDate today    = LocalDate.now();
        LocalDate cutoff   = today.plusDays(30);

        return patients.stream()
                .map(p -> {
                    MonthDay bday        = MonthDay.from(p.getDateOfBirth());
                    LocalDate nextBirthday = bday.atYear(today.getYear());
                    if (nextBirthday.isBefore(today)) {
                        nextBirthday = bday.atYear(today.getYear() + 1);
                    }
                    return new AbstractMap.SimpleEntry<>(p, nextBirthday);
                })
                .filter(e -> !e.getValue().isAfter(cutoff))
                .sorted(Map.Entry.comparingByValue())
                .map(e -> {
                    Patient p    = e.getKey();
                    int daysUntil = (int) ChronoUnit.DAYS.between(today, e.getValue());
                    return new UpcomingBirthdayResponse(
                            p.getId(), p.getFirstName(), p.getLastName(),
                            p.getDateOfBirth(), daysUntil);
                })
                .toList();
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    public PatientResponse addCondition(UUID patientId, AddConditionRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        conditionRepository.findById(request.conditionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Condition not found"));

        PatientCondition pc = new PatientCondition(patientId, request.conditionId());
        pc.setDiagnosedAt(request.diagnosedAt());
        pc.setNotes(request.notes());
        patientConditionRepository.save(pc);

        return buildResponse(patient);
    }

    public void removeCondition(UUID patientId, UUID conditionId, UserPrincipal principal) {
        findPatient(patientId, principal.getOrgId());
        patientConditionRepository.deleteById_PatientIdAndId_ConditionId(patientId, conditionId);
    }

    // ── Parents ───────────────────────────────────────────────────────────────

    public PatientResponse linkParent(UUID patientId, LinkParentRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        User parent = userRepository.findById(request.parentId())
                .filter(u -> u.getOrgId().equals(principal.getOrgId()) && u.hasRole(Role.PARENT))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Parent user not found in your organisation (must have PARENT role)"));

        PatientParent link = new PatientParent(patientId, parent.getId());
        patientParentRepository.save(link);

        return buildResponse(patient);
    }

    public void unlinkParent(UUID patientId, UUID parentId, UserPrincipal principal) {
        findPatient(patientId, principal.getOrgId());
        patientParentRepository.deleteById_PatientIdAndId_ParentId(patientId, parentId);
    }

    /** Invites someone who doesn't have an account yet; they're auto-linked as this patient's parent on accept. */
    public InviteParentResponse inviteParent(UUID patientId, InviteParentRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        String link = invitationService.createLinkedInvitation(
                request.email(), Role.PARENT, patient.getClinicId(), patient.getId(),
                principal.getOrgId(), principal.getId());

        return new InviteParentResponse(link);
    }

    // ── Therapist assignments ─────────────────────────────────────────────────

    public PatientResponse assignTherapist(UUID patientId, AssignTherapistRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        User therapist = userRepository.findById(request.therapistId())
                .filter(u -> u.getOrgId().equals(principal.getOrgId()) && u.hasRole(Role.THERAPIST))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Therapist not found in your organisation (must have THERAPIST role)"));

        therapistPatientRepository.findByPatientIdAndTherapistId(patientId, therapist.getId())
                .ifPresent(existing -> {
                    if (existing.isActive()) {
                        throw new ApiException(HttpStatus.CONFLICT, "Therapist is already assigned to this patient");
                    }
                    existing.setActive(true);
                    // A manual assign always wins over a stale reassignment marker.
                    existing.setReassignmentId(null);
                    therapistPatientRepository.save(existing);
                });

        if (therapistPatientRepository.findByPatientIdAndTherapistId(patientId, therapist.getId()).isEmpty()) {
            TherapistPatient assignment = new TherapistPatient();
            assignment.setPatientId(patientId);
            assignment.setTherapistId(therapist.getId());
            assignment.setAssignedBy(principal.getId());
            therapistPatientRepository.save(assignment);
        }

        return buildResponse(patient);
    }

    public void unassignTherapist(UUID patientId, UUID therapistId, UserPrincipal principal) {
        findPatient(patientId, principal.getOrgId());
        therapistPatientRepository.findByPatientIdAndTherapistId(patientId, therapistId)
                .ifPresent(tp -> {
                    tp.setActive(false);
                    // A manual unassign always wins over a stale reassignment marker.
                    tp.setReassignmentId(null);
                    therapistPatientRepository.save(tp);
                });
    }

    public void delete(UUID patientId, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());
        UUID id = patient.getId();

        // IEP: progress → goals → plans
        iepPlanRepository.findByPatientId(id).forEach(plan -> {
            iepGoalRepository.findByPlanId(plan.getId())
                    .forEach(goal -> iepGoalProgressRepository.deleteByGoalId(goal.getId()));
            iepGoalRepository.deleteByPlanId(plan.getId());
        });
        iepPlanRepository.deleteByPatientId(id);

        // Sessions + attachments (attachments must go first)
        therapySessionRepository.findByPatientId(id)
                .forEach(s -> sessionAttachmentRepository.deleteBySessionId(s.getId()));
        // Enrollments must be deleted before sessions (sessions reference enrollments)
        // Actually sessions reference enrollment_id, enrollments reference patient_id
        // Delete sessions first, then enrollments
        therapySessionRepository.findByPatientId(id)
                .forEach(therapySessionRepository::delete);
        enrollmentRepository.deleteByPatientId(id);

        // Subscriptions
        subscriptionRepository.deleteByPatientId(id);

        // Appointments
        appointmentRepository.deleteByPatientId(id);

        // Nullify patient_id on invitations (nullable FK)
        invitationRepository.findByPatientId(id)
                .forEach(inv -> { inv.setPatientId(null); invitationRepository.save(inv); });

        // Join tables (conditions, parents, therapist assignments)
        patientConditionRepository.deleteById_PatientId(id);
        patientParentRepository.deleteById_PatientId(id);
        therapistPatientRepository.deleteByPatientId(id);

        patientRepository.delete(patient);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Patient findPatient(UUID patientId, UUID orgId) {
        return patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private PatientResponse buildResponse(Patient patient) {
        List<PatientCondition> pcs = patientConditionRepository.findById_PatientId(patient.getId());
        List<UUID> conditionIds = pcs.stream().map(pc -> pc.getId().getConditionId()).toList();
        List<Condition> conditionDetails = conditionIds.isEmpty()
                ? List.of()
                : conditionRepository.findAllById(conditionIds);

        List<PatientParent> pps = patientParentRepository.findById_PatientId(patient.getId());
        List<UUID> parentIds = pps.stream().map(pp -> pp.getId().getParentId()).toList();
        List<User> parents = parentIds.isEmpty() ? List.of() : userRepository.findAllById(parentIds);

        List<TherapistPatient> assignments = therapistPatientRepository
                .findByPatientIdAndIsActive(patient.getId(), true);
        List<UUID> therapistIds = assignments.stream().map(TherapistPatient::getTherapistId).toList();
        List<User> therapists = therapistIds.isEmpty() ? List.of() : userRepository.findAllById(therapistIds);

        List<UUID> activeProgramIds = subscriptionRepository
                .findByOrgIdAndPatientIdOrderByCreatedAtDesc(patient.getOrgId(), patient.getId())
                .stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(Subscription::getProgramId)
                .distinct()
                .toList();
        List<PatientResponse.TherapySummary> therapySummaries = activeProgramIds.isEmpty()
                ? List.of()
                : programRepository.findAllById(activeProgramIds).stream()
                        .map(p -> new PatientResponse.TherapySummary(p.getId(), p.getName()))
                        .toList();

        return PatientResponse.from(patient, pcs, conditionDetails, parents, assignments, therapists, therapySummaries);
    }
}
