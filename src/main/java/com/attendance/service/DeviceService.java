package com.attendance.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.config.AppConfig;
import com.attendance.dto.DeviceChallengeResponse;
import com.attendance.dto.DeviceRegistrationRequest;
import com.attendance.entity.DeviceChallenge;
import com.attendance.entity.RegisteredDevice;
import com.attendance.entity.StudentProfile;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.DeviceChallengeRepository;
import com.attendance.repository.DeviceRepository;
import com.attendance.util.HashUtil;

@Service
@Transactional
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final DeviceChallengeRepository challengeRepository;
    private final AppConfig appConfig;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceChallengeRepository challengeRepository,
                         AppConfig appConfig) {
        this.deviceRepository = deviceRepository;
        this.challengeRepository = challengeRepository;
        this.appConfig = appConfig;
    }

    /**
     * Registers a new cryptographic device for a student.
     */
    public RegisteredDevice registerDevice(StudentProfile student, DeviceRegistrationRequest request) {
        // Enforce maximum devices policy
        long activeCount = deviceRepository.countByStudentStudentIdAndDeviceStatus(
                student.getStudentId(), RegisteredDevice.DeviceStatus.ACTIVE);

        int maxDevices = appConfig.getDevice().getMaxDevicesPerStudent();
        if (activeCount >= maxDevices) {
            throw new AttendanceException("MAX_DEVICES_EXCEEDED",
                    String.format("Maximum registered devices limit (%d) reached. Please revoke an existing device first.", maxDevices));
        }

        // Validate public key format (test parsing)
        validatePublicKeyFormat(request.getPublicKey());

        RegisteredDevice device = new RegisteredDevice(
                student,
                request.getDeviceName() != null ? request.getDeviceName() : "Student Device",
                request.getPublicKey(),
                request.getKeyAlgorithm() != null ? request.getKeyAlgorithm() : "EC"
        );

        RegisteredDevice saved = deviceRepository.save(device);
        log.info("Registered new device id={} for student registerNumber={}", saved.getDeviceId(), student.getRegisterNumber());
        return saved;
    }

    /**
     * Generates a cryptographically random single-use challenge for device verification.
     */
    public DeviceChallengeResponse generateChallenge(Long deviceId, Long studentId) {
        RegisteredDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("RegisteredDevice", "deviceId", deviceId));

        if (!device.getStudent().getStudentId().equals(studentId)) {
            throw new AttendanceException("UNAUTHORIZED_DEVICE", "Device does not belong to the authenticated student");
        }

        if (device.getDeviceStatus() != RegisteredDevice.DeviceStatus.ACTIVE) {
            throw new AttendanceException("DEVICE_REVOKED", "The requested device has been revoked and cannot be verified");
        }

        // Generate 256 bits of cryptographically secure random challenge
        String challengeValue = HashUtil.generateSecureRandom(256);
        String challengeHash = HashUtil.sha256(challengeValue);

        long validitySeconds = appConfig.getDevice().getChallengeValiditySeconds();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(validitySeconds);

        DeviceChallenge challenge = new DeviceChallenge(device, challengeValue, challengeHash, expiresAt);
        DeviceChallenge saved = challengeRepository.save(challenge);

        log.debug("Issued device challenge id={} for deviceId={}", saved.getChallengeId(), deviceId);

        return new DeviceChallengeResponse(
                saved.getChallengeId(),
                challengeValue,
                deviceId,
                validitySeconds
        );
    }

    /**
     * Verifies the cryptographic signature against the registered public key and ensures single-use.
     */
    public boolean verifyChallenge(Long challengeId, Long deviceId, String signatureBase64) {
        DeviceChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("DeviceChallenge", "challengeId", challengeId));

        // 1. Device match check
        if (!challenge.getDevice().getDeviceId().equals(deviceId)) {
            log.warn("Challenge id={} deviceId mismatch with provided deviceId={}", challengeId, deviceId);
            return false;
        }

        // 2. Replay check: Must not already be consumed
        if (Boolean.TRUE.equals(challenge.getConsumed())) {
            log.warn("Challenge id={} has already been consumed (replay attempt detected)", challengeId);
            return false;
        }

        // 3. Expiry check using server clock
        if (LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            log.warn("Challenge id={} has expired at {}", challengeId, challenge.getExpiresAt());
            return false;
        }

        // Mark as consumed immediately to prevent race-condition replays
        challenge.setConsumed(true);
        challenge.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        RegisteredDevice device = challenge.getDevice();
        if (device.getDeviceStatus() != RegisteredDevice.DeviceStatus.ACTIVE) {
            log.warn("Device id={} is no longer active", deviceId);
            return false;
        }

        // 4. Verify cryptographic signature
        try {
            PublicKey publicKey = parsePublicKey(device.getPublicKey());
            byte[] challengeBytes = challenge.getChallengeValue().getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            boolean verified = verifyEcdsaSignature(publicKey, challengeBytes, signatureBytes);
            log.info("Cryptographic signature verification for device id={}: {}", deviceId, verified ? "PASSED" : "FAILED");
            return verified;
        } catch (Exception e) {
            log.error("Cryptographic signature verification failed for device id={}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    /**
     * Verifies ECDSA signature supporting both IEEE P1363 (Web Crypto default) and standard ASN.1 DER.
     */
    private boolean verifyEcdsaSignature(PublicKey publicKey, byte[] data, byte[] signatureBytes) {
        // Try P1363 format first (standard Web Crypto format: 64 bytes for P-256)
        if (signatureBytes.length == 64) {
            try {
                Signature sigP1363 = Signature.getInstance("SHA256withECDSAinP1363Format");
                sigP1363.initVerify(publicKey);
                sigP1363.update(data);
                if (sigP1363.verify(signatureBytes)) {
                    return true;
                }
            } catch (Exception ignored) {
                // Fallback to manual DER conversion
            }

            try {
                byte[] derSig = convertP1363ToDer(signatureBytes);
                Signature sigDer = Signature.getInstance("SHA256withECDSA");
                sigDer.initVerify(publicKey);
                sigDer.update(data);
                return sigDer.verify(derSig);
            } catch (Exception e) {
                log.debug("DER fallback verification failed: {}", e.getMessage());
            }
        }

        // Try standard ASN.1 DER
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Standard signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Converts a 64-byte raw IEEE P1363 signature (r || s) to ASN.1 DER format.
     */
    private byte[] convertP1363ToDer(byte[] p1363) {
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(p1363, 0, r, 0, 32);
        System.arraycopy(p1363, 32, s, 0, 32);

        BigInteger rBig = new BigInteger(1, r);
        BigInteger sBig = new BigInteger(1, s);

        byte[] rDer = rBig.toByteArray();
        byte[] sDer = sBig.toByteArray();

        int len = 2 + rDer.length + 2 + sDer.length;
        byte[] der = new byte[2 + len];
        der[0] = 0x30; // SEQUENCE
        der[1] = (byte) len;
        der[2] = 0x02; // INTEGER
        der[3] = (byte) rDer.length;
        System.arraycopy(rDer, 0, der, 4, rDer.length);
        int sOffset = 4 + rDer.length;
        der[sOffset] = 0x02; // INTEGER
        der[sOffset + 1] = (byte) sDer.length;
        System.arraycopy(sDer, 0, der, sOffset + 2, sDer.length);

        return der;
    }

    public PublicKey parsePublicKey(String base64Spki) {
        try {
            // Clean up any PEM headers if present
            String cleanKey = base64Spki
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new AttendanceException("INVALID_KEY_FORMAT", "Failed to parse public key: " + e.getMessage());
        }
    }

    private void validatePublicKeyFormat(String base64Spki) {
        parsePublicKey(base64Spki);
    }

    @Transactional(readOnly = true)
    public List<RegisteredDevice> getStudentDevices(Long studentId) {
        return deviceRepository.findByStudentStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<RegisteredDevice> getStudentActiveDevices(Long studentId) {
        return deviceRepository.findByStudentStudentIdAndDeviceStatus(studentId, RegisteredDevice.DeviceStatus.ACTIVE);
    }

    public void revokeDevice(Long deviceId, Long studentId, String reason) {
        RegisteredDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("RegisteredDevice", "deviceId", deviceId));

        if (studentId != null && !device.getStudent().getStudentId().equals(studentId)) {
            throw new AttendanceException("UNAUTHORIZED_DEVICE", "Cannot revoke a device belonging to another student");
        }

        device.setDeviceStatus(RegisteredDevice.DeviceStatus.REVOKED);
        device.setRevokedAt(LocalDateTime.now());
        device.setRevocationReason(reason != null ? reason : "Revoked by user");
        deviceRepository.save(device);
        log.info("Device id={} revoked. Reason: {}", deviceId, reason);
    }
}

