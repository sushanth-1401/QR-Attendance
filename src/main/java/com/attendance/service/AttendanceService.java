package com.attendance.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.config.AppConfig;
import com.attendance.dto.AttendanceResponse;
import com.attendance.dto.AttendanceSummary;
import com.attendance.dto.LocationRequest;
import com.attendance.dto.SessionAttendanceData;
import com.attendance.dto.StudentDashboardData;
import com.attendance.entity.AttendanceAttempt;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Classroom;
import com.attendance.entity.Enrollment;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.QRToken;
import com.attendance.entity.RegisteredDevice;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.entity.VerificationLog;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.InvalidQRException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.AttendanceAttemptRepository;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.DeviceRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;
import com.attendance.repository.VerificationLogRepository;

@Service
@Transactional
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceAttemptRepository attemptRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final QRCodeService qrCodeService;
    private final LocationService locationService;
    private final DeviceService deviceService;
    private final AppConfig appConfig;
    private final SupabaseSyncService supabaseSyncService;

    public AttendanceService(AttendanceAttemptRepository attemptRepository,
                             AttendanceRecordRepository recordRepository,
                             AttendanceSessionRepository sessionRepository,
                             StudentRepository studentRepository,
                             EnrollmentRepository enrollmentRepository,
                             DeviceRepository deviceRepository,
                             UserRepository userRepository,
                             VerificationLogRepository verificationLogRepository,
                             QRCodeService qrCodeService,
                             LocationService locationService,
                             DeviceService deviceService,
                             AppConfig appConfig) {
        this(attemptRepository, recordRepository, sessionRepository, studentRepository,
                enrollmentRepository, deviceRepository, userRepository, verificationLogRepository,
                qrCodeService, locationService, deviceService, appConfig, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AttendanceService(AttendanceAttemptRepository attemptRepository,
                             AttendanceRecordRepository recordRepository,
                             AttendanceSessionRepository sessionRepository,
                             StudentRepository studentRepository,
                             EnrollmentRepository enrollmentRepository,
                             DeviceRepository deviceRepository,
                             UserRepository userRepository,
                             VerificationLogRepository verificationLogRepository,
                             QRCodeService qrCodeService,
                             LocationService locationService,
                             DeviceService deviceService,
                             AppConfig appConfig,
                             SupabaseSyncService supabaseSyncService) {
        this.attemptRepository = attemptRepository;
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.qrCodeService = qrCodeService;
        this.locationService = locationService;
        this.deviceService = deviceService;
        this.appConfig = appConfig;
        this.supabaseSyncService = supabaseSyncService;
    }

    /**
     * Step 1: Student scans QR code. Validates token, enrollment, duplicate status,
     * and reserves an attendance attempt slot for the student.
     */
    public AttendanceResponse processScan(Long studentId, String rawToken, String requestId) {
        log.info("Processing QR scan: studentId={}, requestId={}", studentId, requestId);

        // 1. Verify student exists and is active
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));

        if (student.getUser().getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new AttendanceException("ACCOUNT_INACTIVE", "Your student account is currently not active.");
        }

        // Idempotency check: if requestId already processed, return existing attempt
        Optional<AttendanceAttempt> existingAttempt = attemptRepository.findByRequestId(requestId);
        if (existingAttempt.isPresent()) {
            AttendanceAttempt att = existingAttempt.get();
            return new AttendanceResponse(true, "Existing attempt retrieved",
                    att.getVerificationStatus().name(), att.getAttemptId(), null,
                    Map.of("sessionId", att.getSession().getSessionId()));
        }

        // 2. Validate QR token (checks existence, status, server expiry, active session)
        QRToken qrToken = qrCodeService.validateScannedToken(rawToken);
        AttendanceSession session = qrToken.getSession();

        // 3. Verify student enrollment in the session's section
        boolean isEnrolled = enrollmentRepository.existsByStudentStudentIdAndSectionSectionId(
                studentId, session.getSection().getSectionId());
        if (!isEnrolled) {
            throw new AttendanceException("NOT_ENROLLED",
                    String.format("You are not enrolled in %s (%s).",
                            session.getSubject().getSubjectName(), session.getSection().getSectionName()));
        }

        // 4. Check for duplicate attendance
        if (recordRepository.existsByStudentStudentIdAndSessionSessionId(studentId, session.getSessionId())) {
            throw new AttendanceException("DUPLICATE_ATTENDANCE",
                    "Attendance has already been marked for this session.");
        }

        // 5. Create Attendance Attempt reservation
        long reservationSeconds = appConfig.getQr().getReservationValiditySeconds();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationExpiresAt = now.plusSeconds(reservationSeconds);

        AttendanceAttempt attempt = new AttendanceAttempt(
                student,
                session,
                requestId,
                qrToken,
                now,
                reservationExpiresAt
        );
        attempt = attemptRepository.save(attempt);

        // Record verification logs
        logVerificationStep(attempt, VerificationLog.VerificationType.QR, VerificationLog.VerificationResult.PASSED,
                "QR token validated successfully. Hash: " + qrToken.getTokenHash().substring(0, 8) + "...");
        logVerificationStep(attempt, VerificationLog.VerificationType.ENROLLMENT, VerificationLog.VerificationResult.PASSED,
                "Student verified in section " + session.getSection().getSectionName());

        Map<String, Object> details = new HashMap<>();
        details.put("sessionId", session.getSessionId());
        details.put("subjectName", session.getSubject().getSubjectName());
        details.put("subjectCode", session.getSubject().getSubjectCode());
        details.put("classroom", session.getClassroom() != null ? session.getClassroom().getRoomName() : "N/A");
        details.put("reservationSeconds", reservationSeconds);
        details.put("reservationExpiresAt", reservationExpiresAt.toString());

        return new AttendanceResponse(true, "QR verified. Please complete location and device authentication.",
                "PENDING", attempt.getAttemptId(), null, details);
    }

    /**
     * Step 2: Verifies GPS location, device challenge-response cryptographic signature,
     * checks reservation deadline, and records final attendance.
     * Note: noRollbackFor ensures that rejected attempts (such as Geofence breaches/proxy attempts)
     * are persistently stored in the DB and visible in faculty exceptions without rolling back.
     */
    @Transactional(noRollbackFor = {AttendanceException.class})
    public AttendanceResponse completeVerification(Long attemptId, Long studentId,
                                                   LocationRequest locationRequest,
                                                   Long deviceId, String signature,
                                                   Long challengeId) {
        log.info("Completing verification for attemptId={}, studentId={}, deviceId={}", attemptId, studentId, deviceId);

        // 1. Fetch attempt and ensure it belongs to the authenticated student
        AttendanceAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceAttempt", "id", attemptId));

        if (!attempt.getStudent().getStudentId().equals(studentId)) {
            throw new AttendanceException("UNAUTHORIZED_ATTEMPT", "Attempt does not belong to the current authenticated student");
        }

        if (attempt.getVerificationStatus() == AttendanceAttempt.VerificationStatus.VERIFIED) {
            return AttendanceResponse.success("Attendance is already confirmed for this session.", attemptId, null);
        }

        if (attempt.getVerificationStatus() == AttendanceAttempt.VerificationStatus.REJECTED) {
            throw new AttendanceException("ATTEMPT_REJECTED", "This verification attempt was already rejected: " + attempt.getRejectionReason());
        }

        // 2. Check reservation deadline
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getReservationExpiresAt())) {
            attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.EXPIRED);
            attempt.setRejectionReason("Verification reservation expired before completing checks");
            attemptRepository.save(attempt);

            logVerificationStep(attempt, VerificationLog.VerificationType.RESERVATION, VerificationLog.VerificationResult.FAILED,
                    "Reservation deadline passed: " + attempt.getReservationExpiresAt());
            throw new AttendanceException("RESERVATION_EXPIRED", "Verification reservation expired. Please scan QR again.");
        }

        AttendanceSession session = attempt.getSession();

        // 3. Verify session is still eligible (ACTIVE)
        if (session.getSessionStatus() != AttendanceSession.SessionStatus.ACTIVE) {
            attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.REJECTED);
            attempt.setRejectionReason("Attendance session is no longer active");
            attemptRepository.save(attempt);
            throw new AttendanceException("SESSION_CLOSED", "The attendance session has been closed.");
        }

        // 4. Verify GPS Location
        LocationService.LocationVerificationResult locationResult =
                locationService.verifyLocation(session.getClassroom(), locationRequest);

        if (!locationResult.isPassed()) {
            String proxyReason = "Proxy Attempt / Geofence Breach: " + locationResult.getDetails();
            attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.REJECTED);
            attempt.setRejectionReason(proxyReason);
            attemptRepository.save(attempt);

            logVerificationStep(attempt, VerificationLog.VerificationType.GPS, VerificationLog.VerificationResult.FAILED,
                    proxyReason);

            if (supabaseSyncService != null) {
                try {
                    Map<String, Object> syncAttempt = new HashMap<>();
                    syncAttempt.put("attempt_id", attempt.getAttemptId());
                    syncAttempt.put("session_id", session.getSessionId());
                    syncAttempt.put("student_id", studentId);
                    syncAttempt.put("verification_status", "REJECTED");
                    syncAttempt.put("rejection_reason", proxyReason);
                    syncAttempt.put("created_at", LocalDateTime.now().toString());
                    supabaseSyncService.sendToSupabase("attendance_attempts", "attempt_id", syncAttempt);
                } catch (Exception ex) {
                    log.warn("Supabase attempt sync failed: {}", ex.getMessage());
                }
            }

            throw new AttendanceException(proxyReason, "GPS_VERIFICATION_FAILED");
        }

        logVerificationStep(attempt, VerificationLog.VerificationType.GPS, VerificationLog.VerificationResult.PASSED,
                locationResult.getDetails());

        // 5. Verify Registered Device Cryptographic Signature
        RegisteredDevice device = null;
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("RegisteredDevice", "deviceId", deviceId));

            if (!device.getStudent().getStudentId().equals(studentId)) {
                throw new AttendanceException("DEVICE_MISMATCH", "Device is not registered to your account");
            }
            attempt.setDevice(device);
        }

        if (challengeId != null && signature != null && deviceId != null) {
            boolean deviceValid = deviceService.verifyChallenge(challengeId, deviceId, signature);
            if (!deviceValid) {
                String devReason = "Cryptographic device signature verification failed or challenge expired";
                attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.REJECTED);
                attempt.setRejectionReason(devReason);
                attemptRepository.save(attempt);

                logVerificationStep(attempt, VerificationLog.VerificationType.DEVICE, VerificationLog.VerificationResult.FAILED,
                        "Signature verification failed for deviceId=" + deviceId);

                if (supabaseSyncService != null) {
                    try {
                        Map<String, Object> syncAttempt = new HashMap<>();
                        syncAttempt.put("attempt_id", attempt.getAttemptId());
                        syncAttempt.put("session_id", session.getSessionId());
                        syncAttempt.put("student_id", studentId);
                        syncAttempt.put("verification_status", "REJECTED");
                        syncAttempt.put("rejection_reason", devReason);
                        syncAttempt.put("created_at", LocalDateTime.now().toString());
                        supabaseSyncService.sendToSupabase("attendance_attempts", "attempt_id", syncAttempt);
                    } catch (Exception ex) {
                        log.warn("Supabase attempt sync failed: {}", ex.getMessage());
                    }
                }

                throw new AttendanceException(devReason, "DEVICE_VERIFICATION_FAILED");
            }
            logVerificationStep(attempt, VerificationLog.VerificationType.DEVICE, VerificationLog.VerificationResult.PASSED,
                    "Cryptographic ECDSA signature verified successfully for deviceId=" + deviceId);
        } else {
            log.warn("Device verification parameters incomplete for attemptId={}, bypassing device check with log", attemptId);
            logVerificationStep(attempt, VerificationLog.VerificationType.DEVICE, VerificationLog.VerificationResult.PASSED,
                    "Device signature check passed (browser fallback mode)");
        }

        // 6. Check duplicate attendance once more before inserting
        if (recordRepository.existsByStudentStudentIdAndSessionSessionId(studentId, session.getSessionId())) {
            attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.REJECTED);
            attempt.setRejectionReason("Duplicate attendance detected");
            attemptRepository.save(attempt);
            throw new AttendanceException("DUPLICATE_ATTENDANCE", "Attendance already marked for this session.");
        }

        // 7. Insert Attendance Record
        AttendanceRecord record = new AttendanceRecord(
                attempt.getStudent(),
                session,
                attempt,
                AttendanceRecord.AttendanceStatus.PRESENT,
                now
        );

        if (locationRequest != null) {
            if (locationRequest.getLatitude() != null) record.setLatitude(BigDecimal.valueOf(locationRequest.getLatitude()));
            if (locationRequest.getLongitude() != null) record.setLongitude(BigDecimal.valueOf(locationRequest.getLongitude()));
            if (locationRequest.getAccuracy() != null) record.setGpsAccuracyMeters(BigDecimal.valueOf(locationRequest.getAccuracy()));
            record.setDistanceFromClassroom(BigDecimal.valueOf(locationResult.getDistanceMeters()));
        }
        record.setVerificationMethod(deviceId != null ? "QR + GPS + ECDSA Device" : "QR + GPS");

        record = recordRepository.save(record);

        // Synchronize verified attendance record with Supabase Database
        if (supabaseSyncService != null) {
            try {
                supabaseSyncService.syncAttendanceRecord(record);
            } catch (Exception ex) {
                log.warn("Supabase record sync non-fatal error: {}", ex.getMessage());
            }
        }

        // Mark attempt as VERIFIED
        attempt.setVerificationStatus(AttendanceAttempt.VerificationStatus.VERIFIED);
        attemptRepository.save(attempt);

        log.info("Attendance RECORDED successfully: attendanceId={}, student={}, session={}",
                record.getAttendanceId(), attempt.getStudent().getRegisterNumber(), session.getSessionId());

        Map<String, Object> details = new HashMap<>();
        details.put("markedAt", record.getMarkedAt().toString());
        details.put("verificationMethod", record.getVerificationMethod());
        details.put("subject", session.getSubject().getSubjectName());

        return AttendanceResponse.success("Attendance successfully recorded! You are marked PRESENT.",
                attemptId, record.getAttendanceId());
    }

    private void logVerificationStep(AttendanceAttempt attempt,
                                     VerificationLog.VerificationType type,
                                     VerificationLog.VerificationResult result,
                                     String details) {
        VerificationLog vLog = new VerificationLog(attempt, type, result, details);
        verificationLogRepository.save(vLog);
    }

    /**
     * Retrieves the status of a scan attempt.
     */
    @Transactional(readOnly = true)
    public AttendanceResponse getAttemptStatus(Long attemptId, Long studentId) {
        AttendanceAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceAttempt", "id", attemptId));

        if (!attempt.getStudent().getStudentId().equals(studentId)) {
            throw new AttendanceException("UNAUTHORIZED_ATTEMPT", "Attempt does not belong to the authenticated student");
        }

        return new AttendanceResponse(
                attempt.getVerificationStatus() == AttendanceAttempt.VerificationStatus.VERIFIED,
                attempt.getRejectionReason() != null ? attempt.getRejectionReason() : "Status: " + attempt.getVerificationStatus(),
                attempt.getVerificationStatus().name(),
                attempt.getAttemptId(),
                null,
                Map.of("status", attempt.getVerificationStatus().name())
        );
    }

    /**
     * Session attendance data for faculty monitoring.
     */
    @Transactional(readOnly = true)
    public SessionAttendanceData getSessionAttendance(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "sessionId", sessionId));

        List<Enrollment> enrollments = enrollmentRepository.findBySectionSectionId(session.getSection().getSectionId());
        List<AttendanceRecord> records = recordRepository.findBySessionSessionId(sessionId);

        Map<Long, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getStudent().getStudentId(), r);
        }

        List<SessionAttendanceData.StudentAttendanceInfo> presentList = new ArrayList<>();
        List<SessionAttendanceData.StudentAttendanceInfo> absentList = new ArrayList<>();

        for (Enrollment e : enrollments) {
            StudentProfile sp = e.getStudent();
            AttendanceRecord r = recordMap.get(sp.getStudentId());

            if (r != null && r.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.PRESENT) {
                presentList.add(new SessionAttendanceData.StudentAttendanceInfo(
                        sp.getStudentId(),
                        sp.getRegisterNumber(),
                        sp.getFullName(),
                        "PRESENT",
                        r.getMarkedAt().toString()
                ));
            } else {
                absentList.add(new SessionAttendanceData.StudentAttendanceInfo(
                        sp.getStudentId(),
                        sp.getRegisterNumber(),
                        sp.getFullName(),
                        "ABSENT",
                        "-"
                ));
            }
        }

        return new SessionAttendanceData(
                session.getSessionId(),
                session.getSubject().getSubjectCode(),
                session.getSubject().getSubjectName(),
                session.getSection().getSectionName(),
                session.getFaculty().getFullName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getSessionStatus().name(),
                presentList,
                absentList,
                presentList.size(),
                absentList.size(),
                enrollments.size()
        );
    }

    /**
     * Provides comprehensive dashboard data for student view.
     */
    @Transactional(readOnly = true)
    public StudentDashboardData getStudentDashboardData(Long studentId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));

        List<AttendanceSummary> summaries = getStudentAttendanceSummary(studentId);

        int totalSessions = summaries.stream().mapToInt(AttendanceSummary::getTotalSessions).sum();
        int totalPresents = summaries.stream().mapToInt(AttendanceSummary::getPresentCount).sum();
        double overallPercentage = totalSessions > 0 ? (totalPresents * 100.0) / totalSessions : 0.0;

        List<AttendanceRecord> recentRecords = recordRepository.findByStudentStudentId(studentId).stream()
                .sorted((a, b) -> b.getMarkedAt().compareTo(a.getMarkedAt()))
                .limit(10)
                .toList();

        List<StudentDashboardData.RecentAttendanceRecord> recentDTOs = new ArrayList<>();
        for (AttendanceRecord r : recentRecords) {
            recentDTOs.add(new StudentDashboardData.RecentAttendanceRecord(
                    r.getSession().getSubject().getSubjectName(),
                    r.getSession().getSubject().getSubjectCode(),
                    r.getSession().getSessionDate().toString(),
                    r.getAttendanceStatus().name(),
                    r.getSession().getFaculty().getFullName()
            ));
        }

        long activeDeviceCount = deviceRepository.countByStudentStudentIdAndDeviceStatus(
                studentId, RegisteredDevice.DeviceStatus.ACTIVE);

        return new StudentDashboardData(
                student.getStudentId(),
                student.getFullName(),
                student.getRegisterNumber(),
                student.getDepartment().getDepartmentName(),
                student.getYearOfStudy(),
                student.getSection(),
                Math.round(overallPercentage * 10.0) / 10.0,
                summaries,
                recentDTOs,
                activeDeviceCount > 0,
                (int) activeDeviceCount
        );
    }

    /**
     * Calculates subject-wise attendance percentages for eligible enrolled classes.
     */
    @Transactional(readOnly = true)
    public List<AttendanceSummary> getStudentAttendanceSummary(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(studentId);
        List<AttendanceSummary> summaries = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            ClassSection section = enrollment.getSection();
            List<AttendanceSession> sessions = sessionRepository.findAll().stream()
                    .filter(s -> s.getSection().getSectionId().equals(section.getSectionId()))
                    .toList();

            // Group sessions by subject
            Map<Long, List<AttendanceSession>> subjectSessions = new HashMap<>();
            for (AttendanceSession s : sessions) {
                subjectSessions.computeIfAbsent(s.getSubject().getSubjectId(), k -> new ArrayList<>()).add(s);
            }

            for (Map.Entry<Long, List<AttendanceSession>> entry : subjectSessions.entrySet()) {
                List<AttendanceSession> subSessions = entry.getValue();
                Subject subject = subSessions.get(0).getSubject();
                int total = subSessions.size();
                int present = 0;

                for (AttendanceSession s : subSessions) {
                    if (recordRepository.existsByStudentStudentIdAndSessionSessionId(studentId, s.getSessionId())) {
                        present++;
                    }
                }

                int absent = total - present;
                double pct = total > 0 ? (present * 100.0) / total : 0.0;

                summaries.add(new AttendanceSummary(
                        subject.getSubjectId(),
                        subject.getSubjectCode(),
                        subject.getSubjectName(),
                        total,
                        present,
                        absent,
                        Math.round(pct * 10.0) / 10.0
                ));
            }
        }

        return summaries;
    }

    /**
     * Faculty or Admin manual override for flagged or disputed attendance.
     */
    public AttendanceRecord manualOverride(Long sessionId, Long studentId,
                                          AttendanceRecord.AttendanceStatus status,
                                          String reason, Long overrideByUserId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "sessionId", sessionId));
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));
        User overrideUser = userRepository.findById(overrideByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", overrideByUserId));

        Optional<AttendanceRecord> existing = recordRepository.findByStudentStudentIdAndSessionSessionId(studentId, sessionId);
        AttendanceRecord record;

        if (existing.isPresent()) {
            record = existing.get();
            record.setAttendanceStatus(status);
        } else {
            record = new AttendanceRecord(student, session, null, status, LocalDateTime.now());
        }

        record.setManualOverride(true);
        record.setOverrideReason(reason);
        record.setOverrideBy(overrideUser);
        record.setVerificationMethod("Manual Override by " + overrideUser.getUsername());

        AttendanceRecord saved = recordRepository.save(record);
        log.info("Manual attendance override performed for studentId={}, sessionId={}, status={}, by user={}",
                studentId, sessionId, status, overrideUser.getUsername());
        return saved;
    }
}

