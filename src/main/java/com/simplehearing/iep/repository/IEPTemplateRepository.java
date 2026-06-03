package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEPTemplateRepository extends JpaRepository<IEPTemplate, UUID> {

    List<IEPTemplate> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    Optional<IEPTemplate> findByIdAndOrgId(UUID id, UUID orgId);
}
