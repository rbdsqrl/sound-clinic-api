package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentResponseRepository extends JpaRepository<AssessmentResponse, UUID> {
    List<AssessmentResponse> findByPatientAssessmentId(UUID patientAssessmentId);

    List<AssessmentResponse> findByPatientAssessmentIdIn(List<UUID> patientAssessmentIds);
}
