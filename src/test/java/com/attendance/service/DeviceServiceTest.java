package com.attendance.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.attendance.config.AppConfig;
import com.attendance.dto.DeviceChallengeResponse;
import com.attendance.entity.Department;
import com.attendance.entity.DeviceChallenge;
import com.attendance.entity.RegisteredDevice;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.User;
import com.attendance.repository.DeviceChallengeRepository;
import com.attendance.repository.DeviceRepository;

public class DeviceServiceTest {

    private DeviceService deviceService;
    private DeviceRepository deviceRepository;
    private DeviceChallengeRepository challengeRepository;
    private RegisteredDevice device;
    private StudentProfile student;

    @BeforeEach
    public void setUp() {
        deviceRepository = mock(DeviceRepository.class);
        challengeRepository = mock(DeviceChallengeRepository.class);
        AppConfig appConfig = new AppConfig();
        appConfig.getDevice().setMaxDevicesPerStudent(2);
        appConfig.getDevice().setChallengeValiditySeconds(60);

        deviceService = new DeviceService(deviceRepository, challengeRepository, appConfig);

        Department dept = new Department("CSE", "Computer Science");
        User user = new User("rahul.kumar", "rahul@edu.com", "hash", User.Role.STUDENT);
        user.setUserId(10L);

        student = new StudentProfile(user, "21CS101", "Rahul Kumar", dept, 3, "A");
        student.setStudentId(10L);

        device = new RegisteredDevice(student, "Rahul Pixel", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...", "EC");
        device.setDeviceId(1L);
    }

    @Test
    @DisplayName("Generates random single-use challenge for active registered device")
    public void testGenerateChallenge() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(challengeRepository.save(any(DeviceChallenge.class))).thenAnswer(i -> {
            DeviceChallenge c = i.getArgument(0);
            c.setChallengeId(100L);
            return c;
        });

        DeviceChallengeResponse response = deviceService.generateChallenge(1L, 10L);

        assertNotNull(response.getChallenge());
        assertTrue(response.getChallenge().length() >= 32);
        assertTrue(response.getExpiresInSeconds() <= 60);
    }

    @Test
    @DisplayName("Challenge verification FAILS when challenge has already been consumed (Replay Detection)")
    public void testReplayedChallengeFails() {
        DeviceChallenge challenge = new DeviceChallenge(device, "random-val", "hash", LocalDateTime.now().plusSeconds(60));
        challenge.setChallengeId(50L);
        challenge.setConsumed(true); // Already consumed!

        when(challengeRepository.findById(50L)).thenReturn(Optional.of(challenge));

        boolean verified = deviceService.verifyChallenge(50L, 1L, "dummy-signature");
        assertFalse(verified, "Consumed challenge must be rejected immediately to prevent replay attack");
    }

    @Test
    @DisplayName("Challenge verification FAILS when challenge deadline has expired")
    public void testExpiredChallengeFails() {
        // Expired 10 seconds ago
        DeviceChallenge challenge = new DeviceChallenge(device, "random-val", "hash", LocalDateTime.now().minusSeconds(10));
        challenge.setChallengeId(51L);
        challenge.setConsumed(false);

        when(challengeRepository.findById(51L)).thenReturn(Optional.of(challenge));

        boolean verified = deviceService.verifyChallenge(51L, 1L, "dummy-signature");
        assertFalse(verified, "Expired challenge must be rejected");
    }
}

