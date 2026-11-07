package com.attendance.dto;

/**
 * DTO for device challenge responses sent to students for device verification.
 */
public class DeviceChallengeResponse {

    private Long challengeId;
    private String challenge;
    private Long deviceId;
    private Long expiresInSeconds;

    public DeviceChallengeResponse() {
    }

    public DeviceChallengeResponse(Long challengeId, String challenge, Long deviceId, Long expiresInSeconds) {
        this.challengeId = challengeId;
        this.challenge = challenge;
        this.deviceId = deviceId;
        this.expiresInSeconds = expiresInSeconds;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
