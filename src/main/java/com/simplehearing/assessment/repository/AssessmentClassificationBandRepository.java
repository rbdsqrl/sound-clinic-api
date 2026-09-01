package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentClassificationBand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentClassificationBandRepository extends JpaRepository<AssessmentClassificationBand, UUID> {
    List<AssessmentClassificationBand> findByDefinitionIdOrderByDisplayOrder(UUID definitionId);
}
