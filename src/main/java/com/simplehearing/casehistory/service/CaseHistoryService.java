package com.simplehearing.casehistory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.casehistory.dto.CaseHistoryResponse;
import com.simplehearing.casehistory.dto.UpdateCaseHistoryRequest;
import com.simplehearing.casehistory.entity.CaseHistory;
import com.simplehearing.casehistory.repository.CaseHistoryRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CaseHistoryService {

    private final CaseHistoryRepository caseHistoryRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    public CaseHistoryService(
            CaseHistoryRepository caseHistoryRepository,
            PatientRepository patientRepository,
            ObjectMapper objectMapper) {
        this.caseHistoryRepository = caseHistoryRepository;
        this.patientRepository = patientRepository;
        this.objectMapper = objectMapper;
    }

    /** Returns null if the patient has no case history yet — the UI shows an empty-state card. */
    public CaseHistoryResponse get(UUID orgId, UUID patientId) {
        return caseHistoryRepository.findByOrgIdAndPatientId(orgId, patientId)
                .map(c -> CaseHistoryResponse.from(c, objectMapper))
                .orElse(null);
    }

    @Transactional
    public CaseHistoryResponse upsert(UUID orgId, UUID patientId, UpdateCaseHistoryRequest request, UUID actorId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        if (!patient.getOrgId().equals(orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        CaseHistory c = caseHistoryRepository.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    CaseHistory fresh = new CaseHistory();
                    fresh.setOrgId(orgId);
                    fresh.setPatientId(patientId);
                    fresh.setCreatedBy(actorId);
                    return fresh;
                });

        c.setPresentComplaints(request.presentComplaints());
        c.setHabits(writeJson(request.habits()));
        c.setPhysicalOtherProblems(request.physicalOtherProblems());

        c.setPrenatalHealth(writeJson(request.prenatalHealth()));
        c.setDeliveryType(request.deliveryType());
        c.setLabourType(request.labourType());
        c.setBirthCry(request.birthCry());
        c.setPrenatalNotes(request.prenatalNotes());
        c.setBirthAdditionalNotes(request.birthAdditionalNotes());
        c.setBirthHeight(request.birthHeight());
        c.setBirthHeightUnit(request.birthHeightUnit());
        c.setBirthWeight(request.birthWeight());
        c.setBirthWeightUnit(request.birthWeightUnit());
        c.setPostnatalHealth(writeJson(request.postnatalHealth()));
        c.setPhototherapyDays(request.phototherapyDays());
        c.setPostnatalNotes(request.postnatalNotes());

        c.setMotorMilestones(request.motorMilestones());
        c.setSpeechMilestones(request.speechMilestones());
        c.setMilestoneSkills(writeJson(request.milestoneSkills()));
        c.setMilestonesAdditionalNotes(request.milestonesAdditionalNotes());
        c.setHandedness(request.handedness());

        c.setFamilyType(request.familyType());
        c.setFamilyMembers(writeJson(request.familyMembers()));
        c.setConsanguinityHistory(request.consanguinityHistory());
        c.setFamilyImpairmentsNotes(request.familyImpairmentsNotes());

        c.setEyeContact(request.eyeContact());
        c.setStutteringFrequency(request.stutteringFrequency());
        c.setPlayBehavior(request.playBehavior());
        c.setSocialSmiling(request.socialSmiling());
        c.setBehaviouralSelfRegulation(request.behaviouralSelfRegulation());
        c.setEmotionalSelfRegulation(request.emotionalSelfRegulation());
        c.setFriendships(request.friendships());
        c.setListening(request.listening());
        c.setCommunications(writeJson(request.communications()));
        c.setBehavioralProblems(writeJson(request.behavioralProblems()));
        c.setProvisionalDiagnosis(request.provisionalDiagnosis());

        c.setCurrentGrade(request.currentGrade());
        c.setSchool(request.school());
        c.setSyllabus(request.syllabus());
        c.setAgeOfJoining(request.ageOfJoining());
        c.setPerformanceAndProgress(request.performanceAndProgress());
        c.setAttitudeTowardsStudies(request.attitudeTowardsStudies());
        c.setSchoolAdditionalNotes(request.schoolAdditionalNotes());

        c.setUpdatedBy(actorId);

        CaseHistory saved = caseHistoryRepository.save(c);
        return CaseHistoryResponse.from(saved, objectMapper);
    }

    private String writeJson(List<?> value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
