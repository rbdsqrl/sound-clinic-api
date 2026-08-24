package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityChecklistQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityChecklistQuestionRepository extends JpaRepository<ActivityChecklistQuestion, UUID> {
    List<ActivityChecklistQuestion> findByActivityIdOrderByOrderIndexAsc(UUID activityId);

    @Transactional
    void deleteByActivityId(UUID activityId);
}
