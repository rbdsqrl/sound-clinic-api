package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
    Optional<Skill> findByOrgIdAndNameIgnoreCase(UUID orgId, String name);
}
