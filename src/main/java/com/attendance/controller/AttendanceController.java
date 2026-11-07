package com.attendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.attendance.dto.AttendanceResponse;
import com.attendance.dto.CompleteVerificationRequest;
import com.attendance.dto.QRScanRequest;
import com.attendance.entity.StudentProfile;
import com.attendance.exception.AttendanceException;
import com.attendance.service.AttendanceService;
import com.attendance.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student/attendance")
@PreAuthorize("hasRole('STUDENT')")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    private StudentProfile getAuthenticatedStudent() {
        return authService.getCurrentStudentProfile()
                .orElseThrow(() -> new AttendanceException("STUDENT_PROFILE_MISSING", "Student profile not found for user"));
    }

    /**
     * Endpoint 1: Student scans QR code. Accepts token + requestId, reserves an attempt slot.
     */
    @PostMapping("/scan")
    public ResponseEntity<AttendanceResponse> scanQR(@Valid @RequestBody QRScanRequest request) {
        StudentProfile student = getAuthenticatedStudent();
        AttendanceResponse response = attendanceService.processScan(
                student.getStudentId(), request.getToken(), request.getRequestId());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 2: Student submits GPS location and cryptographic device signature to complete attendance.
     */
    @PostMapping("/{attemptId}/verify")
    public ResponseEntity<AttendanceResponse> verifyAttendance(@PathVariable("attemptId") Long attemptId,
                                                               @RequestBody CompleteVerificationRequest request) {
        StudentProfile student = getAuthenticatedStudent();
        AttendanceResponse response = attendanceService.completeVerification(
                attemptId,
                student.getStudentId(),
                request.getLocation(),
                request.getDeviceId(),
                request.getSignature(),
                request.getChallengeId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 3: Check status of an attendance attempt reservation.
     */
    @GetMapping("/{attemptId}/status")
    public ResponseEntity<AttendanceResponse> getAttemptStatus(@PathVariable("attemptId") Long attemptId) {
        StudentProfile student = getAuthenticatedStudent();
        AttendanceResponse response = attendanceService.getAttemptStatus(attemptId, student.getStudentId());
        return ResponseEntity.ok(response);
    }
}

