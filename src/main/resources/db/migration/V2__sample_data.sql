-- ============================================
-- SCSVMV Portal
-- University Structural Master Data & Setup
-- Sri Chandrasekharendra Saraswathi Viswa Mahavidyalaya
-- ============================================

-- 1. DEPARTMENTS
INSERT INTO departments (department_id, department_code, department_name, is_active) VALUES
(1, 'CSE', 'Computer Science & Engineering', TRUE),
(2, 'IT', 'Information Technology', TRUE),
(3, 'ECE', 'Electronics & Communication Engineering', TRUE),
(4, 'EEE', 'Electrical & Electronics Engineering', TRUE),
(5, 'MECH', 'Mechanical Engineering', TRUE),
(6, 'CIVIL', 'Civil Engineering', TRUE),
(7, 'MATHS', 'Mathematics & Computer Applications', TRUE),
(8, 'MBA', 'Department of Management Studies', TRUE);

-- 2. CLASS SECTIONS
INSERT INTO class_sections (section_id, department_id, academic_year, year_of_study, section_name, is_active) VALUES
(1, 1, '2026-2027', 3, 'CSE-A', TRUE),
(2, 1, '2026-2027', 3, 'CSE-B', TRUE),
(3, 2, '2026-2027', 3, 'IT-A', TRUE),
(4, 3, '2026-2027', 3, 'ECE-A', TRUE);

-- 3. CLASSROOMS & GEOFENCES (SCSVMV Enathur, Kanchipuram Campus: 12.8342 N, 79.7036 E)
INSERT INTO classrooms (classroom_id, room_name, building_name, floor_number, latitude, longitude, allowed_radius_meters, is_active) VALUES
(1, 'Lab 101', 'Computing Block', 1, 12.8342000, 79.7036000, 80, TRUE),
(2, 'LH-201', 'Main Academic Block', 2, 12.8345000, 79.7040000, 80, TRUE),
(3, 'Seminar Hall', 'Silver Jubilee Block', 1, 12.8340000, 79.7030000, 100, TRUE);

-- 4. CORE USERS (Default password for all initialized accounts: 'admin123')
INSERT INTO users (user_id, username, email, password_hash, role, account_status) VALUES
(1, 'admin', 'admin@scsvmv.ac.in', '$2a$10$ZEtOnCHrdScYNfWn2CZeC.oireypYahxRSXPEh8ycOOFEt4sZrlxK', 'ADMIN', 'ACTIVE'),
(2, 'senthil-001', 'senthil@kanchiuniv.ac.in', '$2a$10$ZEtOnCHrdScYNfWn2CZeC.oireypYahxRSXPEh8ycOOFEt4sZrlxK', 'FACULTY', 'ACTIVE'),
(3, 'student-001', 'student@kanchiuniv.ac.in', '$2a$10$ZEtOnCHrdScYNfWn2CZeC.oireypYahxRSXPEh8ycOOFEt4sZrlxK', 'STUDENT', 'ACTIVE'),
(4, 'sasank-7799', '11249m010@kanchiuniv.ac.in', '$2a$10$ZEtOnCHrdScYNfWn2CZeC.oireypYahxRSXPEh8ycOOFEt4sZrlxK', 'STUDENT', 'ACTIVE');

-- 5. FACULTY PROFILE (Dr./Prof. Senthilkumaran)
INSERT INTO faculty_profiles (faculty_id, user_id, employee_id, full_name, department_id) VALUES
(1, 2, 'SCSVMV-001', 'Senthilkumaran', 1);

-- 6. STUDENT PROFILE
INSERT INTO student_profiles (student_id, user_id, register_number, full_name, department_id, year_of_study, section) VALUES
(1, 3, '21CS001', 'Student SCSVMV', 1, 3, 'A'),
(2, 4, '11249M010', 'Sasank', 2, 3, 'G');

-- 7. SUBJECTS
INSERT INTO subjects (subject_id, subject_code, subject_name, department_id, credits, is_active) VALUES
(1, 'CS301', 'Database Management Systems', 1, 4, TRUE),
(2, 'CS302', 'Operating Systems', 1, 4, TRUE),
(3, 'IT301', 'Web Technology & Cloud Applications', 2, 3, TRUE);

-- 8. FACULTY SUBJECT ASSIGNMENTS
INSERT INTO faculty_subject_assignments (assignment_id, faculty_id, subject_id, section_id, academic_year, is_active) VALUES
(1, 1, 1, 1, '2026-2027', TRUE); -- Senthilkumaran -> DBMS for CSE-A

-- 9. ENROLLMENTS
INSERT INTO enrollments (enrollment_id, student_id, section_id, is_active) VALUES
(1, 1, 1, TRUE),
(2, 2, 3, TRUE);

-- 10. ACTIVE ATTENDANCE SESSION FOR TODAY
INSERT INTO attendance_sessions (session_id, subject_id, faculty_id, section_id, classroom_id, session_date, start_time, end_time, session_status) VALUES
(1, 1, 1, 1, 1, CURRENT_DATE, '09:00:00', '10:00:00', 'ACTIVE');

-- 11. AUDIT LOG
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description, ip_address) VALUES
(1, 'SYSTEM_INIT', 'System', 1, 'SCSVMV University institutional departments, faculty, and academic hierarchy initialized', '127.0.0.1');
