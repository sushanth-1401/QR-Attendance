package com.attendance.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.attendance.dto.DeviceChallengeResponse;
import com.attendance.dto.DeviceRegistrationRequest;
import com.attendance.dto.DeviceVerificationRequest;
import com.attendance.entity.RegisteredDevice;
import com.attendance.entity.StudentProfile;
import com.attendance.exception.AttendanceException;
import com.attendance.service.AuthService;
import com.attendance.service.DeviceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student/devices")
@PreAuthorize("hasRole('STUDENT')")
public class DeviceController {

    private final DeviceService deviceService;
    private final AuthService authService;

    public DeviceController(DeviceService deviceService, AuthService authService) {
        this.deviceService = deviceService;
        this.authService = authService;
    }

    private StudentProfile getAuthenticatedStudent() {
        return authService.getCurrentStudentProfile()
                .orElseThrow(() -> new AttendanceException("STUDENT_PROFILE_MISSING", "Student profile not found for user"));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredDevice> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        StudentProfile student = getAuthenticatedStudent();
        RegisteredDevice device = deviceService.registerDevice(student, request);
        return ResponseEntity.ok(device);
    }

    @PostMapping("/challenge")
    public ResponseEntity<DeviceChallengeResponse> getChallenge(@RequestBody Map<String, Long> req) {
        StudentProfile student = getAuthenticatedStudent();
        Long deviceId = req.get("deviceId");
        if (deviceId == null) {
            throw new AttendanceException("DEVICE_ID_REQUIRED", "deviceId is required");
        }
        DeviceChallengeResponse challenge = deviceService.generateChallenge(deviceId, student.getStudentId());
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifySignature(@Valid @RequestBody DeviceVerificationRequest request) {
        StudentProfile student = getAuthenticatedStudent();
        boolean verified = deviceService.verifyChallenge(
                request.getChallengeId(), request.getDeviceId(), request.getSignature());
        return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Device signature verified successfully" : "Signature verification failed"
        ));
    }

    @GetMapping
    public ResponseEntity<List<RegisteredDevice>> getMyDevices() {
        StudentProfile student = getAuthenticatedStudent();
        return ResponseEntity.ok(deviceService.getStudentDevices(student.getStudentId()));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> revokeDevice(@PathVariable("deviceId") Long deviceId,
                                                            @RequestParam(value = "reason", required = false) String reason) {
        StudentProfile student = getAuthenticatedStudent();
        deviceService.revokeDevice(deviceId, student.getStudentId(), reason != null ? reason : "Revoked by student");
        return ResponseEntity.ok(Map.of("success", true, "message", "Device revoked successfully"));
    }
}

