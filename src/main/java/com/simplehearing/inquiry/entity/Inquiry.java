package com.simplehearing.inquiry.entity;

import com.simplehearing.inquiry.enums.InquiryStatus;
import com.simplehearing.inquiry.enums.PreferredTime;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inquiries")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time")
    private PreferredTime preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "appointment_date")
    private Instant appointmentDate;

    @Column(name = "appointment_notes", columnDefinition = "TEXT")
    private String appointmentNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inquiry() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public PreferredTime getPreferredTime() { return preferredTime; }
    public void setPreferredTime(PreferredTime preferredTime) { this.preferredTime = preferredTime; }
    public InquiryStatus getStatus() { return status; }
    public void setStatus(InquiryStatus status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public Instant getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Instant appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getAppointmentNotes() { return appointmentNotes; }
    public void setAppointmentNotes(String appointmentNotes) { this.appointmentNotes = appointmentNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
