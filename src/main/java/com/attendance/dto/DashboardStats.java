package com.attendance.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for admin dashboard statistics.
 */
public class DashboardStats {

    private long totalStudents;
    private long totalFaculty;
    private long totalSubjects;
    private long totalSessions;
    private long activeSessions;
    private double overallAttendancePercentage;
    private List<Map<String, Object>> recentActivity;
    private List<Map<String, Object>> departmentStats;

    public DashboardStats() {
        this.recentActivity = new ArrayList<>();
        this.departmentStats = new ArrayList<>();
    }

    public DashboardStats(long totalStudents, long totalFaculty, long totalSubjects,
                          long totalSessions, long activeSessions, double overallAttendancePercentage,
                          List<Map<String, Object>> recentActivity,
                          List<Map<String, Object>> departmentStats) {
        this.totalStudents = totalStudents;
        this.totalFaculty = totalFaculty;
        this.totalSubjects = totalSubjects;
        this.totalSessions = totalSessions;
        this.activeSessions = activeSessions;
        this.overallAttendancePercentage = overallAttendancePercentage;
        this.recentActivity = recentActivity != null ? recentActivity : new ArrayList<>();
        this.departmentStats = departmentStats != null ? departmentStats : new ArrayList<>();
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalFaculty() {
        return totalFaculty;
    }

    public void setTotalFaculty(long totalFaculty) {
        this.totalFaculty = totalFaculty;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(long activeSessions) {
        this.activeSessions = activeSessions;
    }

    public double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public void setOverallAttendancePercentage(double overallAttendancePercentage) {
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public List<Map<String, Object>> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<Map<String, Object>> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<Map<String, Object>> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(List<Map<String, Object>> departmentStats) {
        this.departmentStats = departmentStats;
    }
}
