package com.attendance.config;

import java.io.IOException;
import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationSuccessHandler successHandler) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
            // CSRF configuration
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/api/**")
            )

            // Header security
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss.headerValue(
                        org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentTypeOptions(contentType -> {})
            )

            // URL authorization
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/faculty/**").hasRole("FACULTY")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/faculty/**").hasRole("FACULTY")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )

            // Form login
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Session management
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }

    /**
     * Custom authentication success handler that redirects users to role-specific dashboards.
     */
    @Component
    public static class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {

            String redirectUrl = determineTargetUrl(authentication);
            response.sendRedirect(request.getContextPath() + redirectUrl);
        }

        private String determineTargetUrl(Authentication authentication) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                switch (role) {
                    case "ROLE_ADMIN":
                        return "/admin/dashboard";
                    case "ROLE_FACULTY":
                        return "/faculty/dashboard";
                    case "ROLE_STUDENT":
                        return "/student/dashboard";
                    default:
                        break;
                }
            }

            return "/login?error=true";
        }
    }
}
