package com.attendance.repository;

import com.attendance.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserUserId(Long userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Find audit logs created within a specific date/time range.
     *
     * @param startDateTime the start of the range (inclusive)
     * @param endDateTime   the end of the range (inclusive)
     * @return list of audit logs within the specified range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDateTime AND :endDateTime ORDER BY a.createdAt DESC")
    List<AuditLog> findByCreatedAtBetween(@Param("startDateTime") LocalDateTime startDateTime,
                                          @Param("endDateTime") LocalDateTime endDateTime);
}
