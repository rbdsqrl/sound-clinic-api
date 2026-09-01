package com.simplehearing.assessment.dto;

import com.simplehearing.assessment.entity.PatientAssessment;

import java.time.LocalDate;
import java.util.UUID;

public record PatientAssessmentResponse(
        UUID id,
        String definitionCode,
        LocalDate assessmentDate,
        String filledByName,
        Integer totalScore,
        Integer maxScore,
        String classification
) {
    public static PatientAssessmentResponse from(PatientAssessment a, String definitionCode, String filledByName, Integer maxScore) {
        return new PatientAssessmentResponse(
                a.getId(), definitionCode, a.getAssessmentDate(), filledByName,
                a.getTotalScore(), maxScore, a.getClassification());
    }
}
