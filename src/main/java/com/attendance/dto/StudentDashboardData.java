package com.attendance.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for student dashboard data containing student info, attendance summaries,
 * recent records, and device registration status.
 */
public class StudentDashboardData {

    private Long studentId;
    private String studentName;
    private String registerNumber;
    private String departmentName;
    private Integer yearOfStudy;
    private String section;
    private double overallAttendancePercentage;
    private List<AttendanceSummary> subjectAttendanceSummaries;
    private List<RecentAttendanceRecord> recentAttendanceRecords;
    private boolean deviceRegistered;
    private int deviceCount;

    public StudentDashboardData() {
        this.subjectAttendanceSummaries = new ArrayList<>();
        this.recentAttendanceRecords = new ArrayList<>();
    }

    public StudentDashboardData(Long studentId, String studentName, String registerNumber,
                                String departmentName, Integer yearOfStudy, String section,
                                double overallAttendancePercentage,
                                List<AttendanceSummary> subjectAttendanceSummaries,
                                List<RecentAttendanceRecord> recentAttendanceRecords,
                                boolean deviceRegistered, int deviceCount) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.departmentName = departmentName;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.overallAttendancePercentage = overallAttendancePercentage;
        this.subjectAttendanceSummaries = subjectAttendanceSummaries != null ? subjectAttendanceSummaries : new ArrayList<>();
        this.recentAttendanceRecords = recentAttendanceRecords != null ? recentAttendanceRecords : new ArrayList<>();
        this.deviceRegistered = deviceRegistered;
        this.deviceCount = deviceCount;
    }

    /**
     * Inner class representing a recent attendance record for display on the dashboard.
     */
    public static class RecentAttendanceRecord {
        private String subjectName;
        private String subjectCode;
        private String sessionDate;
        private String status;
        private String facultyName;

        public RecentAttendanceRecord() {
        }

        public RecentAttendanceRecord(String subjectName, String subjectCode, String sessionDate,
                                      String status, String facultyName) {
            this.subjectName = subjectName;
            this.subjectCode = subjectCode;
            this.sessionDate = sessionDate;
            this.status = status;
            this.facultyName = facultyName;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public void setSubjectCode(String subjectCode) {
            this.subjectCode = subjectCode;
        }

        public String getSessionDate() {
            return sessionDate;
        }

        public void setSessionDate(String sessionDate) {
            this.sessionDate = sessionDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getFacultyName() {
            return facultyName;
        }

        public void setFacultyName(String facultyName) {
            this.facultyName = facultyName;
        }
    }

    // Getters and Setters

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public void setOverallAttendancePercentage(double overallAttendancePercentage) {
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public List<AttendanceSummary> getSubjectAttendanceSummaries() {
        return subjectAttendanceSummaries;
    }

    public void setSubjectAttendanceSummaries(List<AttendanceSummary> subjectAttendanceSummaries) {
        this.subjectAttendanceSummaries = subjectAttendanceSummaries;
    }

    public List<RecentAttendanceRecord> getRecentAttendanceRecords() {
        return recentAttendanceRecords;
    }

    public void setRecentAttendanceRecords(List<RecentAttendanceRecord> recentAttendanceRecords) {
        this.recentAttendanceRecords = recentAttendanceRecords;
    }

    public boolean isDeviceRegistered() {
        return deviceRegistered;
    }

    public void setDeviceRegistered(boolean deviceRegistered) {
        this.deviceRegistered = deviceRegistered;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }
}
