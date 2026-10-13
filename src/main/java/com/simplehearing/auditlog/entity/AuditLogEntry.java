package com.simplehearing.auditlog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One entry in a member's, parent's or case's activity log — see migration 125. */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    /** USER (a member or a parent) or CASE (a patient). */
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    /** Null when the system acted. */
    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    /** JSON list of {field, from, to}. */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLogEntry() {}

    public UUID getId()                  { return id; }
    public UUID getOrgId()               { return orgId; }
    public void setOrgId(UUID v)         { this.orgId = v; }
    public String getSubjectType()       { return subjectType; }
    public void setSubjectType(String v) { this.subjectType = v; }
    public UUID getSubjectId()           { return subjectId; }
    public void setSubjectId(UUID v)     { this.subjectId = v; }
    public UUID getActorId()             { return actorId; }
    public void setActorId(UUID v)       { this.actorId = v; }
    public String getEventType()         { return eventType; }
    public void setEventType(String v)   { this.eventType = v; }
    public String getSummary()           { return summary; }
    public void setSummary(String v)     { this.summary = v; }
    public String getChanges()           { return changes; }
    public void setChanges(String v)     { this.changes = v; }
    public Instant getCreatedAt()        { return createdAt; }
}
