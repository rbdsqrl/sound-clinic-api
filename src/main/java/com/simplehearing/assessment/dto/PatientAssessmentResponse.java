package com.simplehearing.assessment.dto;

import com.simplehearing.assessment.entity.PatientAssessment;
import com.simplehearing.assessment.enums.AssessmentType;

import java.time.LocalDate;
import java.util.UUID;

public record PatientAssessmentResponse(
        UUID id,
        AssessmentType assessmentType,
        LocalDate assessmentDate,
        String filledByName,
        int totalScore,
        int maxScore,
        String classification
) {
    public static PatientAssessmentResponse from(PatientAssessment a, String filledByName, int maxScore) {
        return new PatientAssessmentResponse(
                a.getId(), a.getAssessmentType(), a.getAssessmentDate(), filledByName,
                a.getTotalScore(), maxScore, a.getClassification());
    }
}
