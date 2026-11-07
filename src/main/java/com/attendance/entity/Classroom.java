package com.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "classrooms")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column(name = "building_name", length = 100)
    private String buildingName;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "allowed_radius_meters")
    private Integer allowedRadiusMeters;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Classroom() {
    }

    public Classroom(String roomName, String buildingName, Integer floorNumber,
                      BigDecimal latitude, BigDecimal longitude, Integer allowedRadiusMeters) {
        this.roomName = roomName;
        this.buildingName = buildingName;
        this.floorNumber = floorNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.isActive = true;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Long classroomId) {
        this.classroomId = classroomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public void setAllowedRadiusMeters(Integer allowedRadiusMeters) {
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Classroom{" +
                "classroomId=" + classroomId +
                ", roomName='" + roomName + '\'' +
                ", buildingName='" + buildingName + '\'' +
                ", floorNumber=" + floorNumber +
                ", isActive=" + isActive +
                '}';
    }
}
