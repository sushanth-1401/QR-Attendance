package com.attendance.repository;

import com.attendance.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ClassSection entity operations.
 */
@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

    List<ClassSection> findByDepartmentDepartmentId(Long departmentId);

    List<ClassSection> findByDepartmentDepartmentIdAndAcademicYear(Long departmentId, String academicYear);

    List<ClassSection> findByIsActive(Boolean isActive);
}
