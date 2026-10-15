package com.simplehearing.customrole.repository;

import com.simplehearing.customrole.entity.OrgCustomRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgCustomRoleRepository extends JpaRepository<OrgCustomRole, UUID> {

    List<OrgCustomRole> findByOrgIdOrderByNameAsc(UUID orgId);

    Optional<OrgCustomRole> findByOrgIdAndNameIgnoreCase(UUID orgId, String name);

    Optional<OrgCustomRole> findByIdAndOrgId(UUID id, UUID orgId);
}
