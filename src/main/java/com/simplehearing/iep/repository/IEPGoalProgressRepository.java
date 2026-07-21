package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPGoalProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IEPGoalProgressRepository extends JpaRepository<IEPGoalProgress, UUID> {

    List<IEPGoalProgress> findByGoalIdOrderBySessionDateDesc(UUID goalId);

    int countByGoalId(UUID goalId);

    void deleteByGoalId(UUID goalId);
}
