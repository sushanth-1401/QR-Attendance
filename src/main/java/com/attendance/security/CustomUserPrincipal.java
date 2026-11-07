package com.attendance.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Custom principal that extends Spring Security's User to include the application-specific user ID.
 */
public class CustomUserPrincipal extends User {

    private final Long userId;

    public CustomUserPrincipal(Long userId, String username, String password,
                               boolean enabled, boolean accountNonExpired,
                               boolean credentialsNonExpired, boolean accountNonLocked,
                               Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
    }

    public CustomUserPrincipal(Long userId, String username, String password,
                               Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
