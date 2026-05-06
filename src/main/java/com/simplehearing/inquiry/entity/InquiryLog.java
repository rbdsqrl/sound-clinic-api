package com.simplehearing.inquiry.entity;

import com.simplehearing.inquiry.enums.InquiryLogType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inquiry_logs")
public class InquiryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false)
    private InquiryLogType logType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InquiryLog() {}

    public UUID getId() { return id; }

    public UUID getInquiryId() { return inquiryId; }
    public void setInquiryId(UUID inquiryId) { this.inquiryId = inquiryId; }

    public InquiryLogType getLogType() { return logType; }
    public void setLogType(InquiryLogType logType) { this.logType = logType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Instant getCreatedAt() { return createdAt; }
}
