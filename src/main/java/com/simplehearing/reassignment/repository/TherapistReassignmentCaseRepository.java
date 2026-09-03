package com.simplehearing.reassignment.repository;

import com.simplehearing.reassignment.entity.TherapistReassignmentCase;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TherapistReassignmentCaseRepository extends JpaRepository<TherapistReassignmentCase, UUID> {

    List<TherapistReassignmentCase> findByReassignment_Id(UUID reassignmentId);

    @Query("SELECT COUNT(c) > 0 FROM TherapistReassignmentCase c WHERE c.patientId = :patientId "
         + "AND c.reassignment.status = :status")
    boolean existsByPatientIdAndReassignmentStatus(
            @Param("patientId") UUID patientId, @Param("status") ReassignmentStatus status);
}
