package com.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.attendance.config.AppConfig;
import com.attendance.dto.AttendanceResponse;
import com.attendance.entity.AttendanceAttempt;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Department;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.QRToken;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.exception.AttendanceException;
import com.attendance.repository.AttendanceAttemptRepository;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.DeviceRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;
import com.attendance.repository.VerificationLogRepository;

public class AttendanceServiceTest {

    private AttendanceService attendanceService;
    private AttendanceAttemptRepository attemptRepository;
    private AttendanceRecordRepository recordRepository;
    private AttendanceSessionRepository sessionRepository;
    private StudentRepository studentRepository;
    private EnrollmentRepository enrollmentRepository;
    private QRCodeService qrCodeService;
    private LocationService locationService;
    private DeviceService deviceService;
    private AppConfig appConfig;

    private StudentProfile student;
    private AttendanceSession session;
    private QRToken qrToken;

    @BeforeEach
    public void setUp() {
        attemptRepository = mock(AttendanceAttemptRepository.class);
        recordRepository = mock(AttendanceRecordRepository.class);
        sessionRepository = mock(AttendanceSessionRepository.class);
        studentRepository = mock(StudentRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        qrCodeService = mock(QRCodeService.class);
        locationService = mock(LocationService.class);
        deviceService = mock(DeviceService.class);
        appConfig = new AppConfig();
        appConfig.getQr().setReservationValiditySeconds(120);

        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        VerificationLogRepository verificationLogRepository = mock(VerificationLogRepository.class);

        attendanceService = new AttendanceService(
                attemptRepository, recordRepository, sessionRepository,
                studentRepository, enrollmentRepository, deviceRepository,
                userRepository, verificationLogRepository, qrCodeService,
                locationService, deviceService, appConfig
        );

        Department dept = new Department("CSE", "Computer Science");
        dept.setDepartmentId(1L);

        User studentUser = new User("rahul.kumar", "rahul@edu.com", "hash", User.Role.STUDENT);
        student = new StudentProfile(studentUser, "21CS101", "Rahul Kumar", dept, 3, "A");
        student.setStudentId(10L);

        User facultyUser = new User("prof.sharma", "sharma@edu.com", "hash", User.Role.FACULTY);
        FacultyProfile faculty = new FacultyProfile(facultyUser, "EMP-101", "Dr. Sharma", dept);
        Subject subject = new Subject("CS301", "DBMS", dept, 4);
        ClassSection section = new ClassSection(dept, "2026-2027", 3, "CSE-A");
        section.setSectionId(1L);

        session = new AttendanceSession(subject, faculty, section, null, LocalDate.now(), LocalTime.now(), LocalTime.now().plusHours(1));
        session.setSessionId(100L);
        session.setSessionStatus(AttendanceSession.SessionStatus.ACTIVE);

        qrToken = new QRToken(session, "token-hash-1234", LocalDateTime.now(), LocalDateTime.now().plusSeconds(30));
    }

    @Test
    @DisplayName("Process scan successfully creates PENDING reservation")
    public void testProcessScanSuccess() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(qrCodeService.validateScannedToken("valid-token")).thenReturn(qrToken);
        when(enrollmentRepository.existsByStudentStudentIdAndSectionSectionId(10L, 1L)).thenReturn(true);
        when(recordRepository.existsByStudentStudentIdAndSessionSessionId(10L, 100L)).thenReturn(false);
        when(attemptRepository.save(any(AttendanceAttempt.class))).thenAnswer(i -> {
            AttendanceAttempt att = i.getArgument(0);
            att.setAttemptId(55L);
            return att;
        });

        AttendanceResponse response = attendanceService.processScan(10L, "valid-token", "req-1");

        assertTrue(response.isSuccess());
        assertEquals("PENDING", response.getStatus());
        assertEquals(55L, response.getAttemptId());
    }

    @Test
    @DisplayName("Process scan FAILS when student is not enrolled in class section")
    public void testProcessScanNotEnrolled() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(qrCodeService.validateScannedToken("valid-token")).thenReturn(qrToken);
        when(enrollmentRepository.existsByStudentStudentIdAndSectionSectionId(10L, 1L)).thenReturn(false);

        assertThrows(AttendanceException.class, () -> attendanceService.processScan(10L, "valid-token", "req-1"));
    }

    @Test
    @DisplayName("Process scan FAILS when student has already marked attendance (Duplicate Prevention)")
    public void testProcessScanDuplicateAttendance() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(attemptRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(qrCodeService.validateScannedToken("valid-token")).thenReturn(qrToken);
        when(enrollmentRepository.existsByStudentStudentIdAndSectionSectionId(10L, 1L)).thenReturn(true);
        when(recordRepository.existsByStudentStudentIdAndSessionSessionId(10L, 100L)).thenReturn(true); // Already marked!

        assertThrows(AttendanceException.class, () -> attendanceService.processScan(10L, "valid-token", "req-1"));
    }
}

