package com.simplehearing.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.assessment.def.AssessmentDefinitions;
import com.simplehearing.assessment.dto.CreateAssessmentRequest;
import com.simplehearing.assessment.dto.PatientAssessmentResponse;
import com.simplehearing.assessment.entity.PatientAssessment;
import com.simplehearing.assessment.enums.AssessmentType;
import com.simplehearing.assessment.repository.PatientAssessmentRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentService {

    private final PatientAssessmentRepository assessmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AssessmentService(PatientAssessmentRepository assessmentRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<PatientAssessmentResponse> list(UUID orgId, UUID patientId, AssessmentType type) {
        int maxScore = AssessmentDefinitions.maxScoreFor(type);
        return assessmentRepository.findByOrgIdAndPatientIdAndAssessmentTypeOrderByAssessmentDateAsc(orgId, patientId, type).stream()
                .map(a -> PatientAssessmentResponse.from(a, userName(a.getFilledBy()), maxScore))
                .toList();
    }

    public PatientAssessmentResponse create(UUID orgId, UUID patientId, UUID filledBy, AssessmentType type, CreateAssessmentRequest request) {
        Patient patient = patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Map<Integer, AssessmentDefinitions.Item> items = AssessmentDefinitions.itemsByNumber(type);
        if (!request.itemScores().keySet().equals(items.keySet())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Every item must be answered");
        }

        int total = 0;
        for (Map.Entry<Integer, Integer> entry : request.itemScores().entrySet()) {
            AssessmentDefinitions.Item item = items.get(entry.getKey());
            boolean validScore = item.options().stream().anyMatch(o -> o.score() == entry.getValue());
            if (!validScore) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid score for item " + entry.getKey());
            }
            total += entry.getValue();
        }

        PatientAssessment assessment = new PatientAssessment();
        assessment.setOrgId(orgId);
        assessment.setPatientId(patientId);
        assessment.setAssessmentType(type);
        assessment.setAssessmentDate(request.assessmentDate());
        assessment.setFilledBy(filledBy);
        assessment.setItemScores(writeJson(request.itemScores()));
        assessment.setTotalScore(total);
        assessment.setClassification(classify(type, total, patient.getDateOfBirth(), request.assessmentDate()));

        PatientAssessment saved = assessmentRepository.save(assessment);
        return PatientAssessmentResponse.from(saved, userName(filledBy), AssessmentDefinitions.maxScoreFor(type));
    }

    private String classify(AssessmentType type, int total, LocalDate dob, LocalDate assessmentDate) {
        if (type == AssessmentType.ISAA) {
            if (total < 70) return "No Autism";
            if (total <= 106) return "Mild Autism";
            if (total <= 153) return "Moderate Autism";
            return "Severe Autism";
        }

        // PRBA — Adequate/Inadequate depends on the child's age at assessment time. Only two
        // clean bands were available from the scanned scoring table (1-2yrs, 2.1-3yrs); per
        // the clinic, 3+ years reuses the 2.1-3yrs band. A score outside the range that band
        // actually defines (or an age under 1 year) is left unclassified rather than guessed.
        if (dob == null) return null;
        double ageYears = Period.between(dob, assessmentDate).toTotalMonths() / 12.0;
        if (ageYears < 1.0) return null;
        if (ageYears < 2.0) {
            if (total >= 26 && total <= 29) return "Adequate";
            if (total < 25) return "Inadequate";
            return null;
        }
        if (total >= 31 && total <= 35) return "Adequate";
        if (total < 30) return "Inadequate";
        return null;
    }

    private String userName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> (nullToEmpty(u.getFirstName()) + " " + nullToEmpty(u.getLastName())).trim())
                .orElse(null);
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode item scores");
        }
    }
}
