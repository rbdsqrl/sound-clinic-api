package com.simplehearing.review.repository;

import com.simplehearing.review.entity.ReviewMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewMeetingRepository extends JpaRepository<ReviewMeeting, UUID> {

    List<ReviewMeeting> findByEnrollmentIdOrderByMeetingNumberAsc(UUID enrollmentId);

    List<ReviewMeeting> findByOrgIdOrderByMeetingDateAsc(UUID orgId);

    List<ReviewMeeting> findByOrgIdAndPatientIdOrderByMeetingDateAsc(UUID orgId, UUID patientId);

    List<ReviewMeeting> findByOrgIdAndTherapistIdOrderByMeetingDateAsc(UUID orgId, UUID therapistId);

    @Query("SELECT m FROM ReviewMeeting m WHERE m.orgId = :orgId "
         + "AND m.meetingDate BETWEEN :from AND :to ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<ReviewMeeting> findInRange(@Param("orgId") UUID orgId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /** Meetings for the patients this parent is linked to — the parent's own calendar view. */
    @Query("SELECT m FROM ReviewMeeting m WHERE m.orgId = :orgId "
         + "AND m.patientId IN (SELECT p.id.patientId FROM PatientParent p WHERE p.id.parentId = :parentId) "
         + "ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<ReviewMeeting> findForParent(@Param("orgId") UUID orgId, @Param("parentId") UUID parentId);

    long countByEnrollmentId(UUID enrollmentId);

    /** Meetings currently owned by a specific bulk therapist reassignment — the revert scan. */
    List<ReviewMeeting> findByReassignmentId(UUID reassignmentId);
}
