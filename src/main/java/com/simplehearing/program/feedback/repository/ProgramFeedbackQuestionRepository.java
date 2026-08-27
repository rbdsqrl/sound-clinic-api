package com.simplehearing.program.feedback.repository;

import com.simplehearing.program.feedback.entity.ProgramFeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramFeedbackQuestionRepository extends JpaRepository<ProgramFeedbackQuestion, UUID> {
    List<ProgramFeedbackQuestion> findByProgramIdOrderByOrderIndexAsc(UUID programId);
}
