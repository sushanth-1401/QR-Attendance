package com.attendance.repository;

import com.attendance.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Subject entity operations.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySubjectCode(String subjectCode);

    List<Subject> findByDepartmentDepartmentId(Long departmentId);

    List<Subject> findByIsActive(Boolean isActive);

    boolean existsBySubjectCode(String subjectCode);
}
