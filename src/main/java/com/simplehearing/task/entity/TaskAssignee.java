package com.simplehearing.task.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "task_assignees")
public class TaskAssignee {

    @EmbeddedId
    private TaskAssigneeId id;

    public TaskAssignee() {}

    public TaskAssignee(UUID taskId, UUID userId) {
        this.id = new TaskAssigneeId(taskId, userId);
    }

    public TaskAssigneeId getId() { return id; }
    public UUID getTaskId() { return id.getTaskId(); }
    public UUID getUserId() { return id.getUserId(); }
}
