package com.attendance.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.attendance.service.QRCodeService;

@RestController
public class QRController {

    private final QRCodeService qrCodeService;

    public QRController(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/api/qr/generate/{sessionId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<byte[]> getQRImage(@PathVariable("sessionId") Long sessionId) {
        QRCodeService.GeneratedQRData qrData = qrCodeService.generateTokenForSession(sessionId);
        byte[] imageBytes = qrCodeService.generateQRImageBytes(qrData.rawToken(), 400, 400);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .body(imageBytes);
    }
}

