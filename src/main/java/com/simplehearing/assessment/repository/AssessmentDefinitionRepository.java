package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentDefinitionRepository extends JpaRepository<AssessmentDefinition, UUID> {
    Optional<AssessmentDefinition> findByCode(String code);
}
