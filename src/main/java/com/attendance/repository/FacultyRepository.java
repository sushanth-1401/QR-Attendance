package com.attendance.repository;

import com.attendance.entity.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FacultyProfile entity operations.
 */
@Repository
public interface FacultyRepository extends JpaRepository<FacultyProfile, Long> {

    Optional<FacultyProfile> findByUser(com.attendance.entity.User user);

    Optional<FacultyProfile> findByUserUserId(Long userId);

    Optional<FacultyProfile> findByEmployeeId(String employeeId);

    List<FacultyProfile> findByDepartmentDepartmentId(Long departmentId);

    long countByDepartmentDepartmentId(Long departmentId);
}
