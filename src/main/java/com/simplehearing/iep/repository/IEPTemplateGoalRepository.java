package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPTemplateGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEPTemplateGoalRepository extends JpaRepository<IEPTemplateGoal, UUID> {

    List<IEPTemplateGoal> findByTemplateIdOrderByCreatedAtAsc(UUID templateId);

    Optional<IEPTemplateGoal> findByIdAndOrgId(UUID id, UUID orgId);

    void deleteByTemplateId(UUID templateId);
}
