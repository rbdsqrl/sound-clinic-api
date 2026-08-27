package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.PatientAssessment;
import com.simplehearing.assessment.enums.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientAssessmentRepository extends JpaRepository<PatientAssessment, UUID> {
    List<PatientAssessment> findByOrgIdAndPatientIdAndAssessmentTypeOrderByAssessmentDateAsc(
            UUID orgId, UUID patientId, AssessmentType assessmentType);
}
