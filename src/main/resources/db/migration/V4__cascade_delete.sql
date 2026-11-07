-- ============================================================================
-- V4: Foreign Key CASCADE ON DELETE Constraints for H2 / MySQL Parity
-- ============================================================================

-- student_profiles -> users
ALTER TABLE student_profiles DROP CONSTRAINT IF EXISTS fk_student_user;
ALTER TABLE student_profiles ADD CONSTRAINT fk_student_user 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- faculty_profiles -> users
ALTER TABLE faculty_profiles DROP CONSTRAINT IF EXISTS fk_faculty_user;
ALTER TABLE faculty_profiles ADD CONSTRAINT fk_faculty_user 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- faculty_subject_assignments -> faculty_profiles
ALTER TABLE faculty_subject_assignments DROP CONSTRAINT IF EXISTS fk_assignment_faculty;
ALTER TABLE faculty_subject_assignments ADD CONSTRAINT fk_assignment_faculty 
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- faculty_subject_assignments -> subjects
ALTER TABLE faculty_subject_assignments DROP CONSTRAINT IF EXISTS fk_assignment_subject;
ALTER TABLE faculty_subject_assignments ADD CONSTRAINT fk_assignment_subject 
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_sessions -> faculty_profiles
ALTER TABLE attendance_sessions DROP CONSTRAINT IF EXISTS fk_session_faculty;
ALTER TABLE attendance_sessions ADD CONSTRAINT fk_session_faculty 
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- enrollments -> student_profiles
ALTER TABLE enrollments DROP CONSTRAINT IF EXISTS fk_enrollment_student;
ALTER TABLE enrollments ADD CONSTRAINT fk_enrollment_student 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- registered_devices -> student_profiles
ALTER TABLE registered_devices DROP CONSTRAINT IF EXISTS fk_device_student;
ALTER TABLE registered_devices ADD CONSTRAINT fk_device_student 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- qr_tokens -> attendance_sessions
ALTER TABLE qr_tokens DROP CONSTRAINT IF EXISTS fk_qr_session;
ALTER TABLE qr_tokens ADD CONSTRAINT fk_qr_session 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_attempts -> attendance_sessions
ALTER TABLE attendance_attempts DROP CONSTRAINT IF EXISTS fk_attempt_session;
ALTER TABLE attendance_attempts ADD CONSTRAINT fk_attempt_session 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_attempts -> student_profiles
ALTER TABLE attendance_attempts DROP CONSTRAINT IF EXISTS fk_attempt_student;
ALTER TABLE attendance_attempts ADD CONSTRAINT fk_attempt_student 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_records -> attendance_sessions
ALTER TABLE attendance_records DROP CONSTRAINT IF EXISTS fk_attendance_session;
ALTER TABLE attendance_records ADD CONSTRAINT fk_attendance_session 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_records -> student_profiles
ALTER TABLE attendance_records DROP CONSTRAINT IF EXISTS fk_attendance_student;
ALTER TABLE attendance_records ADD CONSTRAINT fk_attendance_student 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- verification_logs -> attendance_attempts
ALTER TABLE verification_logs DROP CONSTRAINT IF EXISTS fk_vlog_attempt;
ALTER TABLE verification_logs ADD CONSTRAINT fk_vlog_attempt 
    FOREIGN KEY (attempt_id) REFERENCES attendance_attempts(attempt_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- device_challenges -> registered_devices
ALTER TABLE device_challenges DROP CONSTRAINT IF EXISTS fk_challenge_device;
ALTER TABLE device_challenges ADD CONSTRAINT fk_challenge_device 
    FOREIGN KEY (device_id) REFERENCES registered_devices(device_id) ON DELETE CASCADE ON UPDATE CASCADE;

