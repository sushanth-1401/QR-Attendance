package com.attendance.dto;

public class CompleteVerificationRequest {

    private LocationRequest location;
    private Long deviceId;
    private String signature;
    private Long challengeId;

    public CompleteVerificationRequest() {
    }

    public CompleteVerificationRequest(LocationRequest location, Long deviceId, String signature, Long challengeId) {
        this.location = location;
        this.deviceId = deviceId;
        this.signature = signature;
        this.challengeId = challengeId;
    }

    public LocationRequest getLocation() {
        return location;
    }

    public void setLocation(LocationRequest location) {
        this.location = location;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }
}

