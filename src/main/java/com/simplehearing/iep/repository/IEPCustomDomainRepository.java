package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPCustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEPCustomDomainRepository extends JpaRepository<IEPCustomDomain, UUID> {

    List<IEPCustomDomain> findByOrgIdOrderByNameAsc(UUID orgId);

    Optional<IEPCustomDomain> findByOrgIdAndNameIgnoreCase(UUID orgId, String name);

    Optional<IEPCustomDomain> findByIdAndOrgId(UUID id, UUID orgId);
}
