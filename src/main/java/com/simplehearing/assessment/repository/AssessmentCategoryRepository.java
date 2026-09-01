package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentCategoryRepository extends JpaRepository<AssessmentCategory, UUID> {
    List<AssessmentCategory> findByDefinitionIdOrderByDisplayOrder(UUID definitionId);
}
