package com.attendance.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Custom Error Controller that replaces Spring Boot's Whitelabel Error Page.
 * Gracefully renders branded error views (400, 403, 404, 500) for browser clients
 * and structured JSON responses for API clients.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {}
        }

        String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (uri == null) {
            uri = request.getRequestURI();
        }

        String accept = request.getHeader("Accept");
        boolean isApi = (uri != null && uri.startsWith("/api/")) || (accept != null && accept.contains("application/json"));

        HttpStatus resolvedStatus = HttpStatus.resolve(statusCode);
        if (resolvedStatus == null) {
            resolvedStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (isApi) {
            Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", resolvedStatus.value(),
                "error", resolvedStatus.getReasonPhrase(),
                "message", "An unexpected error occurred",
                "path", uri != null ? uri : ""
            );
            return new ResponseEntity<>(body, resolvedStatus);
        }

        String viewName = switch (resolvedStatus.value()) {
            case 404 -> "error/404";
            case 403 -> "error/403";
            case 400 -> "error/400";
            default -> "error/500";
        };

        ModelAndView mav = new ModelAndView(viewName);
        mav.setStatus(resolvedStatus);
        return mav;
    }
}

