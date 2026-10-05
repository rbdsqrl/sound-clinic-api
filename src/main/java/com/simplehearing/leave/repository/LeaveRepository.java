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

    /** Approved leaves for one staff member whose (possibly multi-day) range covers the given date. */
    List<Leave> findByOrgIdAndTherapistIdAndStatusAndLeaveDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID orgId, UUID therapistId, LeaveStatus status, java.time.LocalDate date, java.time.LocalDate sameDate);

    /** Leaves of the given people, in the given statuses, whose range overlaps [from, to] — the basis of leave balances. */
    @org.springframework.data.jpa.repository.Query("SELECT l FROM Leave l WHERE l.orgId = :orgId AND l.therapistId IN :userIds "
         + "AND l.status IN :statuses AND l.leaveDate <= :to AND l.endDate >= :from")
    List<Leave> findOverlapping(@org.springframework.data.repository.query.Param("orgId") UUID orgId,
                                @org.springframework.data.repository.query.Param("userIds") java.util.Collection<UUID> userIds,
                                @org.springframework.data.repository.query.Param("statuses") java.util.Collection<LeaveStatus> statuses,
                                @org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
                                @org.springframework.data.repository.query.Param("to") java.time.LocalDate to);
}
