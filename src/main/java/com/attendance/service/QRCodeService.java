package com.attendance.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.config.AppConfig;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.QRToken;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.InvalidQRException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.QRTokenRepository;
import com.attendance.util.QRTokenGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Service
@Transactional
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);

    private final QRTokenRepository qrTokenRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final QRTokenGenerator qrTokenGenerator;
    private final AppConfig appConfig;

    public QRCodeService(QRTokenRepository qrTokenRepository,
                         AttendanceSessionRepository sessionRepository,
                         QRTokenGenerator qrTokenGenerator,
                         AppConfig appConfig) {
        this.qrTokenRepository = qrTokenRepository;
        this.sessionRepository = sessionRepository;
        this.qrTokenGenerator = qrTokenGenerator;
        this.appConfig = appConfig;
    }

    /**
     * Data holder containing the raw token (to put in QR code) and QRToken entity.
     */
    public record GeneratedQRData(String rawToken, String tokenHash, LocalDateTime expiresAt, long expiresInSeconds, String base64Image) {}

    /**
     * Generates a new dynamic QR token for an active attendance session.
     * Automatically expires previous active tokens for the session.
     */
    public GeneratedQRData generateTokenForSession(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "sessionId", sessionId));

        if (session.getSessionStatus() != AttendanceSession.SessionStatus.ACTIVE) {
            throw new AttendanceException("SESSION_NOT_ACTIVE", "Cannot generate QR code for an inactive or closed session");
        }

        // Expire any existing active tokens for this session
        qrTokenRepository.expireTokensBySessionId(sessionId);

        // Generate 256-bit cryptographically secure token
        String rawToken = qrTokenGenerator.generateToken();
        String tokenHash = qrTokenGenerator.hashToken(rawToken);

        long validitySeconds = appConfig.getQr().getTokenValiditySeconds();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(validitySeconds);

        QRToken qrToken = new QRToken(session, tokenHash, now, expiresAt);
        qrTokenRepository.save(qrToken);

        log.debug("Generated QR token for session id={}, expires in {}s", sessionId, validitySeconds);

        // Generate QR image
        byte[] qrImageBytes = generateQRImageBytes(rawToken, 350, 350);
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrImageBytes);

        return new GeneratedQRData(rawToken, tokenHash, expiresAt, validitySeconds, base64Image);
    }

    /**
     * Renders a QR code to PNG image byte array.
     */
    public byte[] generateQRImageBytes(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new AttendanceException("QR_GENERATION_FAILED", "Failed to render QR code image: " + e.getMessage());
        }
    }

    /**
     * Validates a raw token scanned by student.
     * Must be ACTIVE and server timestamp must be before expires_at.
     */
    @Transactional(readOnly = true)
    public QRToken validateScannedToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new InvalidQRException("QR token cannot be empty");
        }

        String tokenHash = qrTokenGenerator.hashToken(rawToken.trim());
        QRToken qrToken = qrTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidQRException("Invalid QR token: Token not recognized"));

        if (qrToken.getTokenStatus() != QRToken.TokenStatus.ACTIVE) {
            throw new InvalidQRException("This QR token is no longer active (status: " + qrToken.getTokenStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(qrToken.getExpiresAt())) {
            throw new InvalidQRException("QR code expired. Please scan the newly displayed QR code on the screen.");
        }

        // Verify associated session is active
        if (qrToken.getSession().getSessionStatus() != AttendanceSession.SessionStatus.ACTIVE) {
            throw new InvalidQRException("The attendance session is not currently active.");
        }

        return qrToken;
    }

    /**
     * Marks token as expired.
     */
    public void expireToken(Long qrId) {
        qrTokenRepository.findById(qrId).ifPresent(token -> {
            token.setTokenStatus(QRToken.TokenStatus.EXPIRED);
            qrTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public Optional<QRToken> getActiveToken(Long sessionId) {
        return qrTokenRepository.findActiveBySessionId(sessionId);
    }
}

