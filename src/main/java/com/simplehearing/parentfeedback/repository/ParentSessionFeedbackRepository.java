package com.simplehearing.parentfeedback.repository;

import com.simplehearing.parentfeedback.entity.ParentSessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentSessionFeedbackRepository extends JpaRepository<ParentSessionFeedback, UUID> {

    List<ParentSessionFeedback> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<ParentSessionFeedback> findBySessionIdAndParentId(UUID sessionId, UUID parentId);

    /** Feedback no admin has read yet — the admin dashboard card — newest first. */
    List<ParentSessionFeedback> findByOrgIdAndAdminReviewedAtIsNullOrderByCreatedAtDesc(UUID orgId);

    int countByOrgIdAndAdminReviewedAtIsNull(UUID orgId);

    /** A parent's own feedback on the sessions of one plan. */
    @Query("SELECT f FROM ParentSessionFeedback f WHERE f.parentId = :parentId AND f.sessionId IN "
         + "(SELECT s.id FROM TherapySession s WHERE s.enrollmentId = :enrollmentId)")
    List<ParentSessionFeedback> findMineForEnrollment(@Param("parentId") UUID parentId,
                                                      @Param("enrollmentId") UUID enrollmentId);

    /** Feedback an admin shared with this therapist that they haven't marked seen — the therapist dashboard card. */
    @Query("SELECT f FROM ParentSessionFeedback f WHERE f.sharedWithTherapist = true AND f.therapistSeenAt IS NULL "
         + "AND f.sessionId IN (SELECT s.id FROM TherapySession s WHERE s.therapistId = :therapistId) "
         + "ORDER BY f.sharedAt DESC")
    List<ParentSessionFeedback> findSharedUnseenForTherapist(@Param("therapistId") UUID therapistId);
}
