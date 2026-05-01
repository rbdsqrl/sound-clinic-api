package com.simplehearing.condition.repository;

import com.simplehearing.condition.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConditionRepository extends JpaRepository<Condition, UUID> {

    List<Condition> findByIsActiveTrue();
}
