package com.attendance.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.attendance.dto.LoginRequest;
import com.attendance.entity.User;
import com.attendance.security.CurrentUser;
import com.attendance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/")
    public String rootRedirect() {
        String role = CurrentUser.getCurrentRole();
        if (role == null) {
            return "redirect:/login";
        }
        return switch (role) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "FACULTY" -> "redirect:/faculty/dashboard";
            case "STUDENT" -> "redirect:/student/dashboard";
            default -> "redirect:/login";
        };
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiLogin(@Valid @RequestBody LoginRequest loginRequest,
                                                        HttpServletRequest request) {
        Authentication auth = authService.authenticate(loginRequest);
        org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);
        request.getSession(true).setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        String role = CurrentUser.getCurrentRole();
        Long userId = CurrentUser.getCurrentUserId();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("username", auth.getName());
        response.put("role", role);
        response.put("userId", userId);

        String redirectUrl = switch (role != null ? role : "") {
            case "ADMIN" -> "/admin/dashboard";
            case "FACULTY" -> "/faculty/dashboard";
            case "STUDENT" -> "/student/dashboard";
            default -> "/login";
        };
        response.put("redirectUrl", redirectUrl);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/api/auth/me")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo() {
        Long userId = CurrentUser.getCurrentUserId();
        String username = CurrentUser.getCurrentUsername();
        String role = CurrentUser.getCurrentRole();

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("userId", userId);
        response.put("username", username);
        response.put("role", role);

        return ResponseEntity.ok(response);
    }
}

