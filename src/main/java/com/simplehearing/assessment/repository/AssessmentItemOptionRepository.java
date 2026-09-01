package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentItemOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentItemOptionRepository extends JpaRepository<AssessmentItemOption, UUID> {
    List<AssessmentItemOption> findByItemIdOrderByDisplayOrder(UUID itemId);

    List<AssessmentItemOption> findByItemIdIn(List<UUID> itemIds);
}
