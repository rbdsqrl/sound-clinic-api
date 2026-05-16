package com.simplehearing.task.repository;

import com.simplehearing.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
    List<Task> findByOrgIdAndAssignedToOrderByCreatedAtDesc(UUID orgId, UUID assignedTo);
}
