package com.simplehearing.task.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_comments")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId()                      { return id; }
    public UUID getOrgId()                   { return orgId; }
    public void setOrgId(UUID orgId)         { this.orgId = orgId; }
    public UUID getTaskId()                  { return taskId; }
    public void setTaskId(UUID taskId)       { this.taskId = taskId; }
    public UUID getAuthorId()                { return authorId; }
    public void setAuthorId(UUID authorId)   { this.authorId = authorId; }
    public String getBody()                  { return body; }
    public void setBody(String body)         { this.body = body; }
    public Instant getCreatedAt()            { return createdAt; }
}
