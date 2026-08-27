package com.simplehearing.program.feedback.repository;

import com.simplehearing.program.feedback.entity.SessionFeedbackAnswerOption;
import com.simplehearing.program.feedback.entity.SessionFeedbackAnswerOptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionFeedbackAnswerOptionRepository
        extends JpaRepository<SessionFeedbackAnswerOption, SessionFeedbackAnswerOptionId> {

    List<SessionFeedbackAnswerOption> findById_AnswerId(UUID answerId);
}
