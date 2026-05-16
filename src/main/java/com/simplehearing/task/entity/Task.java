package com.simplehearing.task.entity;

import com.simplehearing.task.enums.TaskPriority;
import com.simplehearing.task.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId()                        { return id; }
    public UUID getOrgId()                     { return orgId; }
    public void setOrgId(UUID orgId)           { this.orgId = orgId; }
    public String getTitle()                   { return title; }
    public void setTitle(String title)         { this.title = title; }
    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }
    public UUID getAssignedTo()                { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
    public UUID getAssignedBy()                { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }
    public LocalDate getDueDate()              { return dueDate; }
    public void setDueDate(LocalDate dueDate)  { this.dueDate = dueDate; }
    public TaskPriority getPriority()          { return priority; }
    public void setPriority(TaskPriority p)    { this.priority = p; }
    public TaskStatus getStatus()              { return status; }
    public void setStatus(TaskStatus status)   { this.status = status; }
    public Instant getCreatedAt()              { return createdAt; }
    public Instant getUpdatedAt()              { return updatedAt; }
}
