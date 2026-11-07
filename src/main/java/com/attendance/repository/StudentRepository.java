package com.attendance.repository;

import com.attendance.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StudentProfile entity operations.
 */
@Repository
public interface StudentRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUser(com.attendance.entity.User user);

    Optional<StudentProfile> findByUserUserId(Long userId);

    Optional<StudentProfile> findByRegisterNumber(String registerNumber);

    List<StudentProfile> findByDepartmentDepartmentId(Long departmentId);

    List<StudentProfile> findByYearOfStudy(Integer yearOfStudy);

    List<StudentProfile> findByDepartmentDepartmentIdAndYearOfStudyAndSection(
            Long departmentId, Integer yearOfStudy, String section);

    long countByDepartmentDepartmentId(Long departmentId);
}
