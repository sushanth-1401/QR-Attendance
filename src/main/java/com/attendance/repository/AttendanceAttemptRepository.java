package com.attendance.repository;

import com.attendance.entity.AttendanceAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AttendanceAttempt entity operations.
 */
@Repository
public interface AttendanceAttemptRepository extends JpaRepository<AttendanceAttempt, Long> {

    Optional<AttendanceAttempt> findByRequestId(String requestId);

    List<AttendanceAttempt> findByStudentStudentIdAndSessionSessionId(Long studentId, Long sessionId);

    List<AttendanceAttempt> findBySessionSessionIdAndVerificationStatus(Long sessionId,
                                                                        AttendanceAttempt.VerificationStatus verificationStatus);

    List<AttendanceAttempt> findByVerificationStatus(AttendanceAttempt.VerificationStatus verificationStatus);

    long countBySessionSessionId(Long sessionId);

    List<AttendanceAttempt> findByStudentStudentId(Long studentId);
}
