package com.simplehearing.session.entity;

import com.simplehearing.session.enums.TherapySessionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "therapy_sessions")
public class TherapySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TherapySessionStatus status = TherapySessionStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "progress_report", columnDefinition = "TEXT")
    private String progressReport;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TherapySession() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }
    public int getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(int sessionNumber) { this.sessionNumber = sessionNumber; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public TherapySessionStatus getStatus() { return status; }
    public void setStatus(TherapySessionStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getProgressReport() { return progressReport; }
    public void setProgressReport(String progressReport) { this.progressReport = progressReport; }
    public UUID getCompletedBy() { return completedBy; }
    public void setCompletedBy(UUID completedBy) { this.completedBy = completedBy; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
