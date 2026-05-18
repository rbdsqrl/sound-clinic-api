package com.simplehearing.task.repository;

import com.simplehearing.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    @Query("SELECT t FROM Task t WHERE t.orgId = :orgId " +
           "AND EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByOrgIdAndAssignee(@Param("orgId") UUID orgId, @Param("userId") UUID userId);
}
