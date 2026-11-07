package com.attendance.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.attendance.dto.CreateSessionRequest;
import com.attendance.dto.SessionAttendanceData;
import com.attendance.entity.AttendanceAttempt;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Classroom;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.FacultySubjectAssignment;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.AttendanceAttemptRepository;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.ClassSectionRepository;
import com.attendance.repository.ClassroomRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.FacultySubjectAssignmentRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.security.CurrentUser;
import com.attendance.service.AttendanceService;
import com.attendance.service.AuthService;
import com.attendance.service.QRCodeService;
import com.attendance.service.UserService;

@Controller
@PreAuthorize("hasRole('FACULTY')")
public class FacultyController {

    private final AttendanceSessionRepository sessionRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final FacultyRepository facultyRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSectionRepository sectionRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceAttemptRepository attemptRepository;
    private final QRCodeService qrCodeService;
    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final UserService userService;
    private final com.attendance.service.SupabaseSyncService supabaseSyncService;

    public FacultyController(AttendanceSessionRepository sessionRepository,
                             FacultySubjectAssignmentRepository assignmentRepository,
                             FacultyRepository facultyRepository,
                             SubjectRepository subjectRepository,
                             ClassSectionRepository sectionRepository,
                             ClassroomRepository classroomRepository,
                             AttendanceAttemptRepository attemptRepository,
                             QRCodeService qrCodeService,
                             AttendanceService attendanceService,
                             AuthService authService,
                             UserService userService,
                             com.attendance.service.SupabaseSyncService supabaseSyncService) {
        this.sessionRepository = sessionRepository;
        this.assignmentRepository = assignmentRepository;
        this.facultyRepository = facultyRepository;
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
        this.classroomRepository = classroomRepository;
        this.attemptRepository = attemptRepository;
        this.qrCodeService = qrCodeService;
        this.attendanceService = attendanceService;
        this.authService = authService;
        this.userService = userService;
        this.supabaseSyncService = supabaseSyncService;
    }

    private FacultyProfile getAuthenticatedFaculty() {
        return authService.getCurrentFacultyProfile()
                .orElseThrow(() -> new AttendanceException("FACULTY_PROFILE_MISSING", "Faculty profile not found for user"));
    }

    // -----------------------------------------------------------------------------------
    // VIEW CONTROLLER
    // -----------------------------------------------------------------------------------

    @GetMapping("/faculty/dashboard")
    public String facultyDashboard(Model model) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        List<FacultySubjectAssignment> assignments = assignmentRepository.findByFacultyFacultyId(faculty.getFacultyId());
        List<AttendanceSession> sessions = sessionRepository.findByFacultyFacultyId(faculty.getFacultyId());
        List<Classroom> classrooms = classroomRepository.findByIsActive(true);

