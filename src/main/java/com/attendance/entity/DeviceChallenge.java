package com.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_challenges")
public class DeviceChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long challengeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private RegisteredDevice device;

    @Column(name = "challenge_value", nullable = false, length = 255)
    private String challengeValue;

    @Column(name = "challenge_hash", nullable = false, length = 255)
    private String challengeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed", nullable = false)
    private Boolean consumed = false;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeviceChallenge() {
    }

    public DeviceChallenge(RegisteredDevice device, String challengeValue, String challengeHash,
                            LocalDateTime expiresAt) {
        this.device = device;
        this.challengeValue = challengeValue;
        this.challengeHash = challengeHash;
        this.expiresAt = expiresAt;
        this.consumed = false;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public RegisteredDevice getDevice() {
        return device;
    }

    public void setDevice(RegisteredDevice device) {
        this.device = device;
    }

    public String getChallengeValue() {
        return challengeValue;
    }

    public void setChallengeValue(String challengeValue) {
        this.challengeValue = challengeValue;
    }

    public String getChallengeHash() {
        return challengeHash;
    }

    public void setChallengeHash(String challengeHash) {
        this.challengeHash = challengeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getConsumed() {
        return consumed;
    }

    public void setConsumed(Boolean consumed) {
        this.consumed = consumed;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DeviceChallenge{" +
                "challengeId=" + challengeId +
                ", expiresAt=" + expiresAt +
                ", consumed=" + consumed +
                '}';
    }
}
