package com.attendance.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.attendance.entity.User;
import com.attendance.repository.UserRepository;

/**
 * Custom UserDetailsService implementation that loads user details from the database.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));

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
