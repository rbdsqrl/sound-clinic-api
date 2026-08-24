package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityChecklistOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityChecklistOptionRepository extends JpaRepository<ActivityChecklistOption, UUID> {
    List<ActivityChecklistOption> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    @Query("SELECT o FROM ActivityChecklistOption o WHERE o.questionId IN :questionIds ORDER BY o.orderIndex ASC")
    List<ActivityChecklistOption> findByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);
}
