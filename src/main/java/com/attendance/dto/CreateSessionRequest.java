package com.attendance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new attendance session by faculty.
 */
public class CreateSessionRequest {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Section ID is required")
    private Long sectionId;

    private Long classroomId;

    private String sessionDate;

    private String startTime;

    private String endTime;

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(Long subjectId, Long sectionId, Long classroomId,
                                String sessionDate, String startTime, String endTime) {
        this.subjectId = subjectId;
        this.sectionId = sectionId;
        this.classroomId = classroomId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Long classroomId) {
        this.classroomId = classroomId;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
