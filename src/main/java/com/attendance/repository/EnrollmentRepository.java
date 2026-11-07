package com.attendance.repository;

import com.attendance.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Enrollment entity operations.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentStudentId(Long studentId);

    List<Enrollment> findBySectionSectionId(Long sectionId);

    Optional<Enrollment> findByStudentStudentIdAndSectionSectionId(Long studentId, Long sectionId);

    boolean existsByStudentStudentIdAndSectionSectionId(Long studentId, Long sectionId);

    List<Enrollment> findByStudentStudentIdAndIsActive(Long studentId, Boolean isActive);
}
