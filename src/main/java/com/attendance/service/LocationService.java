package com.attendance.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.attendance.config.AppConfig;
import com.attendance.dto.LocationRequest;
import com.attendance.entity.Classroom;
import com.attendance.exception.AttendanceException;
import com.attendance.repository.ClassroomRepository;
import com.attendance.util.DistanceCalculator;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final AppConfig appConfig;
    private final ClassroomRepository classroomRepository;
    private final DistanceCalculator distanceCalculator;

    public LocationService(AppConfig appConfig,
                           ClassroomRepository classroomRepository,
                           DistanceCalculator distanceCalculator) {
        this.appConfig = appConfig;
        this.classroomRepository = classroomRepository;
        this.distanceCalculator = distanceCalculator;
    }

    public static class LocationVerificationResult {
        private final boolean passed;
        private final double distanceMeters;
        private final String details;
        private final boolean suspicious;

        public LocationVerificationResult(boolean passed, double distanceMeters, String details, boolean suspicious) {
            this.passed = passed;
            this.distanceMeters = distanceMeters;
            this.details = details;
            this.suspicious = suspicious;
        }

        public boolean isPassed() {
            return passed;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public String getDetails() {
            return details;
        }

        public boolean isSuspicious() {
            return suspicious;
        }
    }

    /**
     * Verifies that the student's reported GPS coordinates fall within the allowed radius of the classroom.
     */
    public LocationVerificationResult verifyLocation(Classroom classroom, LocationRequest request) {
        if (classroom == null) {
            log.warn("No classroom associated with session, skipping location verification");
            return new LocationVerificationResult(true, 0.0, "No classroom assigned to session; GPS bypassed.", false);
        }

        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return new LocationVerificationResult(false, 0.0, "Location coordinates were not provided.", true);
        }

        double lat = request.getLatitude();
        double lon = request.getLongitude();
        Double accuracy = request.getAccuracy() != null ? request.getAccuracy() : 50.0;

        // 1. Validate coordinate boundaries
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            return new LocationVerificationResult(false, 0.0,
                    String.format("Invalid coordinates: lat=%.6f, lon=%.6f", lat, lon), true);
        }

        // 2. Check accuracy threshold
        double maxAccuracy = appConfig.getGps().getMaxAccuracyMeters();
        if (accuracy > maxAccuracy) {
            return new LocationVerificationResult(false, 0.0,
                    String.format("GPS accuracy too poor: %.1fm (maximum allowed: %.1fm). Move closer to open area or window.",
                            accuracy, maxAccuracy), false);
        }

        // 3. Check freshness of location timestamp if provided
        if (request.getTimestamp() != null && request.getTimestamp() > 0) {
            long nowMillis = System.currentTimeMillis();
            long maxAgeMillis = appConfig.getGps().getMaxLocationAgeSeconds() * 1000L;
            long age = Math.abs(nowMillis - request.getTimestamp());
            if (age > maxAgeMillis) {
                return new LocationVerificationResult(false, 0.0,
                        String.format("Stale GPS timestamp detected (age: %d seconds). Please refresh location.", age / 1000), true);
            }
        }

        // 4. Calculate distance to classroom
        double classroomLat = classroom.getLatitude().doubleValue();
        double classroomLon = classroom.getLongitude().doubleValue();
        double distance = distanceCalculator.calculateDistance(lat, lon, classroomLat, classroomLon);

        double allowedRadius = classroom.getAllowedRadiusMeters() != null
                ? classroom.getAllowedRadiusMeters().doubleValue()
                : appConfig.getGps().getDefaultAllowedRadiusMeters();

        // 5. Verification evaluation: distance <= allowedRadius + tolerance for accuracy uncertainty
        boolean passed = distance <= (allowedRadius + (accuracy * 0.5));
        String details = String.format("Distance: %.2fm, Allowed Radius: %.0fm, Accuracy: ±%.1fm",
                distance, allowedRadius, accuracy);

        boolean suspicious = distance > (allowedRadius * 3); // Very far away is suspicious

        log.info("GPS verification result: passed={}, room={}, {}", passed, classroom.getRoomName(), details);
        return new LocationVerificationResult(passed, distance, details, suspicious);
    }
}

