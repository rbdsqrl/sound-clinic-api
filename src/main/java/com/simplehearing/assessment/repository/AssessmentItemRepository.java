package com.simplehearing.assessment.repository;

import com.simplehearing.assessment.entity.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, UUID> {
    List<AssessmentItem> findByCategoryIdOrderByDisplayOrder(UUID categoryId);

    List<AssessmentItem> findByCategoryIdIn(List<UUID> categoryIds);
}
