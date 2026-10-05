package com.simplehearing.evidence.entity;

import com.simplehearing.evidence.enums.EvidenceKind;
import com.simplehearing.evidence.enums.EvidenceReason;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One piece of IEP goal evidence — a video, or a recorded reason a video couldn't be uploaded. */
@Entity
@Table(name = "goal_evidence")
public class GoalEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "session_id")
    private UUID sessionId;

    /** The therapy the goal's plan was linked to when this was recorded (null if unlinked/ad hoc). */
    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    /** Who recorded it. */
    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private EvidenceKind kind;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 40)
    private EvidenceReason reasonCode;

    @Column(name = "reason_text", columnDefinition = "TEXT")
    private String reasonText;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GoalEvidence() {}

    public UUID getId()                         { return id; }
    public UUID getOrgId()                      { return orgId; }
    public void setOrgId(UUID v)                { this.orgId = v; }
    public UUID getPatientId()                  { return patientId; }
    public void setPatientId(UUID v)            { this.patientId = v; }
    public UUID getPlanId()                     { return planId; }
    public void setPlanId(UUID v)               { this.planId = v; }
    public UUID getGoalId()                     { return goalId; }
    public void setGoalId(UUID v)               { this.goalId = v; }
    public UUID getSessionId()                  { return sessionId; }
    public void setSessionId(UUID v)            { this.sessionId = v; }
    public UUID getEnrollmentId()               { return enrollmentId; }
    public void setEnrollmentId(UUID v)         { this.enrollmentId = v; }
    public UUID getTherapistId()                { return therapistId; }
    public void setTherapistId(UUID v)          { this.therapistId = v; }
    public EvidenceKind getKind()               { return kind; }
    public void setKind(EvidenceKind v)         { this.kind = v; }
    public String getFileName()                 { return fileName; }
    public void setFileName(String v)           { this.fileName = v; }
    public String getFileUrl()                  { return fileUrl; }
    public void setFileUrl(String v)            { this.fileUrl = v; }
    public String getContentType()              { return contentType; }
    public void setContentType(String v)        { this.contentType = v; }
    public Long getFileSizeBytes()              { return fileSizeBytes; }
    public void setFileSizeBytes(Long v)        { this.fileSizeBytes = v; }
    public Integer getDurationSeconds()         { return durationSeconds; }
    public void setDurationSeconds(Integer v)   { this.durationSeconds = v; }
    public EvidenceReason getReasonCode()       { return reasonCode; }
    public void setReasonCode(EvidenceReason v) { this.reasonCode = v; }
    public String getReasonText()               { return reasonText; }
    public void setReasonText(String v)         { this.reasonText = v; }
    public String getNote()                     { return note; }
    public void setNote(String v)               { this.note = v; }
    public Instant getCreatedAt()               { return createdAt; }
}
