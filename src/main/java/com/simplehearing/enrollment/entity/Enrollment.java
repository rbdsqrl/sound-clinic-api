package com.simplehearing.enrollment.entity;

import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "session_duration_minutes", nullable = false)
    private int sessionDurationMinutes;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Last day of the therapy plan. Derived from the session count at creation time when
     * the caller doesn't supply one — review meetings are scheduled inside this window.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Derived from start_date at creation time. Nullable — sessions are now daily. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    /** Clinical-health signal set by the assigned therapist or an admin-tier role, while the enrollment is ACTIVE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "care_status", nullable = false)
    private EnrollmentCareStatus careStatus = EnrollmentCareStatus.ON_TRACK;

    @Column(name = "care_status_note", columnDefinition = "TEXT")
    private String careStatusNote;

    @Column(name = "care_status_updated_by")
    private UUID careStatusUpdatedBy;

    @Column(name = "care_status_updated_at")
    private Instant careStatusUpdatedAt;

    /** One of the three discharge success criteria — the assigned therapist confirming this program's goals were met. */
    @Column(name = "therapist_signed_off", nullable = false)
    private boolean therapistSignedOff = false;

    @Column(name = "therapist_signoff_by")
    private UUID therapistSignoffBy;

    @Column(name = "therapist_signoff_at")
    private Instant therapistSignoffAt;

    @Column(name = "therapist_signoff_notes", columnDefinition = "TEXT")
    private String therapistSignoffNotes;

    /** Which discharge episode closed this enrollment. NULL = belongs to the patient's current, still-open episode. */
    @Column(name = "discharged_in_record_id")
    private UUID dischargedInRecordId;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Enrollment() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }
    public int getSessionDurationMinutes() { return sessionDurationMinutes; }
    public void setSessionDurationMinutes(int sessionDurationMinutes) { this.sessionDurationMinutes = sessionDurationMinutes; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
    public EnrollmentCareStatus getCareStatus() { return careStatus; }
    public void setCareStatus(EnrollmentCareStatus careStatus) { this.careStatus = careStatus; }
    public String getCareStatusNote() { return careStatusNote; }
    public void setCareStatusNote(String careStatusNote) { this.careStatusNote = careStatusNote; }
    public UUID getCareStatusUpdatedBy() { return careStatusUpdatedBy; }
    public void setCareStatusUpdatedBy(UUID careStatusUpdatedBy) { this.careStatusUpdatedBy = careStatusUpdatedBy; }
    public Instant getCareStatusUpdatedAt() { return careStatusUpdatedAt; }
    public void setCareStatusUpdatedAt(Instant careStatusUpdatedAt) { this.careStatusUpdatedAt = careStatusUpdatedAt; }
    public boolean isTherapistSignedOff() { return therapistSignedOff; }
    public void setTherapistSignedOff(boolean therapistSignedOff) { this.therapistSignedOff = therapistSignedOff; }
    public UUID getTherapistSignoffBy() { return therapistSignoffBy; }
    public void setTherapistSignoffBy(UUID therapistSignoffBy) { this.therapistSignoffBy = therapistSignoffBy; }
    public Instant getTherapistSignoffAt() { return therapistSignoffAt; }
    public void setTherapistSignoffAt(Instant therapistSignoffAt) { this.therapistSignoffAt = therapistSignoffAt; }
    public String getTherapistSignoffNotes() { return therapistSignoffNotes; }
    public void setTherapistSignoffNotes(String therapistSignoffNotes) { this.therapistSignoffNotes = therapistSignoffNotes; }
    public UUID getDischargedInRecordId() { return dischargedInRecordId; }
    public void setDischargedInRecordId(UUID dischargedInRecordId) { this.dischargedInRecordId = dischargedInRecordId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
