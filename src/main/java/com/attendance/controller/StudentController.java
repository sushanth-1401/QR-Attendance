package com.attendance.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.attendance.dto.AttendanceSummary;
import com.attendance.dto.StudentDashboardData;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.StudentProfile;
import com.attendance.exception.AttendanceException;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.service.AttendanceService;
import com.attendance.service.AuthService;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final AttendanceRecordRepository recordRepository;

    public StudentController(AttendanceService attendanceService,
                             AuthService authService,
                             AttendanceRecordRepository recordRepository) {
        this.attendanceService = attendanceService;
        this.authService = authService;
        this.recordRepository = recordRepository;
    }

    private StudentProfile getAuthenticatedStudent() {
        return authService.getCurrentStudentProfile()
                .orElseThrow(() -> new AttendanceException("STUDENT_PROFILE_MISSING", "Student profile not found for user"));
    }

    // -----------------------------------------------------------------------------------
    // VIEW CONTROLLER
    // -----------------------------------------------------------------------------------

    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model) {
        StudentProfile student = getAuthenticatedStudent();
        StudentDashboardData dashboardData = attendanceService.getStudentDashboardData(student.getStudentId());

        model.addAttribute("student", student);
        model.addAttribute("dashboard", dashboardData);
        return "student/dashboard";
    }

    @GetMapping("/student/scan-qr")
    public String scanQrPage(Model model) {
        StudentProfile student = getAuthenticatedStudent();
        model.addAttribute("student", student);
        return "student/scan-qr";
    }

    @GetMapping("/student/attendance-history")
    public String attendanceHistoryPage(Model model) {
        StudentProfile student = getAuthenticatedStudent();
        List<AttendanceRecord> records = recordRepository.findByStudentStudentId(student.getStudentId());
        List<AttendanceSummary> summaries = attendanceService.getStudentAttendanceSummary(student.getStudentId());

        model.addAttribute("student", student);
        model.addAttribute("records", records);
        model.addAttribute("summaries", summaries);
        return "student/attendance-history";
    }

    // -----------------------------------------------------------------------------------
    // REST API ENDPOINTS
    // -----------------------------------------------------------------------------------

    @GetMapping("/api/student/dashboard")
    @ResponseBody
    public ResponseEntity<StudentDashboardData> getDashboardData() {
        StudentProfile student = getAuthenticatedStudent();
        return ResponseEntity.ok(attendanceService.getStudentDashboardData(student.getStudentId()));
    }

    @GetMapping("/api/student/profile")
    @ResponseBody
    public ResponseEntity<StudentProfile> getProfile() {
        return ResponseEntity.ok(getAuthenticatedStudent());
    }

    @GetMapping("/api/student/attendance")
    @ResponseBody
    public ResponseEntity<List<AttendanceRecord>> getMyAttendanceRecords() {
        StudentProfile student = getAuthenticatedStudent();
        return ResponseEntity.ok(recordRepository.findByStudentStudentId(student.getStudentId()));
    }

    @GetMapping("/api/student/attendance/summary")
    @ResponseBody
    public ResponseEntity<List<AttendanceSummary>> getMyAttendanceSummary() {
        StudentProfile student = getAuthenticatedStudent();
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(student.getStudentId()));
    }
}

