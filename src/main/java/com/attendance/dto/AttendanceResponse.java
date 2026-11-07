package com.attendance.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for attendance operation responses returned to the client.
 */
public class AttendanceResponse {

    private boolean success;
    private String message;
    private String status;
    private Long attemptId;
    private Long attendanceId;
    private Map<String, Object> details;

    public AttendanceResponse() {
        this.details = new HashMap<>();
    }

    public AttendanceResponse(boolean success, String message, String status,
                              Long attemptId, Long attendanceId, Map<String, Object> details) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.attemptId = attemptId;
        this.attendanceId = attendanceId;
        this.details = details != null ? details : new HashMap<>();
    }

    /**
     * Factory method for a successful attendance response.
     */
    public static AttendanceResponse success(String message, Long attemptId, Long attendanceId) {
        return new AttendanceResponse(true, message, "SUCCESS", attemptId, attendanceId, null);
    }

    /**
     * Factory method for a failed attendance response.
     */
    public static AttendanceResponse failure(String message, String status) {
        return new AttendanceResponse(false, message, status, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}
