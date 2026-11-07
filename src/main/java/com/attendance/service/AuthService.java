package com.attendance.service;

import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.dto.LoginRequest;
import com.attendance.entity.FacultyProfile;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.User;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;
import com.attendance.security.CurrentUser;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       FacultyRepository facultyRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public Authentication authenticate(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    public User getCurrentUserEntity() {
        Long userId = CurrentUser.getCurrentUserId();
        if (userId == null) {
            throw new AttendanceException("AUTH_REQUIRED", "No authenticated user found");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }

    public Optional<StudentProfile> getCurrentStudentProfile() {
        Long userId = CurrentUser.getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return studentRepository.findByUserUserId(userId);
    }

    public Optional<FacultyProfile> getCurrentFacultyProfile() {
        Long userId = CurrentUser.getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return facultyRepository.findByUserUserId(userId);
    }

    public StudentProfile getStudentProfileByUserId(Long userId) {
        return studentRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", "userId", userId));
    }

    public FacultyProfile getFacultyProfileByUserId(Long userId) {
        return facultyRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FacultyProfile", "userId", userId));
    }
}

