package com.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "session_id"})
})
public class AttendanceRecord {

    public enum AttendanceStatus {
        PRESENT, ABSENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AttendanceAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "gps_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal gpsAccuracyMeters;

    @Column(name = "distance_from_classroom", precision = 10, scale = 2)
    private BigDecimal distanceFromClassroom;

    @Column(name = "verification_method", length = 50)
    private String verificationMethod;

    @Column(name = "manual_override", nullable = false)
    private Boolean manualOverride = false;

    @Column(name = "override_reason", length = 255)
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_by")
    private User overrideBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AttendanceRecord() {
    }

    public AttendanceRecord(StudentProfile student, AttendanceSession session,
                             AttendanceStatus attendanceStatus, LocalDateTime markedAt) {
        this.student = student;
        this.session = session;
        this.attendanceStatus = attendanceStatus;
        this.markedAt = markedAt;
        this.manualOverride = false;
    }

    public AttendanceRecord(StudentProfile student, AttendanceSession session, AttendanceAttempt attempt,
                            AttendanceStatus attendanceStatus, LocalDateTime markedAt) {
        this.student = student;
        this.session = session;
        this.attempt = attempt;
        this.attendanceStatus = attendanceStatus;
        this.markedAt = markedAt;
        this.manualOverride = false;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
    }

    public AttendanceAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(AttendanceAttempt attempt) {
        this.attempt = attempt;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getGpsAccuracyMeters() {
        return gpsAccuracyMeters;
    }

    public void setGpsAccuracyMeters(BigDecimal gpsAccuracyMeters) {
        this.gpsAccuracyMeters = gpsAccuracyMeters;
    }

    public BigDecimal getDistanceFromClassroom() {
        return distanceFromClassroom;
    }

    public void setDistanceFromClassroom(BigDecimal distanceFromClassroom) {
        this.distanceFromClassroom = distanceFromClassroom;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public Boolean getManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(Boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public User getOverrideBy() {
        return overrideBy;
    }

    public void setOverrideBy(User overrideBy) {
        this.overrideBy = overrideBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AttendanceRecord{" +
                "attendanceId=" + attendanceId +
                ", attendanceStatus=" + attendanceStatus +
                ", markedAt=" + markedAt +
                ", manualOverride=" + manualOverride +
                '}';
    }
}
