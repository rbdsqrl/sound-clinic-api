package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LanguageRepository extends JpaRepository<Language, UUID> {
    List<Language> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
    Optional<Language> findByOrgIdAndNameIgnoreCase(UUID orgId, String name);
}
