package com.simplehearing.sharedmedia.entity;

import com.simplehearing.sharedmedia.enums.SharedMediaDirection;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shared_media")
public class SharedMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private SharedMediaDirection direction;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SharedMedia() {}

    public UUID getId()                                        { return id; }
    public UUID getOrgId()                                     { return orgId; }
    public void setOrgId(UUID v)                               { this.orgId = v; }
    public UUID getPatientId()                                 { return patientId; }
    public void setPatientId(UUID v)                           { this.patientId = v; }
    public UUID getUploadedBy()                                { return uploadedBy; }
    public void setUploadedBy(UUID v)                          { this.uploadedBy = v; }
    public SharedMediaDirection getDirection()                 { return direction; }
    public void setDirection(SharedMediaDirection v)           { this.direction = v; }
    public String getFileName()                                { return fileName; }
    public void setFileName(String v)                          { this.fileName = v; }
    public String getFileUrl()                                 { return fileUrl; }
    public void setFileUrl(String v)                           { this.fileUrl = v; }
    public String getContentType()                             { return contentType; }
    public void setContentType(String v)                       { this.contentType = v; }
    public Long getFileSizeBytes()                             { return fileSizeBytes; }
    public void setFileSizeBytes(Long v)                       { this.fileSizeBytes = v; }
    public String getNote()                                    { return note; }
    public void setNote(String v)                              { this.note = v; }
    public Instant getCreatedAt()                              { return createdAt; }
}
