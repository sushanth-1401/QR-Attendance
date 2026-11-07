package com.attendance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AttendanceException extends RuntimeException {

    private final String errorCode;

    public AttendanceException(String message) {
        super(message);
        this.errorCode = "ATTENDANCE_ERROR";
    }

    public AttendanceException(String param1, String param2) {
        super(resolveMessage(param1, param2));
        this.errorCode = resolveErrorCode(param1, param2);
    }

    private static String resolveMessage(String param1, String param2) {
        if (param1 != null && param1.matches("^[A-Z0-9_]+$") && param2 != null && param2.contains(" ")) {
            return param2;
        }
        return param1;
    }

    private static String resolveErrorCode(String param1, String param2) {
        if (param1 != null && param1.matches("^[A-Z0-9_]+$") && param2 != null && param2.contains(" ")) {
            return param1;
        }
        return param2 != null ? param2 : "ATTENDANCE_ERROR";
    }

    public AttendanceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
