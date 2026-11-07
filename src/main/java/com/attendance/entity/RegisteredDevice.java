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
@Table(name = "registered_devices")
public class RegisteredDevice {

    public enum DeviceStatus {
        ACTIVE, REVOKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "key_algorithm", length = 50)
    private String keyAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_status", nullable = false, length = 20)
    private DeviceStatus deviceStatus = DeviceStatus.ACTIVE;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", length = 255)
    private String revocationReason;

    public RegisteredDevice() {
    }

    public RegisteredDevice(StudentProfile student, String deviceName, String publicKey,
                             String keyAlgorithm) {
        this.student = student;
        this.deviceName = deviceName;
        this.publicKey = publicKey;
        this.keyAlgorithm = keyAlgorithm;
        this.deviceStatus = DeviceStatus.ACTIVE;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    @Override
    public String toString() {
        return "RegisteredDevice{" +
                "deviceId=" + deviceId +
                ", deviceName='" + deviceName + '\'' +
                ", keyAlgorithm='" + keyAlgorithm + '\'' +
                ", deviceStatus=" + deviceStatus +
                '}';
    }
}
