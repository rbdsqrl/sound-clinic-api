package com.simplehearing.memberdocument.entity;

import com.simplehearing.memberdocument.enums.MemberDocumentCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A file kept on a staff member's record — ID proof, qualification certificate, contract, etc. */
@Entity
@Table(name = "member_documents")
public class MemberDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MemberDocumentCategory category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MemberDocument() {}

    public UUID getId()                                  { return id; }
    public UUID getOrgId()                               { return orgId; }
    public void setOrgId(UUID v)                         { this.orgId = v; }
    public UUID getMemberId()                            { return memberId; }
    public void setMemberId(UUID v)                      { this.memberId = v; }
    public UUID getUploadedBy()                          { return uploadedBy; }
    public void setUploadedBy(UUID v)                    { this.uploadedBy = v; }
    public MemberDocumentCategory getCategory()          { return category; }
    public void setCategory(MemberDocumentCategory v)    { this.category = v; }
    public String getTitle()                             { return title; }
    public void setTitle(String v)                       { this.title = v; }
    public String getFileName()                          { return fileName; }
    public void setFileName(String v)                    { this.fileName = v; }
    public String getFileUrl()                           { return fileUrl; }
    public void setFileUrl(String v)                     { this.fileUrl = v; }
    public String getContentType()                       { return contentType; }
    public void setContentType(String v)                 { this.contentType = v; }
    public Long getFileSizeBytes()                       { return fileSizeBytes; }
    public void setFileSizeBytes(Long v)                 { this.fileSizeBytes = v; }
    public String getNotes()                             { return notes; }
    public void setNotes(String v)                       { this.notes = v; }
    public Instant getCreatedAt()                        { return createdAt; }
}
