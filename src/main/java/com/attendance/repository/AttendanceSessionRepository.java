package com.attendance.repository;

import com.attendance.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for AttendanceSession entity operations.
 */
@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    List<AttendanceSession> findByFacultyFacultyId(Long facultyId);

    List<AttendanceSession> findBySessionDate(LocalDate sessionDate);

    List<AttendanceSession> findBySessionStatus(AttendanceSession.SessionStatus sessionStatus);

    List<AttendanceSession> findByFacultyFacultyIdAndSessionDate(Long facultyId, LocalDate sessionDate);

    List<AttendanceSession> findByFacultyFacultyIdAndSessionStatus(Long facultyId,
                                                                    AttendanceSession.SessionStatus sessionStatus);

    List<AttendanceSession> findBySectionSectionIdAndSessionDate(Long sectionId, LocalDate sessionDate);

    long countBySessionStatus(AttendanceSession.SessionStatus sessionStatus);

    List<AttendanceSession> findBySubjectSubjectId(Long subjectId);
}
