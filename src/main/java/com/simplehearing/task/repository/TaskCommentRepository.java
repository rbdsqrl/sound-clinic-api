package com.simplehearing.task.repository;

import com.simplehearing.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    void deleteByTaskId(UUID taskId);

    @Query("SELECT c.taskId AS taskId, COUNT(c) AS cnt FROM TaskComment c WHERE c.taskId IN :taskIds GROUP BY c.taskId")
    List<CountByTask> countByTaskIdIn(List<UUID> taskIds);

    interface CountByTask {
        UUID getTaskId();
        Long getCnt();
    }
}
