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

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs")
public class VerificationLog {

    public enum VerificationType {
        QR, GPS, DEVICE, ENROLLMENT, DUPLICATE, RESERVATION
    }

    public enum VerificationResult {
        PASSED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AttendanceAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_result", nullable = false, length = 20)
    private VerificationResult verificationResult;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public VerificationLog() {
    }

    public VerificationLog(AttendanceAttempt attempt, VerificationType verificationType,
                            VerificationResult verificationResult, String details) {
        this.attempt = attempt;
        this.verificationType = verificationType;
        this.verificationResult = verificationResult;
        this.details = details;
        this.checkedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = LocalDateTime.now();
        }
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public AttendanceAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(AttendanceAttempt attempt) {
        this.attempt = attempt;
    }

    public VerificationType getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(VerificationType verificationType) {
        this.verificationType = verificationType;
    }

    public VerificationResult getVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(VerificationResult verificationResult) {
        this.verificationResult = verificationResult;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    @Override
    public String toString() {
        return "VerificationLog{" +
                "logId=" + logId +
                ", verificationType=" + verificationType +
                ", verificationResult=" + verificationResult +
                ", checkedAt=" + checkedAt +
                '}';
    }
}
