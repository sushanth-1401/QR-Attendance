package com.attendance.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DistanceCalculatorTest {

    private DistanceCalculator distanceCalculator;

    @BeforeEach
    public void setUp() {
        distanceCalculator = new DistanceCalculator();
    }

    @Test
    @DisplayName("Identical coordinates should yield 0 meters distance")
    public void testSameCoordinates() {
        double lat = 28.5458000;
        double lon = 77.1926000;
        double distance = distanceCalculator.calculateDistance(lat, lon, lat, lon);
        assertEquals(0.0, distance, 0.01);
    }

    @Test
    @DisplayName("Calculates distance between classroom and student within 50 meters accurately")
    public void testShortDistance() {
        // Points ~25 meters apart
        double classroomLat = 28.5458000;
        double classroomLon = 77.1926000;
        double studentLat = 28.5460000;
        double studentLon = 77.1926000;

        double distance = distanceCalculator.calculateDistance(classroomLat, classroomLon, studentLat, studentLon);
        assertTrue(distance > 20.0 && distance < 25.0, "Calculated distance should be approximately 22 meters");
    }

    @Test
    @DisplayName("Calculates long distance accurately across locations")
    public void testLongDistance() {
        // Delhi (28.6139, 77.2090) to Mumbai (19.0760, 72.8777) ~1150 km
        double distance = distanceCalculator.calculateDistance(28.6139, 77.2090, 19.0760, 72.8777);
        assertTrue(distance > 1100000 && distance < 1200000, "Distance should be approximately 1150 km");
    }
}

