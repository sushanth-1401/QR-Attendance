package com.attendance.dto;

/**
 * DTO for per-subject attendance summary for a student.
 */
public class AttendanceSummary {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private int totalSessions;
    private int presentCount;
    private int absentCount;
    private double attendancePercentage;

    public AttendanceSummary() {
    }

    public AttendanceSummary(Long subjectId, String subjectCode, String subjectName,
                             int totalSessions, int presentCount, int absentCount,
                             double attendancePercentage) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalSessions = totalSessions;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.attendancePercentage = attendancePercentage;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
