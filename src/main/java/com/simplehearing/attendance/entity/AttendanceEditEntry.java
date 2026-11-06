package com.simplehearing.attendance.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One manual correction of an attendance record: who made it, when, and what changed (a JSON list of field / from / to). */
@Entity
@Table(name = "attendance_edit_history")
public class AttendanceEditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "attendance_id", nullable = false)
    private UUID attendanceId;

    @Column(name = "edited_by")
    private UUID editedBy;

    @CreationTimestamp
    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "changes", nullable = false, columnDefinition = "TEXT")
    private String changes;

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID v) { this.orgId = v; }
    public UUID getAttendanceId() { return attendanceId; }
    public void setAttendanceId(UUID v) { this.attendanceId = v; }
    public UUID getEditedBy() { return editedBy; }
    public void setEditedBy(UUID v) { this.editedBy = v; }
    public Instant getEditedAt() { return editedAt; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getChanges() { return changes; }
    public void setChanges(String v) { this.changes = v; }
}
