package com.simplehearing.leave.repository;

import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {

    /** All leaves in the org — for business owner / admin view */
    List<Leave> findByOrgIdOrderByLeaveDateDesc(UUID orgId);

    /** All leaves in the org filtered by status */
    List<Leave> findByOrgIdAndStatusOrderByLeaveDateDesc(UUID orgId, LeaveStatus status);

    /** A single therapist's own leaves */
    List<Leave> findByOrgIdAndTherapistIdOrderByLeaveDateDesc(UUID orgId, UUID therapistId);

    /** Approved leaves on a specific date (used for therapist availability checks) */
    List<Leave> findByOrgIdAndLeaveDateAndStatus(UUID orgId, java.time.LocalDate leaveDate, LeaveStatus status);
}
