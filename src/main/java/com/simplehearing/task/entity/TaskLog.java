package com.simplehearing.task.entity;

import com.simplehearing.task.enums.TaskLogType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_logs")
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 50)
    private TaskLogType logType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_name", nullable = false, length = 255)
    private String actorName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId()                    { return id; }
    public UUID getOrgId()                 { return orgId; }
    public void setOrgId(UUID orgId)       { this.orgId = orgId; }
    public UUID getTaskId()                { return taskId; }
    public void setTaskId(UUID taskId)     { this.taskId = taskId; }
    public TaskLogType getLogType()        { return logType; }
    public void setLogType(TaskLogType t)  { this.logType = t; }
    public UUID getActorId()               { return actorId; }
    public void setActorId(UUID actorId)   { this.actorId = actorId; }
    public String getActorName()           { return actorName; }
    public void setActorName(String n)     { this.actorName = n; }
    public String getDetails()             { return details; }
    public void setDetails(String d)       { this.details = d; }
    public Instant getCreatedAt()          { return createdAt; }
}
