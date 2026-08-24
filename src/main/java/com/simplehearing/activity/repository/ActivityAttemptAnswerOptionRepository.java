package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityAttemptAnswerOption;
import com.simplehearing.activity.entity.ActivityAttemptAnswerOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityAttemptAnswerOptionRepository
        extends JpaRepository<ActivityAttemptAnswerOption, ActivityAttemptAnswerOptionId> {

    List<ActivityAttemptAnswerOption> findById_AnswerId(UUID answerId);

    @Query("SELECT o FROM ActivityAttemptAnswerOption o WHERE o.id.answerId IN :answerIds")
    List<ActivityAttemptAnswerOption> findByAnswerIdIn(@Param("answerIds") List<UUID> answerIds);
}
