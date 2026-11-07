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

-- 3. Synchronize auto-increment sequences safely
DO $$
BEGIN
    IF pg_get_serial_sequence('departments', 'department_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('departments', 'department_id'), coalesce(max(department_id), 1)) FROM departments;
    END IF;
    IF pg_get_serial_sequence('classrooms', 'classroom_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('classrooms', 'classroom_id'), coalesce(max(classroom_id), 1)) FROM classrooms;
    END IF;
    IF pg_get_serial_sequence('users', 'user_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('users', 'user_id'), coalesce(max(user_id), 1)) FROM users;
    END IF;
    IF pg_get_serial_sequence('class_sections', 'section_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('class_sections', 'section_id'), coalesce(max(section_id), 1)) FROM class_sections;
    END IF;
    IF pg_get_serial_sequence('subjects', 'subject_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('subjects', 'subject_id'), coalesce(max(subject_id), 1)) FROM subjects;
    END IF;
    IF pg_get_serial_sequence('faculty_profiles', 'faculty_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('faculty_profiles', 'faculty_id'), coalesce(max(faculty_id), 1)) FROM faculty_profiles;
    END IF;
    IF pg_get_serial_sequence('student_profiles', 'student_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('student_profiles', 'student_id'), coalesce(max(student_id), 1)) FROM student_profiles;
    END IF;
    IF pg_get_serial_sequence('attendance_sessions', 'session_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('attendance_sessions', 'session_id'), coalesce(max(session_id), 1)) FROM attendance_sessions;
    END IF;
    IF pg_get_serial_sequence('attendance_records', 'attendance_id') IS NOT NULL THEN
        PERFORM setval(pg_get_serial_sequence('attendance_records', 'attendance_id'), coalesce(max(attendance_id), 1)) FROM attendance_records;
    END IF;
END $$;

-- 4. Enable Supabase Realtime broadcast safely (skips if already added)
DO $$
DECLARE
    tbl text;
BEGIN
    FOR tbl IN SELECT unnest(ARRAY[
        'departments', 'users', 'faculty_profiles', 'student_profiles',
        'classrooms', 'class_sections', 'subjects', 'faculty_subject_assignments',
        'attendance_sessions', 'attendance_records'
    ])
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables 
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = tbl
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE %I', tbl);
        END IF;
    END LOOP;
END $$;

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

-- 5. Automatically mirror public.users into Supabase Authentication (auth.users and auth.identities)
-- This makes all users visible in the Supabase Dashboard "Authentication -> Users" page
-- and prevents "Database error loading user" by ensuring matching identity rows exist.
CREATE OR REPLACE FUNCTION public.sync_user_to_supabase_auth()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    new_auth_id uuid := gen_random_uuid();
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
            new_auth_id,
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

        INSERT INTO auth.identities (
            id,
            user_id,
            identity_data,
            provider,
            provider_id,
            last_sign_in_at,
            created_at,
            updated_at
        ) VALUES (
            new_auth_id::text,
            new_auth_id,
            jsonb_build_object('sub', new_auth_id::text, 'email', NEW.email),
            'email',
            new_auth_id::text,
            now(),
            now(),
            now()
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

-- Synchronize deletions from public.users to auth.users automatically
CREATE OR REPLACE FUNCTION public.sync_user_delete_to_auth()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    DELETE FROM auth.identities WHERE user_id IN (SELECT id FROM auth.users WHERE email = OLD.email);
    DELETE FROM auth.users WHERE email = OLD.email;
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trigger_sync_user_delete_to_auth ON public.users;
CREATE TRIGGER trigger_sync_user_delete_to_auth
AFTER DELETE ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.sync_user_delete_to_auth();

-- 6. Backfill missing auth.identities for existing auth.users
-- This immediately resolves the Supabase Dashboard "Database error loading user" error!
INSERT INTO auth.identities (
    id,
    user_id,
    identity_data,
    provider,
    provider_id,
    last_sign_in_at,
    created_at,
    updated_at
)
SELECT 
    u.id::text,
    u.id,
    jsonb_build_object('sub', u.id::text, 'email', u.email),
    'email',
    u.id::text,
    now(),
    now(),
    now()
FROM auth.users u
WHERE NOT EXISTS (
    SELECT 1 FROM auth.identities i WHERE i.user_id = u.id
);

-- Backfill any existing public.users into auth.users + auth.identities
DO $$
DECLARE
    usr RECORD;
    new_id uuid;
BEGIN
    FOR usr IN SELECT * FROM public.users WHERE email NOT IN (SELECT email FROM auth.users WHERE email IS NOT NULL)
    LOOP
        new_id := gen_random_uuid();
        INSERT INTO auth.users (
            instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
            raw_app_meta_data, raw_user_meta_data, created_at, updated_at, confirmation_token
        ) VALUES (
            '00000000-0000-0000-0000-000000000000',
            new_id,
            'authenticated',
            'authenticated',
            usr.email,
            usr.password_hash,
            now(),
            '{"provider":"email","providers":["email"]}'::jsonb,
            json_build_object('username', usr.username, 'role', usr.role)::jsonb,
            now(),
            now(),
            ''
        );

        INSERT INTO auth.identities (
            id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
        ) VALUES (
            new_id::text,
            new_id,
            jsonb_build_object('sub', new_id::text, 'email', usr.email),
            'email',
            new_id::text,
            now(),
            now(),
            now()
        );
    END LOOP;
END $$;




