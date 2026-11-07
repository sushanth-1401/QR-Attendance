# Database Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    users ||--o| student_profiles : "1-to-1 Profile"
    users ||--o| faculty_profiles : "1-to-1 Profile"
    users ||--o{ audit_logs : "creates"

    departments ||--o{ student_profiles : "contains"
    departments ||--o{ faculty_profiles : "employs"
    departments ||--o{ class_sections : "organizes"
    departments ||--o{ subjects : "offers"

    class_sections ||--o{ enrollments : "has enrolled"
    student_profiles ||--o{ enrollments : "enrolled in"

    faculty_profiles ||--o{ faculty_subject_assignments : "assigned to"
    subjects ||--o{ faculty_subject_assignments : "assigned"
    class_sections ||--o{ faculty_subject_assignments : "teaches section"

    classrooms ||--o{ attendance_sessions : "hosts in geofence"
    faculty_profiles ||--o{ attendance_sessions : "conducts"
    subjects ||--o{ attendance_sessions : "subject of"
    class_sections ||--o{ attendance_sessions : "attends"

    attendance_sessions ||--o{ qr_tokens : "generates dynamic"
    student_profiles ||--o{ registered_devices : "registers"
    registered_devices ||--o{ device_challenges : "receives challenge"

    attendance_sessions ||--o{ attendance_attempts : "attempted for"
    student_profiles ||--o{ attendance_attempts : "scans QR"
    qr_tokens ||--o{ attendance_attempts : "uses token"
    registered_devices ||--o{ attendance_attempts : "authenticates"

    attendance_attempts ||--o{ verification_logs : "records steps"
    attendance_attempts ||--o| attendance_records : "confirms"
    student_profiles ||--o{ attendance_records : "marked for"
    attendance_sessions ||--o{ attendance_records : "session records"
```

### Table Relationships & Key Constraints:
1. **users**: Primary authentication credentials (`user_id`, `username`, `email`, `role`, `password_hash`).
2. **student_profiles** & **faculty_profiles**: Extended profile entities linked via foreign key to `users(user_id)`.
3. **classrooms**: Stores high-precision GPS decimal coordinates (`latitude`, `longitude`) and configurable `allowed_radius_meters`.
4. **qr_tokens**: Holds SHA-256 hashes of temporary 256-bit entropy tokens (`token_hash`, `issued_at`, `expires_at`, `token_status`).
5. **attendance_attempts**: Tracks two-step reservations (`request_id`, `scan_time`, `reservation_expires_at`, `verification_status`).
6. **attendance_records**: Contains verified records guarded by `UNIQUE(student_id, session_id)` ensuring no duplicate attendance is ever recorded.
7. **verification_logs**: Audit logs of each individual check (QR validity, enrollment, GPS geofence distance, ECDSA device challenge signature).

