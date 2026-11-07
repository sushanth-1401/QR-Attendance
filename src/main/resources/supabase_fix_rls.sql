-- ============================================================================
-- SCSVMV Portal - Supabase RLS, CASCADE Foreign Keys & Permissions Script
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/rsxttzenmxkuwrfrbkbv/sql
-- ============================================================================

-- 0. CASCADE ON DELETE CONSTRAINTS
-- Fixes foreign key violations when deleting users in Supabase Table Editor or UI
-- faculty_profiles -> users
ALTER TABLE IF EXISTS faculty_profiles DROP CONSTRAINT IF EXISTS faculty_profiles_user_id_fkey;
ALTER TABLE IF EXISTS faculty_profiles DROP CONSTRAINT IF EXISTS fk_faculty_user;
ALTER TABLE IF EXISTS faculty_profiles ADD CONSTRAINT faculty_profiles_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- student_profiles -> users
ALTER TABLE IF EXISTS student_profiles DROP CONSTRAINT IF EXISTS student_profiles_user_id_fkey;
ALTER TABLE IF EXISTS student_profiles DROP CONSTRAINT IF EXISTS fk_student_user;
ALTER TABLE IF EXISTS student_profiles ADD CONSTRAINT student_profiles_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- faculty_subject_assignments -> faculty_profiles
ALTER TABLE IF EXISTS faculty_subject_assignments DROP CONSTRAINT IF EXISTS faculty_subject_assignments_faculty_id_fkey;
ALTER TABLE IF EXISTS faculty_subject_assignments DROP CONSTRAINT IF EXISTS fk_assignment_faculty;
ALTER TABLE IF EXISTS faculty_subject_assignments ADD CONSTRAINT faculty_subject_assignments_faculty_id_fkey 
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- faculty_subject_assignments -> subjects
ALTER TABLE IF EXISTS faculty_subject_assignments DROP CONSTRAINT IF EXISTS faculty_subject_assignments_subject_id_fkey;
ALTER TABLE IF EXISTS faculty_subject_assignments DROP CONSTRAINT IF EXISTS fk_assignment_subject;
ALTER TABLE IF EXISTS faculty_subject_assignments ADD CONSTRAINT faculty_subject_assignments_subject_id_fkey 
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_sessions -> faculty_profiles
ALTER TABLE IF EXISTS attendance_sessions DROP CONSTRAINT IF EXISTS attendance_sessions_faculty_id_fkey;
ALTER TABLE IF EXISTS attendance_sessions DROP CONSTRAINT IF EXISTS fk_session_faculty;
ALTER TABLE IF EXISTS attendance_sessions ADD CONSTRAINT attendance_sessions_faculty_id_fkey 
    FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(faculty_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- enrollments -> student_profiles
ALTER TABLE IF EXISTS enrollments DROP CONSTRAINT IF EXISTS enrollments_student_id_fkey;
ALTER TABLE IF EXISTS enrollments DROP CONSTRAINT IF EXISTS fk_enrollment_student;
ALTER TABLE IF EXISTS enrollments ADD CONSTRAINT enrollments_student_id_fkey 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- registered_devices -> student_profiles
ALTER TABLE IF EXISTS registered_devices DROP CONSTRAINT IF EXISTS registered_devices_student_id_fkey;
ALTER TABLE IF EXISTS registered_devices DROP CONSTRAINT IF EXISTS fk_device_student;
ALTER TABLE IF EXISTS registered_devices ADD CONSTRAINT registered_devices_student_id_fkey 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- qr_tokens -> attendance_sessions
ALTER TABLE IF EXISTS qr_tokens DROP CONSTRAINT IF EXISTS qr_tokens_session_id_fkey;
ALTER TABLE IF EXISTS qr_tokens DROP CONSTRAINT IF EXISTS fk_qr_session;
ALTER TABLE IF EXISTS qr_tokens ADD CONSTRAINT qr_tokens_session_id_fkey 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_attempts -> attendance_sessions
ALTER TABLE IF EXISTS attendance_attempts DROP CONSTRAINT IF EXISTS attendance_attempts_session_id_fkey;
ALTER TABLE IF EXISTS attendance_attempts DROP CONSTRAINT IF EXISTS fk_attempt_session;
ALTER TABLE IF EXISTS attendance_attempts ADD CONSTRAINT attendance_attempts_session_id_fkey 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_attempts -> student_profiles
ALTER TABLE IF EXISTS attendance_attempts DROP CONSTRAINT IF EXISTS attendance_attempts_student_id_fkey;
ALTER TABLE IF EXISTS attendance_attempts DROP CONSTRAINT IF EXISTS fk_attempt_student;
ALTER TABLE IF EXISTS attendance_attempts ADD CONSTRAINT attendance_attempts_student_id_fkey 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_records -> attendance_sessions
ALTER TABLE IF EXISTS attendance_records DROP CONSTRAINT IF EXISTS attendance_records_session_id_fkey;
ALTER TABLE IF EXISTS attendance_records DROP CONSTRAINT IF EXISTS fk_attendance_session;
ALTER TABLE IF EXISTS attendance_records ADD CONSTRAINT attendance_records_session_id_fkey 
    FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- attendance_records -> student_profiles
ALTER TABLE IF EXISTS attendance_records DROP CONSTRAINT IF EXISTS attendance_records_student_id_fkey;
ALTER TABLE IF EXISTS attendance_records DROP CONSTRAINT IF EXISTS fk_attendance_student;
ALTER TABLE IF EXISTS attendance_records ADD CONSTRAINT attendance_records_student_id_fkey 
    FOREIGN KEY (student_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- verification_logs -> attendance_attempts
ALTER TABLE IF EXISTS verification_logs DROP CONSTRAINT IF EXISTS verification_logs_attempt_id_fkey;
ALTER TABLE IF EXISTS verification_logs DROP CONSTRAINT IF EXISTS fk_vlog_attempt;
ALTER TABLE IF EXISTS verification_logs ADD CONSTRAINT verification_logs_attempt_id_fkey 
    FOREIGN KEY (attempt_id) REFERENCES attendance_attempts(attempt_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- device_challenges -> registered_devices
ALTER TABLE IF EXISTS device_challenges DROP CONSTRAINT IF EXISTS device_challenges_device_id_fkey;
ALTER TABLE IF EXISTS device_challenges DROP CONSTRAINT IF EXISTS fk_challenge_device;
ALTER TABLE IF EXISTS device_challenges ADD CONSTRAINT device_challenges_device_id_fkey 
    FOREIGN KEY (device_id) REFERENCES registered_devices(device_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- 1. Disable Row Level Security on all application tables
ALTER TABLE IF EXISTS departments DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS users DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS faculty_profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS student_profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS classrooms DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS class_sections DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS subjects DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS faculty_subject_assignments DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS enrollments DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS attendance_sessions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS qr_tokens DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS registered_devices DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS attendance_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS attendance_attempts DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS verification_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS audit_logs DISABLE ROW LEVEL SECURITY;

-- 2. Grant full table and sequence access to anon and authenticated roles
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO anon, authenticated, service_role;

-- 3. Synchronize auto-increment sequences above the maximum existing IDs
SELECT setval(pg_get_serial_sequence('departments', 'department_id'), coalesce(max(department_id), 1)) FROM departments;
SELECT setval(pg_get_serial_sequence('classrooms', 'classroom_id'), coalesce(max(classroom_id), 1)) FROM classrooms;
SELECT setval(pg_get_serial_sequence('users', 'user_id'), coalesce(max(user_id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('class_sections', 'section_id'), coalesce(max(section_id), 1)) FROM class_sections;
SELECT setval(pg_get_serial_sequence('subjects', 'subject_id'), coalesce(max(subject_id), 1)) FROM subjects;
SELECT setval(pg_get_serial_sequence('faculty_profiles', 'faculty_id'), coalesce(max(faculty_id), 1)) FROM faculty_profiles;
SELECT setval(pg_get_serial_sequence('student_profiles', 'student_id'), coalesce(max(student_id), 1)) FROM student_profiles;
SELECT setval(pg_get_serial_sequence('attendance_sessions', 'session_id'), coalesce(max(session_id), 1)) FROM attendance_sessions;
SELECT setval(pg_get_serial_sequence('attendance_records', 'attendance_id'), coalesce(max(attendance_id), 1)) FROM attendance_records;

-- 4. Enable Supabase Realtime broadcast and live streaming on all application tables
ALTER PUBLICATION supabase_realtime ADD TABLE departments;
ALTER PUBLICATION supabase_realtime ADD TABLE users;
ALTER PUBLICATION supabase_realtime ADD TABLE faculty_profiles;
ALTER PUBLICATION supabase_realtime ADD TABLE student_profiles;
ALTER PUBLICATION supabase_realtime ADD TABLE classrooms;
ALTER PUBLICATION supabase_realtime ADD TABLE class_sections;
ALTER PUBLICATION supabase_realtime ADD TABLE subjects;
ALTER PUBLICATION supabase_realtime ADD TABLE faculty_subject_assignments;
ALTER PUBLICATION supabase_realtime ADD TABLE attendance_sessions;
ALTER PUBLICATION supabase_realtime ADD TABLE attendance_records;

ALTER TABLE departments REPLICA IDENTITY FULL;
ALTER TABLE users REPLICA IDENTITY FULL;
ALTER TABLE faculty_profiles REPLICA IDENTITY FULL;
ALTER TABLE student_profiles REPLICA IDENTITY FULL;
ALTER TABLE classrooms REPLICA IDENTITY FULL;
ALTER TABLE class_sections REPLICA IDENTITY FULL;
ALTER TABLE subjects REPLICA IDENTITY FULL;
ALTER TABLE faculty_subject_assignments REPLICA IDENTITY FULL;
ALTER TABLE attendance_sessions REPLICA IDENTITY FULL;
ALTER TABLE attendance_records REPLICA IDENTITY FULL;

-- 5. Automatically mirror public.users into Supabase Authentication (auth.users)
-- This makes all users visible in the Supabase Dashboard "Authentication -> Users" page!
CREATE OR REPLACE FUNCTION public.sync_user_to_supabase_auth()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM auth.users WHERE email = NEW.email) THEN
        INSERT INTO auth.users (
            instance_id,
            id,
            aud,
            role,
            email,
            encrypted_password,
            email_confirmed_at,
            raw_app_meta_data,
            raw_user_meta_data,
            created_at,
            updated_at,
            confirmation_token
        ) VALUES (
            '00000000-0000-0000-0000-000000000000',
            gen_random_uuid(),
            'authenticated',
            'authenticated',
            NEW.email,
            NEW.password_hash,
            now(),
            '{"provider":"email","providers":["email"]}'::jsonb,
            json_build_object('username', NEW.username, 'role', NEW.role)::jsonb,
            now(),
            now(),
            ''
        );
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trigger_sync_to_auth_users ON public.users;
CREATE TRIGGER trigger_sync_to_auth_users
AFTER INSERT ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.sync_user_to_supabase_auth();

-- Backfill all existing public.users immediately into auth.users so Authentication -> Users is populated
INSERT INTO auth.users (
    instance_id,
    id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    raw_app_meta_data,
    raw_user_meta_data,
    created_at,
    updated_at,
    confirmation_token
)
SELECT
    '00000000-0000-0000-0000-000000000000',
    gen_random_uuid(),
    'authenticated',
    'authenticated',
    u.email,
    u.password_hash,
    now(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    json_build_object('username', u.username, 'role', u.role)::jsonb,
    now(),
    now(),
    ''
FROM public.users u
WHERE NOT EXISTS (
    SELECT 1 FROM auth.users a WHERE a.email = u.email
);




