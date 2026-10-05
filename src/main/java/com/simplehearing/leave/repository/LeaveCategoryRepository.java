package com.simplehearing.leave.repository;

import com.simplehearing.leave.entity.LeaveCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveCategoryRepository extends JpaRepository<LeaveCategory, UUID> {

    List<LeaveCategory> findByOrgIdOrderByNameAsc(UUID orgId);

    List<LeaveCategory> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);

    Optional<LeaveCategory> findByIdAndOrgId(UUID id, UUID orgId);

    boolean existsByOrgIdAndNameIgnoreCase(UUID orgId, String name);
}
