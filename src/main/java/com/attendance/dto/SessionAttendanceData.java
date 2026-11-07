package com.attendance.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for session attendance data, including session details and lists of
 * present/absent students.
 */
public class SessionAttendanceData {

    // Session details
    private Long sessionId;
    private String subjectCode;
    private String subjectName;
    private String sectionName;
    private String facultyName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String sessionStatus;

    // Attendance data
    private List<StudentAttendanceInfo> presentStudents;
    private List<StudentAttendanceInfo> absentStudents;
    private int presentCount;
    private int absentCount;
    private int totalEnrolled;

    public SessionAttendanceData() {
        this.presentStudents = new ArrayList<>();
        this.absentStudents = new ArrayList<>();
    }

    public SessionAttendanceData(Long sessionId, String subjectCode, String subjectName,
                                 String sectionName, String facultyName,
                                 LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
                                 String sessionStatus,
                                 List<StudentAttendanceInfo> presentStudents,
                                 List<StudentAttendanceInfo> absentStudents,
                                 int presentCount, int absentCount, int totalEnrolled) {
        this.sessionId = sessionId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.sectionName = sectionName;
        this.facultyName = facultyName;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionStatus = sessionStatus;
        this.presentStudents = presentStudents != null ? presentStudents : new ArrayList<>();
        this.absentStudents = absentStudents != null ? absentStudents : new ArrayList<>();
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalEnrolled = totalEnrolled;
    }

    /**
     * Inner class representing individual student attendance information within a session.
     */
    public static class StudentAttendanceInfo {
        private Long studentId;
        private String registerNumber;
        private String studentName;
        private String attendanceStatus;
        private String markedAt;

        public StudentAttendanceInfo() {
        }

        public StudentAttendanceInfo(Long studentId, String registerNumber, String studentName,
                                     String attendanceStatus, String markedAt) {
            this.studentId = studentId;
            this.registerNumber = registerNumber;
            this.studentName = studentName;
            this.attendanceStatus = attendanceStatus;
            this.markedAt = markedAt;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getRegisterNumber() {
            return registerNumber;
        }

        public void setRegisterNumber(String registerNumber) {
            this.registerNumber = registerNumber;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getAttendanceStatus() {
            return attendanceStatus;
        }

        public void setAttendanceStatus(String attendanceStatus) {
            this.attendanceStatus = attendanceStatus;
        }

        public String getMarkedAt() {
            return markedAt;
        }

        public void setMarkedAt(String markedAt) {
            this.markedAt = markedAt;
        }
    }

    // Getters and Setters

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public List<StudentAttendanceInfo> getPresentStudents() {
        return presentStudents;
    }

    public void setPresentStudents(List<StudentAttendanceInfo> presentStudents) {
        this.presentStudents = presentStudents;
    }

    public List<StudentAttendanceInfo> getAbsentStudents() {
        return absentStudents;
    }

    public void setAbsentStudents(List<StudentAttendanceInfo> absentStudents) {
        this.absentStudents = absentStudents;
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

    public int getTotalEnrolled() {
        return totalEnrolled;
    }

    public void setTotalEnrolled(int totalEnrolled) {
        this.totalEnrolled = totalEnrolled;
    }
}
