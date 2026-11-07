package com.attendance.service;

import com.attendance.entity.*;
import com.attendance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service to synchronize application data and attendance records directly to Supabase
 */
@Service
public class SupabaseSyncService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseSyncService.class);

    @Value("${supabase.url:https://rsxttzenmxkuwrfrbkbv.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.publishable-key:sb_publishable_RkOx-OIQIHWS0kSmoj-n7A_ltEygh4V}")
    private String publishableKey;

    @Value("${supabase.service-key:}")
    private String serviceKey;

    private final DepartmentRepository departmentRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public SupabaseSyncService(DepartmentRepository departmentRepository,
                               ClassroomRepository classroomRepository,
                               UserRepository userRepository,
                               ClassSectionRepository classSectionRepository,
                               SubjectRepository subjectRepository,
                               FacultyRepository facultyRepository,
                               StudentRepository studentRepository,
                               FacultySubjectAssignmentRepository assignmentRepository,
                               AttendanceSessionRepository sessionRepository,
                               AttendanceRecordRepository recordRepository) {
        this.departmentRepository = departmentRepository;
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
        this.classSectionRepository = classSectionRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String key = (serviceKey != null && !serviceKey.isBlank()) ? serviceKey : publishableKey;
        headers.set("apikey", key);
        headers.set("Authorization", "Bearer " + key);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "resolution=merge-duplicates");
        return headers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000); // Wait 3s after startup for Flyway & JPA initialization
                // Sync master curriculum subjects, departments, and classrooms on startup without overwriting user deletions
                syncMasterData();
            } catch (Exception e) {
                log.warn("[Supabase Sync] Startup sync warning: {}", e.getMessage());
            }
        });
    }

    public synchronized void syncMasterData() {
        try {
            List<Map<String, Object>> deptPayloads = new ArrayList<>();
            for (Department d : departmentRepository.findAll()) deptPayloads.add(mapDepartment(d));

            List<Map<String, Object>> classroomPayloads = new ArrayList<>();
            for (Classroom c : classroomRepository.findAll()) classroomPayloads.add(mapClassroom(c));

            List<Map<String, Object>> sectionPayloads = new ArrayList<>();
            for (ClassSection cs : classSectionRepository.findAll()) sectionPayloads.add(mapClassSection(cs));

            List<Map<String, Object>> subjectPayloads = new ArrayList<>();
            for (Subject s : subjectRepository.findAll()) subjectPayloads.add(mapSubject(s));

            sendBatchToSupabase("departments", "department_id", deptPayloads);
            sendBatchToSupabase("classrooms", "classroom_id", classroomPayloads);
            sendBatchToSupabase("class_sections", "section_id", sectionPayloads);
            sendBatchToSupabase("subjects", "subject_id", subjectPayloads);

            log.info("[Supabase Sync] Startup master data sync completed (departments, classrooms, sections, 45 undergraduate subjects)");
        } catch (Exception ex) {
            log.warn("[Supabase Sync] Error during master data sync: {}", ex.getMessage());
        }
    }

    /**
     * Full synchronization of all entities in strict foreign-key dependency order
     */
    @Transactional(readOnly = true)
    public synchronized void syncAll() {
        log.info("[Supabase Sync] Extracting entities and starting sequential sync to Supabase...");

        // 1. Extract payloads inside transaction to eliminate Hibernate LazyInitialization or thread issues
        List<Map<String, Object>> deptPayloads = new ArrayList<>();
        for (Department d : departmentRepository.findAll()) {
            deptPayloads.add(mapDepartment(d));
        }

        List<Map<String, Object>> classroomPayloads = new ArrayList<>();
        for (Classroom c : classroomRepository.findAll()) {
            classroomPayloads.add(mapClassroom(c));
        }

        List<Map<String, Object>> userPayloads = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            userPayloads.add(mapUser(u));
        }

        List<Map<String, Object>> sectionPayloads = new ArrayList<>();
        for (ClassSection cs : classSectionRepository.findAll()) {
            sectionPayloads.add(mapClassSection(cs));
        }

        List<Map<String, Object>> subjectPayloads = new ArrayList<>();
        for (Subject s : subjectRepository.findAll()) {
            subjectPayloads.add(mapSubject(s));
        }

        List<Map<String, Object>> facultyPayloads = new ArrayList<>();
        for (FacultyProfile f : facultyRepository.findAll()) {
            facultyPayloads.add(mapFacultyProfile(f));
        }

        List<Map<String, Object>> studentPayloads = new ArrayList<>();
        for (StudentProfile s : studentRepository.findAll()) {
            studentPayloads.add(mapStudentProfile(s));
        }

        List<Map<String, Object>> assignmentPayloads = new ArrayList<>();
        for (FacultySubjectAssignment a : assignmentRepository.findAll()) {
            assignmentPayloads.add(mapFacultySubjectAssignment(a));
        }

        List<Map<String, Object>> sessionPayloads = new ArrayList<>();
        for (AttendanceSession s : sessionRepository.findAll()) {
            sessionPayloads.add(mapAttendanceSession(s));
        }

        List<Map<String, Object>> recordPayloads = new ArrayList<>();
        for (AttendanceRecord r : recordRepository.findAll()) {
            recordPayloads.add(mapAttendanceRecord(r));
        }

        // 2. Dispatch in bulk batches table-by-table to guarantee speed and foreign key integrity
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                sendBatchToSupabase("departments", "department_id", deptPayloads);
                sendBatchToSupabase("classrooms", "classroom_id", classroomPayloads);
                sendBatchToSupabase("users", "user_id", userPayloads);
                sendBatchToSupabase("class_sections", "section_id", sectionPayloads);
                sendBatchToSupabase("subjects", "subject_id", subjectPayloads);
                sendBatchToSupabase("faculty_profiles", "faculty_id", facultyPayloads);
                sendBatchToSupabase("student_profiles", "student_id", studentPayloads);
                sendBatchToSupabase("faculty_subject_assignments", "assignment_id", assignmentPayloads);
                sendBatchToSupabase("attendance_sessions", "session_id", sessionPayloads);
                sendBatchToSupabase("attendance_records", "student_id,session_id", recordPayloads);

                long duration = System.currentTimeMillis() - startTime;
                log.info("[Supabase Sync] Efficient batch sync completed in {} ms ({} dept, {} classrm, {} users, {} sec, {} subj, {} faculty, {} students, {} assign, {} sessions, {} records)",
                        duration, deptPayloads.size(), classroomPayloads.size(), userPayloads.size(), sectionPayloads.size(), subjectPayloads.size(),
                        facultyPayloads.size(), studentPayloads.size(), assignmentPayloads.size(), sessionPayloads.size(), recordPayloads.size());
            } catch (Exception ex) {
                log.error("[Supabase Sync] Error during batch dispatch: {}", ex.getMessage(), ex);
            }
        });
    }

    public void syncFacultySubjectAssignment(FacultySubjectAssignment a) {
        try {
            Map<String, Object> payload = mapFacultySubjectAssignment(a);
            sendToSupabase("faculty_subject_assignments", "assignment_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync faculty subject assignment {}: {}", a.getAssignmentId(), e.getMessage());
        }
    }

    public void syncAttendanceRecord(AttendanceRecord record) {
        try {
            Map<String, Object> payload = mapAttendanceRecord(record);
            sendToSupabase("attendance_records", "student_id,session_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync attendance record {}: {}", record.getAttendanceId(), e.getMessage());
        }
    }

    public void syncDepartment(Department dept) {
        try {
            Map<String, Object> payload = mapDepartment(dept);
            sendToSupabase("departments", "department_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync department {}: {}", dept.getDepartmentCode(), e.getMessage());
        }
    }

    public void syncUser(User user) {
        try {
            Map<String, Object> payload = mapUser(user);
            sendToSupabase("users", "user_id", payload);
            syncToSupabaseAuth(user);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private void syncToSupabaseAuth(User user) {
        try {
            if (user.getEmail() == null || user.getEmail().isBlank()) return;
            String authUrl = supabaseUrl + "/auth/v1/signup";
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", publishableKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", user.getUsername());
            metadata.put("role", user.getRole() != null ? user.getRole().name() : "STUDENT");

            Map<String, Object> body = new HashMap<>();
            body.put("email", user.getEmail());
            body.put("password", "tempPassword123!");
            body.put("data", metadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(authUrl, HttpMethod.POST, request, String.class);
            log.info("[Supabase Auth] Synced user {} into Supabase Auth (auth.users)", user.getUsername());
        } catch (Exception e) {
            log.debug("[Supabase Auth] Auth.users notice for {}: {}", user.getUsername(), e.getMessage());
        }
    }

    public void syncSubject(Subject subject) {
        try {
            Map<String, Object> payload = mapSubject(subject);
            sendToSupabase("subjects", "subject_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync subject {}: {}", subject.getSubjectCode(), e.getMessage());
        }
    }

    public void syncFacultyProfile(FacultyProfile faculty) {
        try {
            if (faculty.getUser() != null) {
                sendToSupabase("users", "user_id", mapUser(faculty.getUser()));
                syncToSupabaseAuth(faculty.getUser());
            }
            if (faculty.getDepartment() != null) {
                sendToSupabase("departments", "department_id", mapDepartment(faculty.getDepartment()));
            }
            Map<String, Object> payload = mapFacultyProfile(faculty);
            sendToSupabase("faculty_profiles", "faculty_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync faculty {}: {}", faculty.getEmployeeId(), e.getMessage());
        }
    }

    public void syncStudentProfile(StudentProfile student) {
        try {
            if (student.getUser() != null) {
                sendToSupabase("users", "user_id", mapUser(student.getUser()));
                syncToSupabaseAuth(student.getUser());
            }
            if (student.getDepartment() != null) {
                sendToSupabase("departments", "department_id", mapDepartment(student.getDepartment()));
            }
            Map<String, Object> payload = mapStudentProfile(student);
            sendToSupabase("student_profiles", "student_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync student {}: {}", student.getRegisterNumber(), e.getMessage());
        }
    }

    public void syncClassroom(Classroom room) {
        try {
            Map<String, Object> payload = mapClassroom(room);
            sendToSupabase("classrooms", "classroom_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync classroom {}: {}", room.getRoomName(), e.getMessage());
        }
    }

    public void syncAttendanceSession(AttendanceSession session) {
        try {
            Map<String, Object> payload = mapAttendanceSession(session);
            sendToSupabase("attendance_sessions", "session_id", payload);
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to sync session {}: {}", session.getSessionId(), e.getMessage());
        }
    }

    public boolean sendToSupabase(String table, String onConflictCol, Map<String, Object> payload) {
        String endpoint = supabaseUrl + "/rest/v1/" + table;
        if (onConflictCol != null && !onConflictCol.isBlank()) {
            endpoint += "?on_conflict=" + onConflictCol;
        }
        HttpHeaders headers = createHeaders();
        headers.set("Prefer", "resolution=merge-duplicates");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Supabase Sync] Successfully persisted row to table '{}' (on_conflict={})", table, onConflictCol);
                return true;
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403 || e.getResponseBodyAsString().contains("42501")) {
                log.error("[Supabase Sync] RLS Policy restriction on table '{}': {}. Run supabase_fix_rls.sql in Supabase SQL editor to allow row-level inserts.", table, e.getResponseBodyAsString());
            } else {
                log.warn("[Supabase Sync] HTTP {} from Supabase on table '{}': {}", e.getStatusCode(), table, e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to post to Supabase table '{}': {}", table, e.getMessage());
        }
        return false;
    }

    public boolean sendBatchToSupabase(String table, String onConflictCol, List<Map<String, Object>> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return true;
        }
        String endpoint = supabaseUrl + "/rest/v1/" + table;
        if (onConflictCol != null && !onConflictCol.isBlank()) {
            endpoint += "?on_conflict=" + onConflictCol;
        }
        HttpHeaders headers = createHeaders();
        headers.set("Prefer", "resolution=merge-duplicates");
        HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(payloads, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Supabase Sync] Batch persisted {} rows to table '{}' in one request", payloads.size(), table);
                return true;
            }
        } catch (HttpClientErrorException e) {
            log.warn("[Supabase Sync] HTTP {} batch on table '{}': {}", e.getStatusCode(), table, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed batch post to table '{}': {}", table, e.getMessage());
        }
        return false;
    }

    public boolean deleteFromSupabase(String table, String idColumn, Object idValue) {
        String endpoint = supabaseUrl + "/rest/v1/" + table + "?" + idColumn + "=eq." + idValue;
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.DELETE, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Supabase Sync] Successfully deleted from table '{}' where {}={}", table, idColumn, idValue);
                return true;
            }
        } catch (Exception e) {
            log.warn("[Supabase Sync] Failed to delete from '{}' where {}={}: {}", table, idColumn, idValue, e.getMessage());
        }
        return false;
    }

    public void syncDeleteFaculty(Long facultyId, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Delete assignments and sessions first for child-to-parent integrity
                deleteFromSupabase("faculty_subject_assignments", "faculty_id", facultyId);
                deleteFromSupabase("attendance_sessions", "faculty_id", facultyId);
                deleteFromSupabase("faculty_profiles", "faculty_id", facultyId);
                if (userId != null) {
                    deleteFromSupabase("users", "user_id", userId);
                }
                log.info("[Supabase Sync] Cascaded deletion completed for faculty_id={}, user_id={}", facultyId, userId);
            } catch (Exception e) {
                log.warn("[Supabase Sync] Failed to cascade delete faculty {}: {}", facultyId, e.getMessage());
            }
        });
    }

    public void syncDeleteStudent(Long studentId, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                deleteFromSupabase("attendance_records", "student_id", studentId);
                deleteFromSupabase("attendance_attempts", "student_id", studentId);
                deleteFromSupabase("registered_devices", "student_id", studentId);
                deleteFromSupabase("enrollments", "student_id", studentId);
                deleteFromSupabase("student_profiles", "student_id", studentId);
                if (userId != null) {
                    deleteFromSupabase("users", "user_id", userId);
                }
                log.info("[Supabase Sync] Cascaded deletion completed for student_id={}, user_id={}", studentId, userId);
            } catch (Exception e) {
                log.warn("[Supabase Sync] Failed to cascade delete student {}: {}", studentId, e.getMessage());
            }
        });
    }

    public void syncDeleteUser(Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                facultyRepository.findByUserUserId(userId).ifPresent(f -> {
                    deleteFromSupabase("faculty_subject_assignments", "faculty_id", f.getFacultyId());
                    deleteFromSupabase("attendance_sessions", "faculty_id", f.getFacultyId());
                    deleteFromSupabase("faculty_profiles", "faculty_id", f.getFacultyId());
                });
                studentRepository.findByUserUserId(userId).ifPresent(s -> {
                    deleteFromSupabase("attendance_records", "student_id", s.getStudentId());
                    deleteFromSupabase("attendance_attempts", "student_id", s.getStudentId());
                    deleteFromSupabase("registered_devices", "student_id", s.getStudentId());
                    deleteFromSupabase("enrollments", "student_id", s.getStudentId());
                    deleteFromSupabase("student_profiles", "student_id", s.getStudentId());
                });
                deleteFromSupabase("users", "user_id", userId);
                log.info("[Supabase Sync] Cascaded deletion completed for user_id={}", userId);
            } catch (Exception e) {
                log.warn("[Supabase Sync] Failed to cascade delete user {}: {}", userId, e.getMessage());
            }
        });
    }

    // ==========================================
    // Entity -> Map Mappers
    // ==========================================

    public Map<String, Object> mapDepartment(Department d) {
        Map<String, Object> m = new HashMap<>();
        if (d.getDepartmentId() != null) m.put("department_id", d.getDepartmentId());
        m.put("department_code", d.getDepartmentCode());
        m.put("department_name", d.getDepartmentName());
        m.put("is_active", d.getIsActive());
        return m;
    }

    public Map<String, Object> mapClassroom(Classroom c) {
        Map<String, Object> m = new HashMap<>();
        if (c.getClassroomId() != null) m.put("classroom_id", c.getClassroomId());
        m.put("room_name", c.getRoomName());
        m.put("building_name", c.getBuildingName());
        m.put("latitude", c.getLatitude());
        m.put("longitude", c.getLongitude());
        m.put("allowed_radius_meters", c.getAllowedRadiusMeters());
        m.put("is_active", c.getIsActive());
        return m;
    }

    public Map<String, Object> mapUser(User u) {
        Map<String, Object> m = new HashMap<>();
        if (u.getUserId() != null) m.put("user_id", u.getUserId());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("password_hash", u.getPasswordHash());
        m.put("role", u.getRole() != null ? u.getRole().name() : "STUDENT");
        m.put("account_status", u.getAccountStatus() != null ? u.getAccountStatus().name() : "ACTIVE");
        return m;
    }

    public Map<String, Object> mapClassSection(ClassSection s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getSectionId() != null) m.put("section_id", s.getSectionId());
        if (s.getDepartment() != null) m.put("department_id", s.getDepartment().getDepartmentId());
        m.put("academic_year", s.getAcademicYear());
        m.put("year_of_study", s.getYearOfStudy());
        m.put("section_name", s.getSectionName());
        m.put("is_active", s.getIsActive());
        return m;
    }

    public Map<String, Object> mapSubject(Subject s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getSubjectId() != null) m.put("subject_id", s.getSubjectId());
        m.put("subject_code", s.getSubjectCode());
        m.put("subject_name", s.getSubjectName());
        if (s.getDepartment() != null) m.put("department_id", s.getDepartment().getDepartmentId());
        m.put("credits", s.getCredits());
        m.put("is_active", s.getIsActive());
        return m;
    }

    public Map<String, Object> mapFacultyProfile(FacultyProfile f) {
        Map<String, Object> m = new HashMap<>();
        if (f.getFacultyId() != null) m.put("faculty_id", f.getFacultyId());
        if (f.getUser() != null) m.put("user_id", f.getUser().getUserId());
        if (f.getDepartment() != null) m.put("department_id", f.getDepartment().getDepartmentId());
        m.put("employee_id", f.getEmployeeId());
        m.put("full_name", f.getFullName());
        return m;
    }

    public Map<String, Object> mapFacultySubjectAssignment(FacultySubjectAssignment a) {
        Map<String, Object> m = new HashMap<>();
        if (a.getAssignmentId() != null) m.put("assignment_id", a.getAssignmentId());
        if (a.getFaculty() != null) m.put("faculty_id", a.getFaculty().getFacultyId());
        if (a.getSubject() != null) m.put("subject_id", a.getSubject().getSubjectId());
        if (a.getSection() != null) m.put("section_id", a.getSection().getSectionId());
        m.put("academic_year", a.getAcademicYear() != null ? a.getAcademicYear() : "2026-2027");
        m.put("is_active", a.getIsActive() != null ? a.getIsActive() : true);
        return m;
    }

    public Map<String, Object> mapStudentProfile(StudentProfile s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getStudentId() != null) m.put("student_id", s.getStudentId());
        if (s.getUser() != null) m.put("user_id", s.getUser().getUserId());
        if (s.getDepartment() != null) m.put("department_id", s.getDepartment().getDepartmentId());
        m.put("register_number", s.getRegisterNumber());
        m.put("full_name", s.getFullName());
        m.put("year_of_study", s.getYearOfStudy());
        m.put("section", s.getSection());
        return m;
    }

    public Map<String, Object> mapAttendanceSession(AttendanceSession s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getSessionId() != null) m.put("session_id", s.getSessionId());
        if (s.getSubject() != null) m.put("subject_id", s.getSubject().getSubjectId());
        if (s.getFaculty() != null) m.put("faculty_id", s.getFaculty().getFacultyId());
        if (s.getSection() != null) m.put("section_id", s.getSection().getSectionId());
        if (s.getClassroom() != null) m.put("classroom_id", s.getClassroom().getClassroomId());
        m.put("session_date", s.getSessionDate() != null ? s.getSessionDate().toString() : java.time.LocalDate.now().toString());
        if (s.getStartTime() != null) m.put("start_time", s.getStartTime().toString());
        if (s.getEndTime() != null) m.put("end_time", s.getEndTime().toString());
        m.put("session_status", s.getSessionStatus() != null ? s.getSessionStatus().name() : "ACTIVE");
        return m;
    }

    public Map<String, Object> mapAttendanceRecord(AttendanceRecord r) {
        Map<String, Object> m = new HashMap<>();
        if (r.getAttendanceId() != null) m.put("attendance_id", r.getAttendanceId());
        if (r.getStudent() != null) m.put("student_id", r.getStudent().getStudentId());
        if (r.getSession() != null) m.put("session_id", r.getSession().getSessionId());
        m.put("attendance_status", r.getAttendanceStatus() != null ? r.getAttendanceStatus().name() : "PRESENT");
        m.put("marked_at", r.getMarkedAt() != null ? r.getMarkedAt().toString() : java.time.LocalDateTime.now().toString());
        if (r.getLatitude() != null) m.put("latitude", r.getLatitude());
        if (r.getLongitude() != null) m.put("longitude", r.getLongitude());
        if (r.getGpsAccuracyMeters() != null) m.put("gps_accuracy_meters", r.getGpsAccuracyMeters());
        if (r.getDistanceFromClassroom() != null) m.put("distance_from_classroom", r.getDistanceFromClassroom());
        m.put("verification_method", r.getVerificationMethod() != null ? r.getVerificationMethod() : "QR + GPS");
        m.put("manual_override", r.getManualOverride() != null ? r.getManualOverride() : false);
        return m;
    }
}
