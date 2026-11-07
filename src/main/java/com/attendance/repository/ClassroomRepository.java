package com.attendance.repository;

import com.attendance.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Classroom entity operations.
 */
@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByIsActive(Boolean isActive);

    List<Classroom> findByBuildingName(String buildingName);

    boolean existsByRoomNameAndBuildingName(String roomName, String buildingName);
}
