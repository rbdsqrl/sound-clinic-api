package com.simplehearing.patient.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.condition.entity.Condition;
import com.simplehearing.condition.repository.ConditionRepository;
import com.simplehearing.patient.dto.*;
import com.simplehearing.patient.entity.*;
import com.simplehearing.patient.repository.*;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    public PatientService(PatientRepository patientRepository,
                          PatientConditionRepository patientConditionRepository,
                          PatientParentRepository patientParentRepository,
                          TherapistPatientRepository therapistPatientRepository,
                          ConditionRepository conditionRepository,
                          UserRepository userRepository,
                          ClinicRepository clinicRepository) {
        this.patientRepository = patientRepository;
        this.patientConditionRepository = patientConditionRepository;
        this.patientParentRepository = patientParentRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.conditionRepository = conditionRepository;
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public PatientResponse create(CreatePatientRequest request, UserPrincipal principal) {
        clinicRepository.findByIdAndOrgId(request.clinicId(), principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found in your organisation"));

        Patient patient = new Patient();
        patient.setOrgId(principal.getOrgId());
        patient.setClinicId(request.clinicId());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setNotes(request.notes());
        patientRepository.save(patient);

        return buildResponse(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listForOrg(UserPrincipal principal) {
        return patientRepository.findByOrgId(principal.getOrgId()).stream()
                .map(this::buildResponse)
                .toList();
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
        Patient patient = findPatient(patientId, principal.getOrgId());
        patient.setStage(request.stage());
        return buildResponse(patientRepository.save(patient));
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
                    therapistPatientRepository.save(tp);
                });
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

        return PatientResponse.from(patient, pcs, conditionDetails, parents, assignments, therapists);
    }
}
