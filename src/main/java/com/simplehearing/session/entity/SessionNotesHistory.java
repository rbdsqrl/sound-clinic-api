package com.simplehearing.session.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One row per edit of a session's feedback/progress report/notes/performance score —
 *  captures what those fields held right before the edit that overwrote them. */
@Entity
@Table(name = "session_notes_history")
public class SessionNotesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "previous_feedback", columnDefinition = "TEXT")
    private String previousFeedback;

    @Column(name = "previous_progress_report", columnDefinition = "TEXT")
    private String previousProgressReport;

    @Column(name = "previous_notes", columnDefinition = "TEXT")
    private String previousNotes;

    @Column(name = "previous_performance_score")
    private Integer previousPerformanceScore;

    public SessionNotesHistory() {}

    public UUID getId()                                    { return id; }
    public UUID getOrgId()                                 { return orgId; }
    public void setOrgId(UUID v)                           { this.orgId = v; }
    public UUID getSessionId()                             { return sessionId; }
    public void setSessionId(UUID v)                       { this.sessionId = v; }
    public UUID getChangedBy()                             { return changedBy; }
    public void setChangedBy(UUID v)                       { this.changedBy = v; }
    public Instant getChangedAt()                          { return changedAt; }
    public void setChangedAt(Instant v)                    { this.changedAt = v; }
    public String getPreviousFeedback()                    { return previousFeedback; }
    public void setPreviousFeedback(String v)              { this.previousFeedback = v; }
    public String getPreviousProgressReport()              { return previousProgressReport; }
    public void setPreviousProgressReport(String v)        { this.previousProgressReport = v; }
    public String getPreviousNotes()                       { return previousNotes; }
    public void setPreviousNotes(String v)                 { this.previousNotes = v; }
    public Integer getPreviousPerformanceScore()           { return previousPerformanceScore; }
    public void setPreviousPerformanceScore(Integer v)     { this.previousPerformanceScore = v; }
}
