package com.attendance.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.entity.ClassSection;
import com.attendance.entity.Classroom;
import com.attendance.entity.Department;
import com.attendance.entity.Enrollment;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.FacultySubjectAssignment;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.repository.ClassSectionRepository;
import com.attendance.repository.ClassroomRepository;
import com.attendance.repository.DepartmentRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.FacultySubjectAssignmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.UserRepository;

@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               FacultyRepository facultyRepository,
                               StudentRepository studentRepository,
                               ClassroomRepository classroomRepository,
                               ClassSectionRepository classSectionRepository,
                               SubjectRepository subjectRepository,
                               EnrollmentRepository enrollmentRepository,
                               FacultySubjectAssignmentRepository assignmentRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.classSectionRepository = classSectionRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaultData() {
        try {
            if (userRepository.count() > 0) {
                log.info("[DatabaseInitializer] Database already contains {} users, skipping auto-seeding.", userRepository.count());
                return;
            }

            log.info("[DatabaseInitializer] Empty database detected. Seeding initial institutional data...");

            // 1. Departments
            Department cse = departmentRepository.save(new Department("CSE", "Computer Science & Engineering"));
            Department it = departmentRepository.save(new Department("IT", "Information Technology"));
            Department ece = departmentRepository.save(new Department("ECE", "Electronics & Communication Engineering"));
            Department mech = departmentRepository.save(new Department("MECH", "Mechanical Engineering"));

            // 2. Classrooms
            Classroom room401 = classroomRepository.save(new Classroom(
                    "Science Block Room 401", "Science Block", 4,
                    BigDecimal.valueOf(12.8342000), BigDecimal.valueOf(79.7036000), 50
            ));
            Classroom room402 = classroomRepository.save(new Classroom(
                    "Science Block Room 402", "Science Block", 4,
                    BigDecimal.valueOf(12.8342500), BigDecimal.valueOf(79.7036500), 50
            ));
            Classroom iotLab = classroomRepository.save(new Classroom(
                    "IoT Innovation Lab", "Tech Tower", 2,
                    BigDecimal.valueOf(12.8345000), BigDecimal.valueOf(79.7040000), 60
            ));

            // 3. Class Sections
            ClassSection secA = classSectionRepository.save(new ClassSection(cse, "2026-2027", 3, "A"));
            ClassSection secB = classSectionRepository.save(new ClassSection(cse, "2026-2027", 3, "B"));

            // 4. Undergraduate Subjects
            Subject sub1 = subjectRepository.save(new Subject("CS501", "Database Management Systems", cse, 4));
            Subject sub2 = subjectRepository.save(new Subject("CS502", "Operating Systems & Virtualization", cse, 4));
            Subject sub3 = subjectRepository.save(new Subject("CS503", "Design and Analysis of Algorithms", cse, 4));
            Subject sub4 = subjectRepository.save(new Subject("CS504", "Computer Networks & Security", cse, 3));
            Subject sub5 = subjectRepository.save(new Subject("CS505", "Artificial Intelligence & Expert Systems", cse, 3));

            String defaultHash = passwordEncoder.encode("admin123");

            // 5. Admin User
            User admin = new User("admin", "admin@scsvmv.ac.in", defaultHash, User.Role.ADMIN);
            admin.setAccountStatus(User.AccountStatus.ACTIVE);
            userRepository.save(admin);

            // 6. Faculty User & Profile
            User facUser1 = new User("prof-ramesh", "ramesh@kanchiuniv.ac.in", defaultHash, User.Role.FACULTY);
            facUser1.setAccountStatus(User.AccountStatus.ACTIVE);
            facUser1 = userRepository.save(facUser1);
            FacultyProfile faculty1 = facultyRepository.save(new FacultyProfile(facUser1, "FAC-101", "Dr. K. Ramesh", cse));

            User facUser2 = new User("karthik123", "karthik@kanchiuniv.ac.in", defaultHash, User.Role.FACULTY);
            facUser2.setAccountStatus(User.AccountStatus.ACTIVE);
            facUser2 = userRepository.save(facUser2);
            FacultyProfile faculty2 = facultyRepository.save(new FacultyProfile(facUser2, "SCSVMV003", "Prof. Karthik", it));

            // Assign subjects to faculty
            assignmentRepository.save(new FacultySubjectAssignment(faculty1, sub1, secA, "2026-2027"));
            assignmentRepository.save(new FacultySubjectAssignment(faculty1, sub2, secA, "2026-2027"));
            assignmentRepository.save(new FacultySubjectAssignment(faculty2, sub3, secB, "2026-2027"));

            // 7. Student Users & Profiles
            User studUser1 = new User("student-001", "student@kanchiuniv.ac.in", defaultHash, User.Role.STUDENT);
            studUser1.setAccountStatus(User.AccountStatus.ACTIVE);
            studUser1 = userRepository.save(studUser1);
            StudentProfile student1 = studentRepository.save(new StudentProfile(studUser1, "11249A001", "Test Student", cse, 3, "A"));

            User studUser2 = new User("sasank-7799", "11249m010@kanchiuniv.ac.in", defaultHash, User.Role.STUDENT);
            studUser2.setAccountStatus(User.AccountStatus.ACTIVE);
            studUser2 = userRepository.save(studUser2);
            StudentProfile student2 = studentRepository.save(new StudentProfile(studUser2, "11249M010", "Sasank", cse, 3, "A"));

            // Enroll students into sections
            enrollmentRepository.save(new Enrollment(student1, secA));
            enrollmentRepository.save(new Enrollment(student2, secA));

            log.info("[DatabaseInitializer] Successfully seeded 4 departments, 3 classrooms, 5 subjects, admin, 2 faculty, and 2 students.");
        } catch (Exception ex) {
            log.error("[DatabaseInitializer] Auto-seeding encountered an error: {}", ex.getMessage(), ex);
        }
    }
}
