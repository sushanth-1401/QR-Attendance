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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_attempts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"request_id"})
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AttendanceAttempt {

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private RegisteredDevice device;

    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private QRToken qrToken;

    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AttendanceAttempt() {
    }

    public AttendanceAttempt(StudentProfile student, AttendanceSession session, String requestId,
                              LocalDateTime scanTime) {
        this.student = student;
        this.session = session;
        this.requestId = requestId;
        this.scanTime = scanTime;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public AttendanceAttempt(StudentProfile student, AttendanceSession session, String requestId,
                             QRToken qrToken, LocalDateTime scanTime, LocalDateTime reservationExpiresAt) {
        this.student = student;
        this.session = session;
        this.requestId = requestId;
        this.qrToken = qrToken;
        this.scanTime = scanTime;
        this.reservationExpiresAt = reservationExpiresAt;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
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

    public RegisteredDevice getDevice() {
        return device;
    }

    public void setDevice(RegisteredDevice device) {
        this.device = device;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public QRToken getQrToken() {
        return qrToken;
    }

    public void setQrToken(QRToken qrToken) {
        this.qrToken = qrToken;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    public LocalDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AttendanceAttempt{" +
                "attemptId=" + attemptId +
                ", requestId='" + requestId + '\'' +
                ", scanTime=" + scanTime +
                ", verificationStatus=" + verificationStatus +
                '}';
    }
}
