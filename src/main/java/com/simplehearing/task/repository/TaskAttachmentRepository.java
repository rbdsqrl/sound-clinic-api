package com.simplehearing.task.repository;

import com.simplehearing.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {
    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    void deleteByTaskId(UUID taskId);

    @Query("SELECT a.taskId AS taskId, COUNT(a) AS cnt FROM TaskAttachment a WHERE a.taskId IN :taskIds GROUP BY a.taskId")
    List<CountByTask> countByTaskIdIn(List<UUID> taskIds);

    interface CountByTask {
        UUID getTaskId();
        Long getCnt();
    }
}
