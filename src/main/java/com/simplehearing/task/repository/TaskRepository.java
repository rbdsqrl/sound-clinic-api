package com.simplehearing.task.repository;

import com.simplehearing.task.entity.Task;
import com.simplehearing.task.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /** Paginated form of the above — backs the Dashboard's "My Tasks" preview + its "View all" fetch. */
    Page<Task> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

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

    /** Paginated form of the above. */
    @Query(value = "SELECT t FROM Task t WHERE t.orgId = :orgId " +
           "AND (t.assignedBy = :userId " +
           "     OR EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId)) " +
           "ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE t.orgId = :orgId " +
           "AND (t.assignedBy = :userId " +
           "     OR EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId))")
    Page<Task> findByOrgIdAndAssigneeOrCreator(@Param("orgId") UUID orgId, @Param("userId") UUID userId, Pageable pageable);

    /**
     * Backs the Dashboard's "My Tasks" widget specifically — tasks assigned to the caller (not
     * creator, unlike the board view above) in a given status set. Only used when the caller
     * passes {@code mine} or {@code status}; the plain org/board queries above are untouched so
     * the Tasks page's behaviour never changes.
     */
    @Query(value = "SELECT t FROM Task t WHERE t.orgId = :orgId " +
           "AND (:mine = false OR EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId)) " +
           "AND (:anyStatus = true OR t.status IN :statuses) " +
           "ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE t.orgId = :orgId " +
           "AND (:mine = false OR EXISTS (SELECT a FROM TaskAssignee a WHERE a.id.taskId = t.id AND a.id.userId = :userId)) " +
           "AND (:anyStatus = true OR t.status IN :statuses)")
    Page<Task> search(@Param("orgId") UUID orgId, @Param("mine") boolean mine, @Param("userId") UUID userId,
                       @Param("anyStatus") boolean anyStatus, @Param("statuses") Collection<TaskStatus> statuses,
                       Pageable pageable);
}
