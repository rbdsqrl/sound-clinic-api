package com.simplehearing.program.feedback.repository;

import com.simplehearing.program.feedback.entity.SessionFeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SessionFeedbackAnswerRepository extends JpaRepository<SessionFeedbackAnswer, UUID> {
    List<SessionFeedbackAnswer> findBySessionId(UUID sessionId);
    List<SessionFeedbackAnswer> findBySessionIdIn(List<UUID> sessionIds);

    @Transactional
    void deleteBySessionId(UUID sessionId);
}
