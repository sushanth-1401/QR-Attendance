package com.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.attendance.config.AppConfig;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Department;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.QRToken;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.InvalidQRException;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.QRTokenRepository;
import com.attendance.util.QRTokenGenerator;

public class QRCodeServiceTest {

    private QRCodeService qrCodeService;
    private QRTokenRepository qrTokenRepository;
    private AttendanceSessionRepository sessionRepository;
    private QRTokenGenerator tokenGenerator;
    private AppConfig appConfig;
    private AttendanceSession activeSession;

    @BeforeEach
    public void setUp() {
        qrTokenRepository = mock(QRTokenRepository.class);
        sessionRepository = mock(AttendanceSessionRepository.class);
        tokenGenerator = new QRTokenGenerator();
        appConfig = new AppConfig();
        appConfig.getQr().setTokenValiditySeconds(30);

        qrCodeService = new QRCodeService(qrTokenRepository, sessionRepository, tokenGenerator, appConfig);

        Department dept = new Department("CSE", "Computer Science");
        User facultyUser = new User("prof.sharma", "sharma@edu.com", "hash", User.Role.FACULTY);
        FacultyProfile faculty = new FacultyProfile(facultyUser, "EMP-101", "Dr. Sharma", dept);
        Subject subject = new Subject("CS301", "DBMS", dept, 4);
        ClassSection section = new ClassSection(dept, "2026-2027", 3, "CSE-A");

        activeSession = new AttendanceSession(subject, faculty, section, null,
                LocalDate.now(), LocalTime.now(), LocalTime.now().plusHours(1));
        activeSession.setSessionStatus(AttendanceSession.SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Generates 256-bit dynamic token and QR image for active session")
    public void testGenerateTokenForSession() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        QRCodeService.GeneratedQRData qrData = qrCodeService.generateTokenForSession(1L);

        assertNotNull(qrData.rawToken());
        assertNotNull(qrData.tokenHash());
        assertNotNull(qrData.base64Image());
        assertTrue(qrData.base64Image().startsWith("data:image/png;base64,"));
        assertTrue(qrData.expiresInSeconds() <= 30);
    }

    @Test
    @DisplayName("Throws exception when generating QR for closed session")
    public void testGenerateQRForClosedSession() {
        activeSession.setSessionStatus(AttendanceSession.SessionStatus.CLOSED);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        assertThrows(AttendanceException.class, () -> qrCodeService.generateTokenForSession(1L));
    }

    @Test
    @DisplayName("Throws InvalidQRException when scanned token has expired")
    public void testValidateExpiredToken() {
        String rawToken = tokenGenerator.generateToken();
        String hash = tokenGenerator.hashToken(rawToken);

        // Expired 10 seconds ago
        QRToken tokenEntity = new QRToken(activeSession, hash,
                LocalDateTime.now().minusSeconds(40), LocalDateTime.now().minusSeconds(10));

        when(qrTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));

        assertThrows(InvalidQRException.class, () -> qrCodeService.validateScannedToken(rawToken));
    }
}

