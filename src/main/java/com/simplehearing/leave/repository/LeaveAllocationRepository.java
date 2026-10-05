package com.simplehearing.leave.repository;

import com.simplehearing.leave.entity.LeaveAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveAllocationRepository extends JpaRepository<LeaveAllocation, UUID> {

    List<LeaveAllocation> findByOrgIdAndLeaveYearAndUserIdIn(UUID orgId, int leaveYear, Collection<UUID> userIds);

    Optional<LeaveAllocation> findByUserIdAndCategoryIdAndLeaveYear(UUID userId, UUID categoryId, int leaveYear);
}
