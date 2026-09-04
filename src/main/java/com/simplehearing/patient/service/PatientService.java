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
import com.simplehearing.program.entity.Program;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
     * ACTIVE/INACTIVE (matching the UI's two filter pills) — a case is Active until discharged,
     * Inactive means stage = DISCHARGED. Omitted or blank defaults to ACTIVE only, and an
     * explicitly empty selection shows every status (mirrors the frontend's prior client-side
     * behaviour, where clearing all pills showed everything rather than nothing). A THERAPIST is
     * always scoped to their own assigned patients regardless of {@code mine} — that param only
     * matters for admin-tier roles filtering to their own caseload.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PatientResponse> listForOrg(String search, boolean mine, String status, boolean compact,
                                                       Pageable pageable, UserPrincipal principal) {
        Role role = principal.getUser().getRole();
        boolean onlyMine = mine || role == Role.THERAPIST;

        String q = (search == null || search.isBlank()) ? "" : search.trim();

        Set<String> statuses = status == null
                ? Set.of("ACTIVE")
                : Arrays.stream(status.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
        // An explicit but empty selection (every pill toggled off) shows everything, not nothing.
        boolean anyStatus = statuses.isEmpty();

        // Fast path: no search, no caseload scoping, no status narrowing — this is a plain
        // "every patient in the org" request (the shape patientsApi.list() sends for pickers and
        // dashboards). Skip the filtered query entirely rather than running the caseload subquery
        // the Cases page's status pills need but this request doesn't.
        Page<Patient> page = (q.isEmpty() && !onlyMine && anyStatus)
                ? patientRepository.findByOrgId(principal.getOrgId(), pageable)
                : patientRepository.search(
                        principal.getOrgId(), q, onlyMine, principal.getId(),
                        anyStatus || statuses.contains("ACTIVE"),
                        anyStatus || statuses.contains("INACTIVE"),
                        pageable);

        List<PatientResponse> content = buildResponses(page.getContent(), compact);
        return new PagedResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Returns patients where the calling user is a linked parent. */
    @Transactional(readOnly = true)
    public List<PatientResponse> listMyChildren(UserPrincipal principal) {
        List<PatientParent> links = patientParentRepository.findById_ParentId(principal.getId());
        List<UUID> patientIds = links.stream().map(pp -> pp.getId().getPatientId()).toList();
        return buildResponses(patientIds.isEmpty() ? List.of() : patientRepository.findAllById(patientIds), false);
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

    /** Invites someone who doesn't have an account yet; they're auto-linked as this patient's
     *  parent on accept. If the email already belongs to an active account in this org (e.g. a
     *  staff Member without the Parent role), no invite is sent — the response carries that
     *  user's summary instead, for the caller to confirm before linkExistingUserAsParent. */
    public InviteParentResponse inviteParent(UUID patientId, InviteParentRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        Optional<User> existing = userRepository.findByEmail(request.email())
                .filter(u -> u.isActive() && u.getOrgId().equals(principal.getOrgId()));

        if (existing.isPresent()) {
            User u = existing.get();
            return InviteParentResponse.existingUser(
                    new InviteParentResponse.ExistingUserSummary(u.getId(), u.getFirstName(), u.getLastName(), u.getRole()));
        }

        String link = invitationService.createLinkedInvitation(
                request.email(), Role.PARENT, patient.getClinicId(), patient.getId(),
                principal.getOrgId(), principal.getId());

        return InviteParentResponse.invited(link);
    }

    /** Grants an existing org member Parent access to a patient — adds PARENT to their
     *  additionalRoles if they don't already have it (their primary role, e.g. THERAPIST, is
     *  untouched), then links them the same way linkParent does. Reached only after
     *  inviteParent's existingUser response has been confirmed by the caller. */
    public PatientResponse linkExistingUserAsParent(UUID patientId, LinkParentRequest request, UserPrincipal principal) {
        Patient patient = findPatient(patientId, principal.getOrgId());

        User user = userRepository.findById(request.parentId())
                .filter(u -> u.getOrgId().equals(principal.getOrgId()) && u.isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found in your organisation"));

        boolean alreadyLinked = patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(user.getId()));
        if (alreadyLinked) {
            throw new ApiException(HttpStatus.CONFLICT, "This person is already linked as a parent of this case");
        }

        if (!user.hasRole(Role.PARENT)) {
            Set<Role> roles = new HashSet<>(user.getAdditionalRoles());
            roles.add(Role.PARENT);
            user.setAdditionalRoles(roles);
            userRepository.save(user);
        }

        PatientParent link = new PatientParent(patientId, user.getId());
        patientParentRepository.save(link);

        return buildResponse(patient);
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
        return buildResponses(List.of(patient), false).get(0);
    }

    /**
     * Batched form of the single-patient builder above — one round trip per related table for the
     * whole page instead of one per patient. The single-patient version funnels through this for
     * the handful of call sites that only ever touch one patient (create/update/etc); every list
     * endpoint (Cases page, and every other page's "give me all patients" call via patientsApi.list)
     * calls this directly. With real cross-region latency to the DB, the old per-patient version
     * turned a page of 18 patients into 90+ sequential round trips — seconds of wall-clock time for
     * what should be a handful of queries.
     *
     * {@code compact}: the Cases page only ever reads parents.length / therapists.length (invite
     * status, specialist count) — never their names. When true, skips resolving parent/therapist
     * ids to User rows entirely (one fewer batched query, smaller payload); conditions and
     * therapies are unaffected since both are actually rendered as name chips on that same page.
     */
    private List<PatientResponse> buildResponses(List<Patient> patients, boolean compact) {
        if (patients.isEmpty()) return List.of();

        List<UUID> patientIds = patients.stream().map(Patient::getId).toList();
        UUID orgId = patients.get(0).getOrgId();

        Map<UUID, List<PatientCondition>> conditionsByPatient = patientConditionRepository
                .findById_PatientIdIn(patientIds).stream()
                .collect(Collectors.groupingBy(pc -> pc.getId().getPatientId()));
        Set<UUID> conditionIds = conditionsByPatient.values().stream()
                .flatMap(List::stream).map(pc -> pc.getId().getConditionId()).collect(Collectors.toSet());
        Map<UUID, Condition> conditionMap = conditionIds.isEmpty() ? Map.of()
                : conditionRepository.findAllById(conditionIds).stream()
                        .collect(Collectors.toMap(Condition::getId, c -> c));

        Map<UUID, List<PatientParent>> parentsByPatient = patientParentRepository
                .findById_PatientIdIn(patientIds).stream()
                .collect(Collectors.groupingBy(pp -> pp.getId().getPatientId()));

        Map<UUID, List<TherapistPatient>> assignmentsByPatient = therapistPatientRepository
                .findByPatientIdInAndIsActive(patientIds, true).stream()
                .collect(Collectors.groupingBy(TherapistPatient::getPatientId));

        Map<UUID, User> userMap = Map.of();
        if (!compact) {
            Set<UUID> userIds = new HashSet<>();
            parentsByPatient.values().forEach(list -> list.forEach(pp -> userIds.add(pp.getId().getParentId())));
            assignmentsByPatient.values().forEach(list -> list.forEach(a -> userIds.add(a.getTherapistId())));
            userMap = userIds.isEmpty() ? Map.of()
                    : userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        Map<UUID, User> finalUserMap = userMap;

        Map<UUID, List<Subscription>> subscriptionsByPatient = subscriptionRepository
                .findByOrgIdAndPatientIdInOrderByCreatedAtDesc(orgId, patientIds).stream()
                .collect(Collectors.groupingBy(Subscription::getPatientId));
        Set<UUID> programIds = subscriptionsByPatient.values().stream().flatMap(List::stream)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(Subscription::getProgramId).collect(Collectors.toSet());
        Map<UUID, Program> programMap = programIds.isEmpty() ? Map.of()
                : programRepository.findAllById(programIds).stream()
                        .collect(Collectors.toMap(Program::getId, p -> p));

        return patients.stream().map(patient -> {
            List<PatientCondition> pcs = conditionsByPatient.getOrDefault(patient.getId(), List.of());
            List<Condition> conditionDetails = pcs.stream()
                    .map(pc -> conditionMap.get(pc.getId().getConditionId()))
                    .filter(Objects::nonNull)
                    .toList();

            List<PatientParent> pps = parentsByPatient.getOrDefault(patient.getId(), List.of());
            List<TherapistPatient> assignments = assignmentsByPatient.getOrDefault(patient.getId(), List.of());

            List<UUID> activeProgramIds = subscriptionsByPatient.getOrDefault(patient.getId(), List.of()).stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .map(Subscription::getProgramId)
                    .distinct()
                    .toList();
            List<PatientResponse.TherapySummary> therapySummaries = activeProgramIds.stream()
                    .map(programMap::get)
                    .filter(Objects::nonNull)
                    .map(p -> new PatientResponse.TherapySummary(p.getId(), p.getName()))
                    .toList();

            if (compact) {
                return PatientResponse.fromCompact(patient, pcs, conditionDetails, pps, assignments, therapySummaries);
            }

            List<User> parents = pps.stream()
                    .map(pp -> finalUserMap.get(pp.getId().getParentId()))
                    .filter(Objects::nonNull)
                    .toList();
            List<User> therapists = assignments.stream()
                    .map(a -> finalUserMap.get(a.getTherapistId()))
                    .filter(Objects::nonNull)
                    .toList();

            return PatientResponse.from(patient, pcs, conditionDetails, parents, assignments, therapists, therapySummaries);
        }).toList();
    }
}
