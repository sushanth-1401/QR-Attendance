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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.dto.DashboardStats;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.AuditLog;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Classroom;
import com.attendance.entity.Department;
import com.attendance.entity.Enrollment;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.FacultySubjectAssignment;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.AuditLogRepository;
import com.attendance.repository.ClassSectionRepository;
import com.attendance.repository.ClassroomRepository;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.FacultySubjectAssignmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.UserRepository;
import com.attendance.security.CurrentUser;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassroomRepository classroomRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       FacultyRepository facultyRepository,
                       DepartmentRepository departmentRepository,
                       SubjectRepository subjectRepository,
                       ClassSectionRepository classSectionRepository,
                       ClassroomRepository classroomRepository,
                       FacultySubjectAssignmentRepository assignmentRepository,
                       EnrollmentRepository enrollmentRepository,
                       AttendanceSessionRepository sessionRepository,
                       AttendanceRecordRepository attendanceRecordRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.classSectionRepository = classSectionRepository;
        this.classroomRepository = classroomRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void logAudit(String action, String entityType, Long entityId, String description) {
        User currentUser = null;
        Long userId = CurrentUser.getCurrentUserId();
        if (userId != null) {
            currentUser = userRepository.findById(userId).orElse(null);
        }
        AuditLog audit = new AuditLog(currentUser, action, entityType, entityId, description, "127.0.0.1");
        auditLogRepository.save(audit);
    }

    // -----------------------------------------------------------------------------------
    // STUDENT MANAGEMENT
    // -----------------------------------------------------------------------------------

    public StudentProfile createStudent(String username, String email, String password,
                                       String registerNumber, String fullName, Long departmentId,
                                       Integer yearOfStudy, String section) {
        if (userRepository.existsByUsername(username)) {
            throw new AttendanceException("USERNAME_EXISTS", "Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new AttendanceException("EMAIL_EXISTS", "Email already exists: " + email);
        }
        if (studentRepository.findByRegisterNumber(registerNumber).isPresent()) {
            throw new AttendanceException("REGISTER_EXISTS", "Register number already exists: " + registerNumber);
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));

        User user = new User(username, email, passwordEncoder.encode(password), User.Role.STUDENT);
        userRepository.save(user);

        StudentProfile profile = new StudentProfile(user, registerNumber, fullName, department, yearOfStudy, section);
        StudentProfile saved = studentRepository.save(profile);

        logAudit("CREATE_STUDENT", "StudentProfile", saved.getStudentId(), "Created student " + fullName + " (" + registerNumber + ")");
        return saved;
    }

    public StudentProfile updateStudent(Long studentId, String fullName, Long departmentId,
                                       Integer yearOfStudy, String section) {
        StudentProfile profile = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));

        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
            profile.setDepartment(department);
        }

        if (fullName != null && !fullName.trim().isEmpty()) {
            profile.setFullName(fullName.trim());
        }
        if (yearOfStudy != null) {
            profile.setYearOfStudy(yearOfStudy);
        }
        if (section != null) {
            profile.setSection(section.trim());
        }

        StudentProfile saved = studentRepository.save(profile);
        logAudit("UPDATE_STUDENT", "StudentProfile", saved.getStudentId(), "Updated student " + saved.getFullName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StudentProfile> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public StudentProfile getStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));
    }

    // -----------------------------------------------------------------------------------
    // FACULTY MANAGEMENT
    // -----------------------------------------------------------------------------------

    public FacultyProfile createFaculty(String username, String email, String password,
                                       String employeeId, String fullName, Long departmentId) {
        if (userRepository.existsByUsername(username)) {
            throw new AttendanceException("USERNAME_EXISTS", "Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new AttendanceException("EMAIL_EXISTS", "Email already exists: " + email);
        }
        if (facultyRepository.findByEmployeeId(employeeId).isPresent()) {
            throw new AttendanceException("EMPLOYEE_ID_EXISTS", "Employee ID already exists: " + employeeId);
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));

        User user = new User(username, email, passwordEncoder.encode(password), User.Role.FACULTY);
        userRepository.save(user);

        FacultyProfile profile = new FacultyProfile(user, employeeId, fullName, department);
        FacultyProfile saved = facultyRepository.save(profile);

        logAudit("CREATE_FACULTY", "FacultyProfile", saved.getFacultyId(), "Created faculty " + fullName + " (" + employeeId + ")");
        return saved;
    }

    public FacultyProfile updateFaculty(Long facultyId, String fullName, Long departmentId) {
        FacultyProfile profile = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("FacultyProfile", "id", facultyId));

        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
            profile.setDepartment(department);
        }

        if (fullName != null && !fullName.trim().isEmpty()) {
            profile.setFullName(fullName.trim());
        }

        FacultyProfile saved = facultyRepository.save(profile);
        logAudit("UPDATE_FACULTY", "FacultyProfile", saved.getFacultyId(), "Updated faculty " + saved.getFullName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FacultyProfile> getAllFaculty() {
        return facultyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public FacultyProfile getFacultyById(Long facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("FacultyProfile", "id", facultyId));
    }

    @Transactional
    public Long deleteFaculty(Long facultyId) {
        FacultyProfile profile = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("FacultyProfile", "id", facultyId));
        Long userId = profile.getUser() != null ? profile.getUser().getUserId() : null;

        // Cascade delete assignments
        List<FacultySubjectAssignment> assignments = assignmentRepository.findByFacultyFacultyId(facultyId);
        assignmentRepository.deleteAll(assignments);

        facultyRepository.delete(profile);
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        logAudit("DELETE_FACULTY", "FacultyProfile", facultyId, "Deleted faculty profile " + profile.getFullName());
        return userId;
    }

    @Transactional
    public Long deleteStudent(Long studentId) {
        StudentProfile profile = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));
        Long userId = profile.getUser() != null ? profile.getUser().getUserId() : null;

        // Cascade delete enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(studentId);
        enrollmentRepository.deleteAll(enrollments);

        studentRepository.delete(profile);
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        logAudit("DELETE_STUDENT", "StudentProfile", studentId, "Deleted student profile " + profile.getFullName());
        return userId;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        facultyRepository.findByUserUserId(userId).ifPresent(f -> {
            List<FacultySubjectAssignment> assignments = assignmentRepository.findByFacultyFacultyId(f.getFacultyId());
            assignmentRepository.deleteAll(assignments);
            facultyRepository.delete(f);
        });

        studentRepository.findByUserUserId(userId).ifPresent(s -> {
            List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(s.getStudentId());
            enrollmentRepository.deleteAll(enrollments);
            studentRepository.delete(s);
        });

        userRepository.delete(user);
        logAudit("DELETE_USER", "User", userId, "Deleted user " + user.getUsername());
    }

    // -----------------------------------------------------------------------------------
    // USER STATUS
    // -----------------------------------------------------------------------------------

    public void updateUserStatus(Long userId, User.AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setAccountStatus(status);
        userRepository.save(user);
        logAudit("CHANGE_USER_STATUS", "User", userId, "Changed status of user " + user.getUsername() + " to " + status);
    }

    // -----------------------------------------------------------------------------------
    // DEPARTMENTS, SUBJECTS, SECTIONS, CLASSROOMS
    // -----------------------------------------------------------------------------------

    public Department createDepartment(String code, String name) {
        Department dept = new Department(code, name);
        Department saved = departmentRepository.save(dept);
        logAudit("CREATE_DEPARTMENT", "Department", saved.getDepartmentId(), "Created department " + code);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Subject createSubject(String code, String name, Long departmentId, Integer credits) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
        Subject subject = new Subject(code, name, dept, credits != null ? credits : 3);
        Subject saved = subjectRepository.save(subject);
        logAudit("CREATE_SUBJECT", "Subject", saved.getSubjectId(), "Created subject " + code);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public ClassSection createClassSection(Long departmentId, String academicYear, Integer yearOfStudy, String sectionName) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
        ClassSection section = new ClassSection(dept, academicYear, yearOfStudy, sectionName);
        ClassSection saved = classSectionRepository.save(section);
        logAudit("CREATE_SECTION", "ClassSection", saved.getSectionId(), "Created section " + sectionName + " (" + academicYear + ")");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClassSection> getAllClassSections() {
        return classSectionRepository.findAll();
    }

    public Classroom createClassroom(String roomName, String buildingName, Integer floorNumber,
                                     BigDecimal latitude, BigDecimal longitude, Integer allowedRadiusMeters) {
        Classroom room = new Classroom(roomName, buildingName, floorNumber, latitude, longitude,
                allowedRadiusMeters != null ? allowedRadiusMeters : 50);
        Classroom saved = classroomRepository.save(room);
        logAudit("CREATE_CLASSROOM", "Classroom", saved.getClassroomId(), "Created classroom " + roomName + " (" + buildingName + ")");
        return saved;
    }

    public Classroom updateClassroom(Long classroomId, String roomName, String buildingName, Integer floorNumber,
                                     BigDecimal latitude, BigDecimal longitude, Integer allowedRadiusMeters) {
        Classroom room = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", "id", classroomId));
        if (roomName != null) room.setRoomName(roomName);
        if (buildingName != null) room.setBuildingName(buildingName);
        if (floorNumber != null) room.setFloorNumber(floorNumber);
        if (latitude != null) room.setLatitude(latitude);
        if (longitude != null) room.setLongitude(longitude);
        if (allowedRadiusMeters != null) room.setAllowedRadiusMeters(allowedRadiusMeters);
        Classroom saved = classroomRepository.save(room);
        logAudit("UPDATE_CLASSROOM", "Classroom", saved.getClassroomId(), "Updated classroom " + saved.getRoomName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Classroom> getAllClassrooms() {
        return classroomRepository.findAll();
    }

    public FacultySubjectAssignment assignFacultyToSubject(Long facultyId, Long subjectId, Long sectionId, String academicYear) {
        FacultyProfile faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("FacultyProfile", "id", facultyId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        ClassSection section = classSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSection", "id", sectionId));

        FacultySubjectAssignment assignment = new FacultySubjectAssignment(faculty, subject, section, academicYear);
        FacultySubjectAssignment saved = assignmentRepository.save(assignment);
        logAudit("ASSIGN_FACULTY", "FacultySubjectAssignment", saved.getAssignmentId(),
                "Assigned " + faculty.getFullName() + " to " + subject.getSubjectName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FacultySubjectAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Enrollment enrollStudent(Long studentId, Long sectionId) {
        StudentProfile student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "id", studentId));
        ClassSection section = classSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSection", "id", sectionId));

        if (enrollmentRepository.existsByStudentStudentIdAndSectionSectionId(studentId, sectionId)) {
            throw new AttendanceException("ALREADY_ENROLLED", "Student is already enrolled in this section");
        }

        Enrollment enrollment = new Enrollment(student, section);
        Enrollment saved = enrollmentRepository.save(enrollment);
        logAudit("ENROLL_STUDENT", "Enrollment", saved.getEnrollmentId(),
                "Enrolled " + student.getFullName() + " in section " + section.getSectionName());
        return saved;
    }

    // -----------------------------------------------------------------------------------
    // ADMIN DASHBOARD STATS
    // -----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.countBySessionStatus(AttendanceSession.SessionStatus.ACTIVE);

        long presentCount = attendanceRecordRepository.count(); // total records
        long presentsOnly = attendanceRecordRepository.findAll().stream()
                .filter(ar -> ar.getAttendanceStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                .count();

        double overallPercentage = presentCount > 0 ? (double) (presentsOnly * 100.0) / presentCount : 0.0;

        // Recent activity from audit logs
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        List<AuditLog> recentLogs = auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .toList();

        for (AuditLog logItem : recentLogs) {
            Map<String, Object> map = new HashMap<>();
            map.put("action", logItem.getAction());
            map.put("description", logItem.getDescription());
            map.put("user", logItem.getUser() != null ? logItem.getUser().getUsername() : "System");
            map.put("time", logItem.getCreatedAt().toString());
            recentActivity.add(map);
        }

        // Department stats
        List<Map<String, Object>> deptStats = new ArrayList<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department d : departments) {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentCode", d.getDepartmentCode());
            map.put("departmentName", d.getDepartmentName());
            map.put("studentCount", studentRepository.countByDepartmentDepartmentId(d.getDepartmentId()));
            map.put("facultyCount", facultyRepository.countByDepartmentDepartmentId(d.getDepartmentId()));
            deptStats.add(map);
        }

        return new DashboardStats(totalStudents, totalFaculty, totalSubjects, totalSessions,
                activeSessions, Math.round(overallPercentage * 10.0) / 10.0, recentActivity, deptStats);
    }
}

