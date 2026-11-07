package com.attendance.repository;

import com.attendance.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for VerificationLog entity operations.
 */
@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    List<VerificationLog> findByAttemptAttemptId(Long attemptId);

    List<VerificationLog> findByVerificationTypeAndVerificationResult(
            VerificationLog.VerificationType verificationType,
            VerificationLog.VerificationResult verificationResult);
}
