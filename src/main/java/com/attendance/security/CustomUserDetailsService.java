package com.attendance.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.attendance.entity.FacultyProfile;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.User;
import com.attendance.repository.FacultyRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;

/**
 * Custom UserDetailsService implementation that loads user details from the database.
 * Supports authentication by username, institutional email, student register number, or faculty employee ID.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    StudentRepository studentRepository,
                                    FacultyRepository facultyRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> studentRepository.findByRegisterNumber(identifier).map(StudentProfile::getUser))
                .or(() -> facultyRepository.findByEmployeeId(identifier).map(FacultyProfile::getUser))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with identifier: " + identifier));

        boolean isActive = user.getAccountStatus() == User.AccountStatus.ACTIVE;
        boolean isNotSuspended = user.getAccountStatus() != User.AccountStatus.SUSPENDED;

        return new CustomUserPrincipal(
                user.getUserId(),
                user.getUsername(),
                user.getPasswordHash(),
                isActive,            // enabled
                true,                // accountNonExpired
                true,                // credentialsNonExpired
                isNotSuspended,      // accountNonLocked
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }
}

