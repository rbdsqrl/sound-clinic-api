package com.simplehearing.reassignment.repository;

import com.simplehearing.reassignment.entity.TherapistReassignment;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TherapistReassignmentRepository extends JpaRepository<TherapistReassignment, UUID> {

    /** Every batch a therapist appears in, either side, newest first. */
    @Query("SELECT r FROM TherapistReassignment r WHERE r.orgId = :orgId "
         + "AND (r.fromTherapistId = :therapistId OR r.toTherapistId = :therapistId) "
         + "ORDER BY r.createdAt DESC")
    List<TherapistReassignment> findForTherapist(@Param("orgId") UUID orgId, @Param("therapistId") UUID therapistId);

    /** The nightly revert job's scan: temporary batches whose window has closed. */
    List<TherapistReassignment> findByStatusAndTypeAndEndDateLessThanEqual(
            ReassignmentStatus status, ReassignmentType type, LocalDate date);
}
