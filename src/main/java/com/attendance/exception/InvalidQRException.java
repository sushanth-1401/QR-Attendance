package com.attendance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidQRException extends RuntimeException {

    public InvalidQRException(String message) {
        super(message);
    }

    public InvalidQRException(String message, Throwable cause) {
        super(message, cause);
    }
}
