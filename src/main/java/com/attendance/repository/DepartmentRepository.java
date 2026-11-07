package com.attendance.repository;

import com.attendance.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Department entity operations.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDepartmentCode(String departmentCode);

    List<Department> findByIsActive(Boolean isActive);

    boolean existsByDepartmentCode(String departmentCode);
}
