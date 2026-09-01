package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.PatientAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientAssessmentRepository extends JpaRepository<PatientAssessment, UUID> {
    List<PatientAssessment> findByOrgIdAndPatientIdAndDefinitionIdOrderByAssessmentDateAsc(
            UUID orgId, UUID patientId, UUID definitionId);
}
