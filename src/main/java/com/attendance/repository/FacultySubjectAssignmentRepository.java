package com.attendance.repository;

import com.attendance.entity.FacultySubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for FacultySubjectAssignment entity operations.
 */
@Repository
public interface FacultySubjectAssignmentRepository extends JpaRepository<FacultySubjectAssignment, Long> {

    List<FacultySubjectAssignment> findByFacultyFacultyId(Long facultyId);

    List<FacultySubjectAssignment> findBySubjectSubjectId(Long subjectId);

    List<FacultySubjectAssignment> findBySectionSectionId(Long sectionId);

    List<FacultySubjectAssignment> findByFacultyFacultyIdAndIsActive(Long facultyId, Boolean isActive);

    List<FacultySubjectAssignment> findByFacultyFacultyIdAndAcademicYear(Long facultyId, String academicYear);
}
