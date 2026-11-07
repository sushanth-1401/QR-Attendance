package com.attendance.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.attendance.dto.DashboardStats;
import com.attendance.entity.AuditLog;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Classroom;
import com.attendance.entity.Department;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.FacultySubjectAssignment;
import com.attendance.entity.RegisteredDevice;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.repository.AuditLogRepository;
import com.attendance.repository.DeviceRepository;
import com.attendance.service.DeviceService;
import com.attendance.service.ReportService;
import com.attendance.service.UserService;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ReportService reportService;
    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.attendance.service.SupabaseSyncService supabaseSyncService;

    public AdminController(UserService userService,
                           ReportService reportService,
                           DeviceService deviceService,
                           DeviceRepository deviceRepository,
                           AuditLogRepository auditLogRepository,
                           com.attendance.service.SupabaseSyncService supabaseSyncService) {
        this.userService = userService;
        this.reportService = reportService;
        this.deviceService = deviceService;
        this.deviceRepository = deviceRepository;
        this.auditLogRepository = auditLogRepository;
        this.supabaseSyncService = supabaseSyncService;
    }

    // -----------------------------------------------------------------------------------
    // VIEW CONTROLLER
    // -----------------------------------------------------------------------------------

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        DashboardStats stats = userService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("departments", userService.getAllDepartments());
        model.addAttribute("classrooms", userService.getAllClassrooms());
        model.addAttribute("subjects", userService.getAllSubjects());
        model.addAttribute("sections", userService.getAllClassSections());
        model.addAttribute("faculties", userService.getAllFaculty());
        model.addAttribute("students", userService.getAllStudents());
        return "admin/dashboard";
    }

    // -----------------------------------------------------------------------------------
    // REST API ENDPOINTS
    // -----------------------------------------------------------------------------------

    @GetMapping("/api/admin/dashboard")
    @ResponseBody
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(userService.getDashboardStats());
    }

    // Student Management
    @GetMapping("/api/admin/students")
    @ResponseBody
    public ResponseEntity<List<StudentProfile>> getAllStudents() {
        return ResponseEntity.ok(userService.getAllStudents());
    }

    @PostMapping("/api/admin/students")
    @ResponseBody
    public ResponseEntity<StudentProfile> createStudent(@RequestBody Map<String, Object> req) {
        if (req.get("departmentId") == null || req.get("departmentId").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required. Please select an academic department.");
        }
        StudentProfile profile = userService.createStudent(
                (String) req.get("username"),
                (String) req.get("email"),
                (String) req.get("password"),
                (String) req.get("registerNumber"),
                (String) req.get("fullName"),
                Long.valueOf(req.get("departmentId").toString()),
                Integer.valueOf(req.get("yearOfStudy").toString()),
                (String) req.get("section")
        );
        try {
            supabaseSyncService.syncStudentProfile(profile);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/api/admin/students/{id}")
    @ResponseBody
    public ResponseEntity<StudentProfile> getStudentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getStudentById(id));
    }

    @PutMapping("/api/admin/students/{id}")
    @ResponseBody
    public ResponseEntity<StudentProfile> updateStudent(@PathVariable("id") Long id,
                                                        @RequestBody Map<String, Object> req) {
        StudentProfile updated = userService.updateStudent(
                id,
                (String) req.get("fullName"),
                req.get("departmentId") != null ? Long.valueOf(req.get("departmentId").toString()) : null,
                req.get("yearOfStudy") != null ? Integer.valueOf(req.get("yearOfStudy").toString()) : null,
                (String) req.get("section")
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/api/admin/students/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStudentStatus(@PathVariable("id") Long id,
                                                                   @RequestBody Map<String, String> req) {
        StudentProfile student = userService.getStudentById(id);
        User.AccountStatus status = User.AccountStatus.valueOf(req.get("status"));
        userService.updateUserStatus(student.getUser().getUserId(), status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Student status updated to " + status));
    }

    // Faculty Management
    @GetMapping("/api/admin/faculty")
    @ResponseBody
    public ResponseEntity<List<FacultyProfile>> getAllFaculty() {
        return ResponseEntity.ok(userService.getAllFaculty());
    }

    @PostMapping("/api/admin/faculty")
    @ResponseBody
    public ResponseEntity<FacultyProfile> createFaculty(@RequestBody Map<String, Object> req) {
        if (req.get("departmentId") == null || req.get("departmentId").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required. Please select an academic department.");
        }
        Long deptId = Long.valueOf(req.get("departmentId").toString());
        FacultyProfile profile = userService.createFaculty(
                (String) req.get("username"),
                (String) req.get("email"),
                (String) req.get("password"),
                (String) req.get("employeeId"),
                (String) req.get("fullName"),
                deptId
        );

        // First sync faculty profile to Supabase so foreign keys in dependent tables (like assignments) exist
        try {
            supabaseSyncService.syncFacultyProfile(profile);
        } catch (Exception ignored) {}

        // Assign faculty to the selected undergraduate curriculum course / subject
        if (req.get("subjectId") != null && !req.get("subjectId").toString().trim().isEmpty()) {
            try {
                Long subjectId = Long.valueOf(req.get("subjectId").toString());
                List<ClassSection> sections = userService.getAllClassSections();
                ClassSection targetSection = sections.stream()
                        .filter(s -> s.getDepartment() != null && s.getDepartment().getDepartmentId().equals(deptId))
                        .findFirst()
                        .orElse(sections.isEmpty() ? null : sections.get(0));

                if (targetSection != null) {
                    FacultySubjectAssignment assignment = userService.assignFacultyToSubject(
                            profile.getFacultyId(), subjectId, targetSection.getSectionId(), "2026-2027");
                    supabaseSyncService.syncFacultySubjectAssignment(assignment);
                }
            } catch (Exception ex) {
                // Non-fatal if course assignment failed
            }
        }

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/api/admin/faculty/{id}")
    @ResponseBody
    public ResponseEntity<FacultyProfile> updateFaculty(@PathVariable("id") Long id,
                                                        @RequestBody Map<String, Object> req) {
        FacultyProfile updated = userService.updateFaculty(
                id,
                (String) req.get("fullName"),
                req.get("departmentId") != null ? Long.valueOf(req.get("departmentId").toString()) : null
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/api/admin/faculty/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateFacultyStatus(@PathVariable("id") Long id,
                                                                   @RequestBody Map<String, String> req) {
        FacultyProfile faculty = userService.getFacultyById(id);
        User.AccountStatus status = User.AccountStatus.valueOf(req.get("status"));
        userService.updateUserStatus(faculty.getUser().getUserId(), status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Faculty status updated to " + status));
    }

    @DeleteMapping("/api/admin/faculty/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFaculty(@PathVariable("id") Long id) {
        Long userId = userService.deleteFaculty(id);
        supabaseSyncService.syncDeleteFaculty(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Faculty member deleted successfully"));
    }

    @DeleteMapping("/api/admin/students/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable("id") Long id) {
        Long userId = userService.deleteStudent(id);
        supabaseSyncService.syncDeleteStudent(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Student profile deleted successfully"));
    }

    @DeleteMapping("/api/admin/users/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        supabaseSyncService.syncDeleteUser(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "User deleted successfully"));
    }

    // Departments
    @GetMapping("/api/admin/departments")
    @ResponseBody
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(userService.getAllDepartments());
    }

    @PostMapping("/api/admin/departments")
    @ResponseBody
    public ResponseEntity<Department> createDepartment(@RequestBody Map<String, String> req) {
        Department dept = userService.createDepartment(req.get("departmentCode"), req.get("departmentName"));
        try {
            supabaseSyncService.syncDepartment(dept);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(dept);
    }

    // Subjects
    @GetMapping("/api/admin/subjects")
    @ResponseBody
    public ResponseEntity<List<Subject>> getAllSubjects() {
        return ResponseEntity.ok(userService.getAllSubjects());
    }

    @PostMapping("/api/admin/subjects")
    @ResponseBody
    public ResponseEntity<Subject> createSubject(@RequestBody Map<String, Object> req) {
        Subject s = userService.createSubject(
                (String) req.get("subjectCode"),
                (String) req.get("subjectName"),
                Long.valueOf(req.get("departmentId").toString()),
                req.get("credits") != null ? Integer.valueOf(req.get("credits").toString()) : 3
        );
        try {
            supabaseSyncService.syncSubject(s);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(s);
    }

    // Classrooms
    @GetMapping("/api/admin/classrooms")
    @ResponseBody
    public ResponseEntity<List<Classroom>> getAllClassrooms() {
        return ResponseEntity.ok(userService.getAllClassrooms());
    }

    @PostMapping("/api/admin/classrooms")
    @ResponseBody
    public ResponseEntity<Classroom> createClassroom(@RequestBody Map<String, Object> req) {
        Classroom room = userService.createClassroom(
                (String) req.get("roomName"),
                (String) req.get("buildingName"),
                req.get("floorNumber") != null ? Integer.valueOf(req.get("floorNumber").toString()) : null,
                new BigDecimal(req.get("latitude").toString()),
                new BigDecimal(req.get("longitude").toString()),
                req.get("allowedRadiusMeters") != null ? Integer.valueOf(req.get("allowedRadiusMeters").toString()) : 50
        );
        try {
            supabaseSyncService.syncClassroom(room);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(room);
    }

    @PutMapping("/api/admin/classrooms/{id}")
    @ResponseBody
    public ResponseEntity<Classroom> updateClassroom(@PathVariable("id") Long id,
                                                     @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(userService.updateClassroom(
                id,
                (String) req.get("roomName"),
                (String) req.get("buildingName"),
                req.get("floorNumber") != null ? Integer.valueOf(req.get("floorNumber").toString()) : null,
                req.get("latitude") != null ? new BigDecimal(req.get("latitude").toString()) : null,
                req.get("longitude") != null ? new BigDecimal(req.get("longitude").toString()) : null,
                req.get("allowedRadiusMeters") != null ? Integer.valueOf(req.get("allowedRadiusMeters").toString()) : null
        ));
    }

    // Class Sections
    @GetMapping("/api/admin/sections")
    @ResponseBody
    public ResponseEntity<List<ClassSection>> getAllSections() {
        return ResponseEntity.ok(userService.getAllClassSections());
    }

    @PostMapping("/api/admin/sections")
    @ResponseBody
    public ResponseEntity<ClassSection> createSection(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(userService.createClassSection(
                Long.valueOf(req.get("departmentId").toString()),
                (String) req.get("academicYear"),
                Integer.valueOf(req.get("yearOfStudy").toString()),
                (String) req.get("sectionName")
        ));
    }

    // Faculty Assignments
    @GetMapping("/api/admin/assignments")
    @ResponseBody
    public ResponseEntity<List<FacultySubjectAssignment>> getAllAssignments() {
        return ResponseEntity.ok(userService.getAllAssignments());
    }

    @PostMapping("/api/admin/assignments")
    @ResponseBody
    public ResponseEntity<FacultySubjectAssignment> createAssignment(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(userService.assignFacultyToSubject(
                Long.valueOf(req.get("facultyId").toString()),
                Long.valueOf(req.get("subjectId").toString()),
                Long.valueOf(req.get("sectionId").toString()),
                (String) req.get("academicYear")
        ));
    }

    // Student Enrollment
    @PostMapping("/api/admin/enrollments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enrollStudent(@RequestBody Map<String, Object> req) {
        userService.enrollStudent(
                Long.valueOf(req.get("studentId").toString()),
                Long.valueOf(req.get("sectionId").toString())
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "Student successfully enrolled"));
    }

    // Devices & Audit Logs
    @GetMapping("/api/admin/devices")
    @ResponseBody
    public ResponseEntity<List<RegisteredDevice>> getAllDevices() {
        return ResponseEntity.ok(deviceRepository.findAll());
    }

    @DeleteMapping("/api/admin/devices/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeDevice(@PathVariable("id") Long id,
                                                            @RequestParam(value = "reason", required = false) String reason) {
        deviceService.revokeDevice(id, null, reason != null ? reason : "Revoked by Administrator");
        return ResponseEntity.ok(Map.of("success", true, "message", "Device revoked successfully"));
    }

    @GetMapping("/api/admin/audit-logs")
    @ResponseBody
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/api/admin/suspicious-attempts")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSuspiciousAttempts() {
        return ResponseEntity.ok(reportService.getSuspiciousAttempts());
    }

    @PostMapping("/api/admin/supabase/sync")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerSupabaseSync() {
        supabaseSyncService.syncAll();
        return ResponseEntity.ok(Map.of("success", true, "message", "Full Supabase database synchronization triggered"));
    }
}

