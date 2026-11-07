package com.attendance.repository;

import com.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AttendanceRecord entity operations.
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByStudentStudentIdAndSessionSessionId(Long studentId, Long sessionId);

    List<AttendanceRecord> findBySessionSessionId(Long sessionId);

    List<AttendanceRecord> findByStudentStudentId(Long studentId);

    long countBySessionSessionIdAndAttendanceStatus(Long sessionId,
                                                     AttendanceRecord.AttendanceStatus attendanceStatus);

    boolean existsByStudentStudentIdAndSessionSessionId(Long studentId, Long sessionId);

    /**
     * Calculates the attendance percentage for a student in a specific subject.
     * Returns the percentage of sessions where the student was marked PRESENT
     * out of the total sessions for that subject.
     *
     * @param studentId the student ID
     * @param subjectId the subject ID
     * @return the attendance percentage (0.0 to 100.0), or null if no records exist
     */
    @Query("SELECT (COUNT(CASE WHEN ar.attendanceStatus = 'PRESENT' THEN 1 END) * 100.0) / COUNT(ar) " +
           "FROM AttendanceRecord ar " +
           "WHERE ar.student.studentId = :studentId " +
           "AND ar.session.subject.subjectId = :subjectId")
    Double findAttendancePercentageByStudentAndSubject(@Param("studentId") Long studentId,
                                                       @Param("subjectId") Long subjectId);

    long countByStudentStudentIdAndAttendanceStatus(Long studentId,
                                                    AttendanceRecord.AttendanceStatus attendanceStatus);
}
