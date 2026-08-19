package com.simplehearing.task.repository;

import com.simplehearing.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /**
     * A non-manager's view of the board: work assigned to them, plus work they raised
     * themselves. Without the second clause a task would vanish from its creator's list
     * the moment they assigned it to somebody else.
     */
    @Query("SELECT t FROM Task t WHERE t.orgId = :orgId " +
           "AND (t.assignedBy = :userId " +
           "     OR EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId)) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByOrgIdAndAssigneeOrCreator(@Param("orgId") UUID orgId, @Param("userId") UUID userId);
}
