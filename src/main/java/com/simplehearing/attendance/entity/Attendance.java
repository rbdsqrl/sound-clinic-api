package com.simplehearing.attendance.entity;

import com.simplehearing.attendance.enums.AttendanceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "check_in_lat", precision = 10, scale = 8)
    private Double checkInLat;

    @Column(name = "check_in_lon", precision = 11, scale = 8)
    private Double checkInLon;

    @Column(name = "check_out_lat", precision = 10, scale = 8)
    private Double checkOutLat;

    @Column(name = "check_out_lon", precision = 11, scale = 8)
    private Double checkOutLon;

    @Column(name = "geo_verified", nullable = false)
    private boolean geoVerified = false;

    @Column(name = "face_verified", nullable = false)
    private boolean faceVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.CHECKED_IN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Attendance() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getClinicId() { return clinicId; }
    public void setClinicId(UUID clinicId) { this.clinicId = clinicId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public Instant getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Instant checkInTime) { this.checkInTime = checkInTime; }
    public Instant getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(Instant checkOutTime) { this.checkOutTime = checkOutTime; }
    public Double getCheckInLat() { return checkInLat; }
    public void setCheckInLat(Double checkInLat) { this.checkInLat = checkInLat; }
    public Double getCheckInLon() { return checkInLon; }
    public void setCheckInLon(Double checkInLon) { this.checkInLon = checkInLon; }
    public Double getCheckOutLat() { return checkOutLat; }
    public void setCheckOutLat(Double checkOutLat) { this.checkOutLat = checkOutLat; }
    public Double getCheckOutLon() { return checkOutLon; }
    public void setCheckOutLon(Double checkOutLon) { this.checkOutLon = checkOutLon; }
    public boolean isGeoVerified() { return geoVerified; }
    public void setGeoVerified(boolean geoVerified) { this.geoVerified = geoVerified; }
    public boolean isFaceVerified() { return faceVerified; }
    public void setFaceVerified(boolean faceVerified) { this.faceVerified = faceVerified; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