        model.addAttribute("faculty", faculty);
        model.addAttribute("assignments", assignments);
        model.addAttribute("sessions", sessions);
        model.addAttribute("classrooms", classrooms);
        return "faculty/dashboard";
    }

    @GetMapping("/faculty/qr-attendance/{sessionId}")
    public String qrAttendancePage(@PathVariable("sessionId") Long sessionId, Model model) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", sessionId));

        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "You do not have access to this session");
        }

        model.addAttribute("faculty", faculty);
        model.addAttribute("attendanceSession", session);
        model.addAttribute("currentSession", session);
        return "faculty/qr-attendance";
    }

    // -----------------------------------------------------------------------------------
    // REST API ENDPOINTS
    // -----------------------------------------------------------------------------------

    @GetMapping("/api/faculty/assignments")
    @ResponseBody
    public ResponseEntity<List<FacultySubjectAssignment>> getMyAssignments() {
        FacultyProfile faculty = getAuthenticatedFaculty();
        return ResponseEntity.ok(assignmentRepository.findByFacultyFacultyId(faculty.getFacultyId()));
    }

    @PostMapping("/api/faculty/sessions")
    @ResponseBody
    public ResponseEntity<AttendanceSession> createSession(@RequestBody CreateSessionRequest req) {
        FacultyProfile faculty = getAuthenticatedFaculty();

        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", req.getSubjectId()));
        ClassSection section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSection", "id", req.getSectionId()));
        Classroom classroom = req.getClassroomId() != null
                ? classroomRepository.findById(req.getClassroomId()).orElse(null)
                : null;

        LocalDate sessionDate = req.getSessionDate() != null ? LocalDate.parse(req.getSessionDate()) : LocalDate.now();
        LocalTime startTime = req.getStartTime() != null ? LocalTime.parse(req.getStartTime()) : LocalTime.now();
        LocalTime endTime = req.getEndTime() != null ? LocalTime.parse(req.getEndTime()) : startTime.plusHours(1);

        AttendanceSession session = new AttendanceSession(subject, faculty, section, classroom, sessionDate, startTime, endTime);
        AttendanceSession saved = sessionRepository.save(session);

        try {
            supabaseSyncService.syncAttendanceSession(saved);
        } catch (Exception ignored) {}

        userService.logAudit("CREATE_SESSION", "AttendanceSession", saved.getSessionId(),
                "Created session for " + subject.getSubjectName() + " (" + section.getSectionName() + ")");
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/faculty/sessions")
    @ResponseBody
    public ResponseEntity<List<AttendanceSession>> getMySessions() {
        FacultyProfile faculty = getAuthenticatedFaculty();
        return ResponseEntity.ok(sessionRepository.findByFacultyFacultyId(faculty.getFacultyId()));
    }

    @GetMapping("/api/faculty/sessions/{id}")
    @ResponseBody
    public ResponseEntity<AttendanceSession> getSessionById(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to view this session");
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/api/faculty/sessions/{id}/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startSession(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to start this session");
        }

        session.setSessionStatus(AttendanceSession.SessionStatus.ACTIVE);
        sessionRepository.save(session);

        userService.logAudit("START_SESSION", "AttendanceSession", id, "Started attendance session #" + id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session started successfully", "status", "ACTIVE"));
    }

    @PostMapping("/api/faculty/sessions/{id}/close")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> closeSession(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to close this session");
        }

        session.setSessionStatus(AttendanceSession.SessionStatus.CLOSED);
        sessionRepository.save(session);

        // Expire all active tokens
        qrCodeService.expireToken(id);

        userService.logAudit("CLOSE_SESSION", "AttendanceSession", id, "Closed attendance session #" + id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session closed successfully", "status", "CLOSED"));
    }

    @GetMapping("/api/faculty/sessions/{id}/qr")
    @ResponseBody
    public ResponseEntity<QRCodeService.GeneratedQRData> generateOrRefreshQR(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to generate QR for this session");
        }

        QRCodeService.GeneratedQRData qrData = qrCodeService.generateTokenForSession(id);
        return ResponseEntity.ok(qrData);
    }

    @GetMapping("/api/faculty/sessions/{id}/attendance")
    @ResponseBody
    public ResponseEntity<SessionAttendanceData> getSessionAttendance(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to view attendance for this session");
        }

        return ResponseEntity.ok(attendanceService.getSessionAttendance(id));
    }

    @GetMapping("/api/faculty/sessions/{id}/attempts")
    @ResponseBody
    public ResponseEntity<List<AttendanceAttempt>> getSessionAttempts(@PathVariable("id") Long id) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", id));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to view attempts for this session");
        }

        return ResponseEntity.ok(attemptRepository.findBySessionSessionIdAndVerificationStatus(
                id, AttendanceAttempt.VerificationStatus.REJECTED));
    }

    @PostMapping("/api/faculty/sessions/{id}/override")
    @ResponseBody
    public ResponseEntity<AttendanceRecord> manualOverride(@PathVariable("id") Long sessionId,
                                                           @RequestBody Map<String, Object> req) {
        FacultyProfile faculty = getAuthenticatedFaculty();
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", sessionId));
        if (!session.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new AttendanceException("UNAUTHORIZED_ACCESS", "Unauthorized to override attendance for this session");
        }

        Long studentId = Long.valueOf(req.get("studentId").toString());
        String statusStr = (String) req.get("status");
        String reason = (String) req.get("reason");
        AttendanceRecord.AttendanceStatus status = AttendanceRecord.AttendanceStatus.valueOf(statusStr);

        AttendanceRecord record = attendanceService.manualOverride(
                sessionId, studentId, status, reason, faculty.getUser().getUserId());

        userService.logAudit("MANUAL_ATTENDANCE_OVERRIDE", "AttendanceRecord", record.getAttendanceId(),
                "Faculty " + faculty.getFullName() + " marked studentId=" + studentId + " as " + status + ". Reason: " + reason);

        return ResponseEntity.ok(record);
    }
}

