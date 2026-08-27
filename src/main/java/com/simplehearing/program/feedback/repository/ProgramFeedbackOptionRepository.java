package com.simplehearing.program.feedback.repository;

import com.simplehearing.program.feedback.entity.ProgramFeedbackOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProgramFeedbackOptionRepository extends JpaRepository<ProgramFeedbackOption, UUID> {
    List<ProgramFeedbackOption> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    @Query("SELECT o FROM ProgramFeedbackOption o WHERE o.questionId IN :questionIds ORDER BY o.orderIndex ASC")
    List<ProgramFeedbackOption> findByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);
}
