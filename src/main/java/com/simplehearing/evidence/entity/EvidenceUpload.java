package com.simplehearing.evidence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A direct-to-storage video upload in flight — see migration 121. Becomes a {@link GoalEvidence} on completion. */
@Entity
@Table(name = "evidence_uploads")
public class EvidenceUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "stored_url", nullable = false, length = 1000)
    private String storedUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EvidenceUpload() {}

    public UUID getId()                       { return id; }
    public UUID getOrgId()                    { return orgId; }
    public void setOrgId(UUID v)              { this.orgId = v; }
    public UUID getPatientId()                { return patientId; }
    public void setPatientId(UUID v)          { this.patientId = v; }
    public UUID getUploadedBy()               { return uploadedBy; }
    public void setUploadedBy(UUID v)         { this.uploadedBy = v; }
    public UUID getGoalId()                   { return goalId; }
    public void setGoalId(UUID v)             { this.goalId = v; }
    public UUID getSessionId()                { return sessionId; }
    public void setSessionId(UUID v)          { this.sessionId = v; }
    public String getStoredUrl()              { return storedUrl; }
    public void setStoredUrl(String v)        { this.storedUrl = v; }
    public String getFileName()               { return fileName; }
    public void setFileName(String v)         { this.fileName = v; }
    public String getContentType()            { return contentType; }
    public void setContentType(String v)      { this.contentType = v; }
    public long getSizeBytes()                { return sizeBytes; }
    public void setSizeBytes(long v)          { this.sizeBytes = v; }
    public Integer getDurationSeconds()       { return durationSeconds; }
    public void setDurationSeconds(Integer v) { this.durationSeconds = v; }
    public String getNote()                   { return note; }
    public void setNote(String v)             { this.note = v; }
    public Instant getExpiresAt()             { return expiresAt; }
    public void setExpiresAt(Instant v)       { this.expiresAt = v; }
    public Instant getCreatedAt()             { return createdAt; }
}
