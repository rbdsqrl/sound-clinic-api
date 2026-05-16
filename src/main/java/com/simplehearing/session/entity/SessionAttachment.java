package com.simplehearing.session.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_attachments")
public class SessionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SessionAttachment() {}

    public UUID getId()                        { return id; }
    public UUID getOrgId()                     { return orgId; }
    public void setOrgId(UUID v)               { this.orgId = v; }
    public UUID getSessionId()                 { return sessionId; }
    public void setSessionId(UUID v)           { this.sessionId = v; }
    public UUID getTherapistId()               { return therapistId; }
    public void setTherapistId(UUID v)         { this.therapistId = v; }
    public String getFileName()                { return fileName; }
    public void setFileName(String v)          { this.fileName = v; }
    public String getFileUrl()                 { return fileUrl; }
    public void setFileUrl(String v)           { this.fileUrl = v; }
    public String getContentType()             { return contentType; }
    public void setContentType(String v)       { this.contentType = v; }
    public Long getFileSizeBytes()             { return fileSizeBytes; }
    public void setFileSizeBytes(Long v)       { this.fileSizeBytes = v; }
    public Instant getCreatedAt()              { return createdAt; }
}
