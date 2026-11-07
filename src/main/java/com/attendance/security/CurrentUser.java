package com.attendance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to retrieve the current authenticated user's details
 * from the Spring Security context.
 */
public final class CurrentUser {

    private CurrentUser() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns the user ID of the currently authenticated user.
     *
     * @return the user ID, or null if not authenticated or principal is not a CustomUserPrincipal
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    /**
     * Returns the username of the currently authenticated user.
     *
     * @return the username, or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getUsername();
        }
        return null;
    }

    /**
     * Returns the role of the currently authenticated user (without the "ROLE_" prefix).
     *
     * @return the role string (e.g., "ADMIN", "FACULTY", "STUDENT"), or null if not authenticated
     */
    public static String getCurrentRole() {
        Authentication authentication = getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
