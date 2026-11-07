package com.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for device verification requests containing a signed challenge.
 */
public class DeviceVerificationRequest {

    @NotNull(message = "Challenge ID is required")
    private Long challengeId;

    @NotBlank(message = "Signature is required")
    private String signature;

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    public DeviceVerificationRequest() {
    }

    public DeviceVerificationRequest(Long challengeId, String signature, Long deviceId) {
        this.challengeId = challengeId;
        this.signature = signature;
        this.deviceId = deviceId;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
}
