package com.attendance.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler that returns JSON for API requests and
 * redirects to error pages for web requests.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── ResourceNotFoundException → 404 ───────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
            body.put("entityName", ex.getEntityName());
            body.put("fieldName", ex.getFieldName());
            body.put("fieldValue", ex.getFieldValue());
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    // ── InvalidQRException → 400 ─────────────────────────────────────────

    @ExceptionHandler(InvalidQRException.class)
    public Object handleInvalidQR(InvalidQRException ex, HttpServletRequest request) {
        log.warn("Invalid QR: {}", ex.getMessage());

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
            body.put("errorType", "INVALID_QR");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    // ── AttendanceException → 400 ─────────────────────────────────────────

    @ExceptionHandler(AttendanceException.class)
    public Object handleAttendanceException(AttendanceException ex, HttpServletRequest request) {
        log.warn("Attendance error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
            body.put("errorCode", ex.getErrorCode());
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    // ── AccessDeniedException → 403 ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for URI {}: {}", request.getRequestURI(), ex.getMessage());

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
            return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
        }

        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", "You do not have permission to access this resource.");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    // ── MethodArgumentNotValidException → 400 with field errors ───────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
            body.put("fieldErrors", fieldErrors);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "Validation failed");
        mav.addObject("fieldErrors", fieldErrors);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    // ── DataIntegrityViolationException → 409 ─────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        String message = "A data conflict occurred. The resource may already exist.";

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.CONFLICT, message, request.getRequestURI());
            return new ResponseEntity<>(body, HttpStatus.CONFLICT);
        }

        ModelAndView mav = new ModelAndView("error/409");
        mav.addObject("message", message);
        mav.setStatus(HttpStatus.CONFLICT);
        return mav;
    }

    // ── NoResourceFoundException → 404 (for missing static assets like favicon) ───

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Object handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return new ResponseEntity<>(buildErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()), HttpStatus.NOT_FOUND);
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = "An unexpected error occurred. Please try again later.";

        if (isApiRequest(request)) {
            Map<String, Object> body = buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getRequestURI());
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", message);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json"));
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
