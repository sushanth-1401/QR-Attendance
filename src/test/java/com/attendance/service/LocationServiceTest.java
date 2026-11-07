package com.attendance.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import com.attendance.config.AppConfig;
import com.attendance.dto.LocationRequest;
import com.attendance.entity.Classroom;
import com.attendance.repository.ClassroomRepository;
import com.attendance.util.DistanceCalculator;

public class LocationServiceTest {

    private LocationService locationService;
    private AppConfig appConfig;
    private Classroom classroom;

    @BeforeEach
    public void setUp() {
        appConfig = new AppConfig();
        appConfig.getGps().setDefaultAllowedRadiusMeters(50);
        appConfig.getGps().setMaxAccuracyMeters(100);
        appConfig.getGps().setMaxLocationAgeSeconds(60);

        ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
        DistanceCalculator distanceCalculator = new DistanceCalculator();

        locationService = new LocationService(appConfig, classroomRepository, distanceCalculator);

        classroom = new Classroom(
                "Room 401",
                "Academic Block",
                4,
                new BigDecimal("28.5458000"),
                new BigDecimal("77.1926000"),
                50
        );
    }

    @Test
    @DisplayName("GPS verification PASSES when student is within classroom radius")
    public void testLocationWithinRadius() {
        LocationRequest request = new LocationRequest(28.5458200, 77.1926100, 10.0, System.currentTimeMillis());
        LocationService.LocationVerificationResult result = locationService.verifyLocation(classroom, request);

        assertTrue(result.isPassed());
        assertTrue(result.getDistanceMeters() < 50.0);
        assertNotNull(result.getDetails());
    }

    @Test
    @DisplayName("GPS verification FAILS when student is far outside classroom radius")
    public void testLocationOutsideRadius() {
        // Coordinate 500 meters away
        LocationRequest request = new LocationRequest(28.5500000, 77.1926000, 10.0, System.currentTimeMillis());
        LocationService.LocationVerificationResult result = locationService.verifyLocation(classroom, request);

        assertFalse(result.isPassed());
        assertTrue(result.getDistanceMeters() > 50.0);
    }

    @Test
    @DisplayName("GPS verification FAILS when accuracy is too poor (> 100 meters)")
    public void testLocationPoorAccuracy() {
        LocationRequest request = new LocationRequest(28.5458000, 77.1926000, 150.0, System.currentTimeMillis());
        LocationService.LocationVerificationResult result = locationService.verifyLocation(classroom, request);

        assertFalse(result.isPassed());
        assertTrue(result.getDetails().contains("accuracy too poor"));
    }

    @Test
    @DisplayName("GPS verification FAILS on invalid coordinate boundaries")
    public void testInvalidCoordinates() {
        LocationRequest request = new LocationRequest(120.0, 77.1926000, 10.0, System.currentTimeMillis());
        LocationService.LocationVerificationResult result = locationService.verifyLocation(classroom, request);

        assertFalse(result.isPassed());
        assertTrue(result.isSuspicious());
    }
}

