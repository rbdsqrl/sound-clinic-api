package com.simplehearing.activity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_resources")
public class ActivityResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ActivityResource() {}

    public UUID getId()                          { return id; }
    public UUID getOrgId()                       { return orgId; }
    public void setOrgId(UUID orgId)             { this.orgId = orgId; }
    public UUID getActivityId()                  { return activityId; }
    public void setActivityId(UUID activityId)   { this.activityId = activityId; }
    public UUID getUploadedBy()                  { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy)   { this.uploadedBy = uploadedBy; }
    public String getFileName()                  { return fileName; }
    public void setFileName(String fileName)     { this.fileName = fileName; }
    public String getFileUrl()                   { return fileUrl; }
    public void setFileUrl(String fileUrl)       { this.fileUrl = fileUrl; }
    public String getContentType()               { return contentType; }
    public void setContentType(String ct)        { this.contentType = ct; }
    public Long getFileSizeBytes()               { return fileSizeBytes; }
    public void setFileSizeBytes(Long size)      { this.fileSizeBytes = size; }
    public Instant getCreatedAt()                { return createdAt; }
}
