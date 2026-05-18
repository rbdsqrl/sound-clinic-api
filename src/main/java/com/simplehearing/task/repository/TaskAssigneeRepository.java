package com.simplehearing.task.repository;

import com.simplehearing.task.entity.TaskAssignee;
import com.simplehearing.task.entity.TaskAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, TaskAssigneeId> {

    List<TaskAssignee> findById_TaskId(UUID taskId);

    @Query("SELECT a FROM TaskAssignee a WHERE a.id.taskId IN :taskIds")
    List<TaskAssignee> findByTaskIdIn(@Param("taskIds") List<UUID> taskIds);

    void deleteById_TaskId(UUID taskId);
}
