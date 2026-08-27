package com.simplehearing.assessment.dto;

import com.simplehearing.assessment.def.AssessmentDefinitions;
import com.simplehearing.assessment.enums.AssessmentType;

import java.util.List;

public record AssessmentDefinitionResponse(
        AssessmentType assessmentType,
        int maxScore,
        List<AssessmentDefinitions.Section> sections
) {
    public static AssessmentDefinitionResponse from(AssessmentType type) {
        return new AssessmentDefinitionResponse(type, AssessmentDefinitions.maxScoreFor(type), AssessmentDefinitions.sectionsFor(type));
    }
}
