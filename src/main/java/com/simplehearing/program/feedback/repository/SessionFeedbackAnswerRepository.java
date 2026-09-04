package com.simplehearing.program.feedback.repository;

import com.simplehearing.program.feedback.entity.SessionFeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SessionFeedbackAnswerRepository extends JpaRepository<SessionFeedbackAnswer, UUID> {
    List<SessionFeedbackAnswer> findBySessionId(UUID sessionId);
    List<SessionFeedbackAnswer> findBySessionIdIn(List<UUID> sessionIds);

    /**
     * A bulk DELETE, not a derived delete — a derived delete queues individual entity removals
     * that Hibernate flushes AFTER pending inserts (its action queue always orders inserts before
     * deletes, regardless of call order), so re-saving a session's answers would insert the new
     * row before the old same-(session_id, question_id) row was actually gone, tripping the
     * unique constraint. A bulk query runs immediately, sidestepping that ordering entirely.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SessionFeedbackAnswer a WHERE a.sessionId = :sessionId")
    void deleteBySessionId(UUID sessionId);
}
