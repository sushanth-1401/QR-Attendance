package com.attendance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for device registration requests from students.
 */
public class DeviceRegistrationRequest {

    @NotBlank(message = "Public key is required")
    private String publicKey;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    private String keyAlgorithm;

    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(String publicKey, String deviceName, String keyAlgorithm) {
        this.publicKey = publicKey;
        this.deviceName = deviceName;
        this.keyAlgorithm = keyAlgorithm;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }
}
