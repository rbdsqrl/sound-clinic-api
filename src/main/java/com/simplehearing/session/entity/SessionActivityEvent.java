package com.simplehearing.session.entity;

import com.simplehearing.session.enums.SessionActivityType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One entry in a session's Activity Log — see migration 117. */
@Entity
@Table(name = "session_activity_events")
public class SessionActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /** Null when the system acted (e.g. the automatic cancellation of a missed reschedule). */
    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private SessionActivityType eventType;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    /** JSON list of {field, from, to}. */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SessionActivityEvent() {}

    public UUID getId()                               { return id; }
    public UUID getOrgId()                            { return orgId; }
    public void setOrgId(UUID v)                      { this.orgId = v; }
    public UUID getSessionId()                        { return sessionId; }
    public void setSessionId(UUID v)                  { this.sessionId = v; }
    public UUID getActorId()                          { return actorId; }
    public void setActorId(UUID v)                    { this.actorId = v; }
    public SessionActivityType getEventType()         { return eventType; }
    public void setEventType(SessionActivityType v)   { this.eventType = v; }
    public String getSummary()                        { return summary; }
    public void setSummary(String v)                  { this.summary = v; }
    public String getChanges()                        { return changes; }
    public void setChanges(String v)                  { this.changes = v; }
    public Instant getCreatedAt()                     { return createdAt; }
}
