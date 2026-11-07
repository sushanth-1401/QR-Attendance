package com.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Qr qr = new Qr();
    private Gps gps = new Gps();
    private Device device = new Device();
    private Attendance attendance = new Attendance();
    private RateLimit rateLimit = new RateLimit();

    public Qr getQr() {
        return qr;
    }

    public void setQr(Qr qr) {
        this.qr = qr;
    }

    public Gps getGps() {
        return gps;
    }

    public void setGps(Gps gps) {
        this.gps = gps;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Qr {
        private int tokenValiditySeconds = 30;
        private int reservationValiditySeconds = 60;
        private int tokenLengthBits = 256;
        private int refreshIntervalSeconds = 10;

        public int getTokenValiditySeconds() {
            return tokenValiditySeconds;
        }

        public void setTokenValiditySeconds(int tokenValiditySeconds) {
            this.tokenValiditySeconds = tokenValiditySeconds;
        }

        public int getReservationValiditySeconds() {
            return reservationValiditySeconds;
        }

        public void setReservationValiditySeconds(int reservationValiditySeconds) {
            this.reservationValiditySeconds = reservationValiditySeconds;
        }

        public int getTokenLengthBits() {
            return tokenLengthBits;
        }

        public void setTokenLengthBits(int tokenLengthBits) {
            this.tokenLengthBits = tokenLengthBits;
        }

        public int getRefreshIntervalSeconds() {
            return refreshIntervalSeconds;
        }

        public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
            this.refreshIntervalSeconds = refreshIntervalSeconds;
        }
    }

    public static class Gps {
        private double defaultAllowedRadiusMeters = 100.0;
        private double maxAccuracyMeters = 50.0;
        private int maxLocationAgeSeconds = 30;

        public double getDefaultAllowedRadiusMeters() {
            return defaultAllowedRadiusMeters;
        }

        public void setDefaultAllowedRadiusMeters(double defaultAllowedRadiusMeters) {
            this.defaultAllowedRadiusMeters = defaultAllowedRadiusMeters;
        }

        public double getMaxAccuracyMeters() {
            return maxAccuracyMeters;
        }

        public void setMaxAccuracyMeters(double maxAccuracyMeters) {
            this.maxAccuracyMeters = maxAccuracyMeters;
        }

        public int getMaxLocationAgeSeconds() {
            return maxLocationAgeSeconds;
        }

        public void setMaxLocationAgeSeconds(int maxLocationAgeSeconds) {
            this.maxLocationAgeSeconds = maxLocationAgeSeconds;
        }
    }

    public static class Device {
        private int maxDevicesPerStudent = 2;
        private int challengeValiditySeconds = 300;
        private String algorithm = "EC";
        private String ecCurve = "secp256r1";

        public int getMaxDevicesPerStudent() {
            return maxDevicesPerStudent;
        }

        public void setMaxDevicesPerStudent(int maxDevicesPerStudent) {
            this.maxDevicesPerStudent = maxDevicesPerStudent;
        }

        public int getChallengeValiditySeconds() {
            return challengeValiditySeconds;
        }

        public void setChallengeValiditySeconds(int challengeValiditySeconds) {
            this.challengeValiditySeconds = challengeValiditySeconds;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getEcCurve() {
            return ecCurve;
        }

        public void setEcCurve(String ecCurve) {
            this.ecCurve = ecCurve;
        }
    }

    public static class Attendance {
        private double lowThresholdPercentage = 75.0;

        public double getLowThresholdPercentage() {
            return lowThresholdPercentage;
        }

        public void setLowThresholdPercentage(double lowThresholdPercentage) {
            this.lowThresholdPercentage = lowThresholdPercentage;
        }
    }

    public static class RateLimit {
        private int scanAttemptsPerMinute = 10;
        private int loginAttemptsPerMinute = 5;

        public int getScanAttemptsPerMinute() {
            return scanAttemptsPerMinute;
        }

        public void setScanAttemptsPerMinute(int scanAttemptsPerMinute) {
            this.scanAttemptsPerMinute = scanAttemptsPerMinute;
        }

        public int getLoginAttemptsPerMinute() {
            return loginAttemptsPerMinute;
        }

        public void setLoginAttemptsPerMinute(int loginAttemptsPerMinute) {
            this.loginAttemptsPerMinute = loginAttemptsPerMinute;
        }
    }
}
