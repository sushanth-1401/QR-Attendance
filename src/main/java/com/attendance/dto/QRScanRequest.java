package com.attendance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for QR code scan requests submitted by students.
 */
public class QRScanRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Request ID is required")
    private String requestId;

    public QRScanRequest() {
    }

    public QRScanRequest(String token, String requestId) {
        this.token = token;
        this.requestId = requestId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
