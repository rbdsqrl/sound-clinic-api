package com.simplehearing.task.repository;

import com.simplehearing.task.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskLogRepository extends JpaRepository<TaskLog, UUID> {
    List<TaskLog> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
