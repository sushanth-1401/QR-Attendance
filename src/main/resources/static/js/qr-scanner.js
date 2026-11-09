/**
 * QR Attendance Management System - Scanner & Verification Orchestrator
 */

let html5QrScanner = null;
let isProcessing = false;

document.addEventListener('DOMContentLoaded', () => {
    initScanner();
});

function updateStepStatus(stepNumber, state, text) {
    const stepItem = document.getElementById(`step-${stepNumber}`);
    if (!stepItem) return;

    stepItem.classList.remove('active', 'completed', 'failed');
    if (state === 'active') stepItem.classList.add('active');
    if (state === 'completed') stepItem.classList.add('completed');
    if (state === 'failed') stepItem.classList.add('failed');

    const descEl = document.getElementById(`step-${stepNumber}-desc`);
    if (descEl && text) descEl.textContent = text;
}

function showResult(success, title, message) {
    const box = document.getElementById('result-box');
    const titleEl = document.getElementById('result-title');
    const msgEl = document.getElementById('result-message');
    const iconEl = document.getElementById('result-icon');

    if (!box) return;

    box.style.display = 'block';
    box.className = success ? 'alert alert-success mt-4 p-4 text-center' : 'alert alert-danger mt-4 p-4 text-center';
    titleEl.textContent = title;
    msgEl.textContent = message;

    if (iconEl) {
        iconEl.className = success ? 'fas fa-circle-check fa-2x text-success mb-2' : 'fas fa-circle-xmark fa-2x text-danger mb-2';
    }
}

function initScanner() {
    const qrRegion = document.getElementById('qr-reader');
    if (!qrRegion) return;

    // Use Html5Qrcode if library is loaded
    if (typeof Html5Qrcode === 'undefined') {
        console.warn('Html5Qrcode library not loaded yet.');
        return;
    }

    html5QrScanner = new Html5Qrcode("qr-reader");

    const qrConfig = {
        fps: 10,
        qrbox: { width: 250, height: 250 }
    };

    html5QrScanner.start(
        { facingMode: "environment" },
        qrConfig,
        onQrCodeSuccess,
        (errorMessage) => {
            // continuous scan frame errors ignored
        }
    ).catch(err => {
        console.error("Unable to start camera scanning:", err);
        showResult(false, "Camera Error", "Could not access mobile camera. Please check camera permissions.");
    });
}

async function onQrCodeSuccess(decodedText, decodedResult) {
    if (isProcessing) return;
    isProcessing = true;

    try {
        if (html5QrScanner) {
            await html5QrScanner.pause();
        }

        console.log("Scanned QR token:", decodedText);
        updateStepStatus(1, 'completed', 'QR Code Scanned');

        // Step 1: Submit scanned token to backend reservation endpoint
        updateStepStatus(2, 'active', 'Validating Token & Reserving...');
        const requestId = generateUUID();

        const scanResponse = await fetchWithCsrf('/api/student/attendance/scan', {
            method: 'POST',
            body: JSON.stringify({
                token: decodedText.trim(),
                requestId: requestId
            })
        });

        const attemptId = scanResponse.attemptId;
        console.log("Scan reservation confirmed! Attempt ID:", attemptId);
        updateStepStatus(2, 'completed', 'Reservation Confirmed (2 min)');

        // Step 2: Acquire GPS Geolocation
        updateStepStatus(3, 'active', 'Acquiring GPS location...');
        let locationData = null;
        try {
            locationData = await GPSLocationManager.getCurrentLocation();
            updateStepStatus(3, 'completed', `GPS Verified (±${Math.round(locationData.accuracy)}m)`);
        } catch (gpsErr) {
            console.error("GPS error:", gpsErr);
            throw new Error(gpsErr.message || "Failed to obtain GPS location");
        }

        // Step 3: Submit Final Verification
        const verifyResponse = await fetchWithCsrf(`/api/student/attendance/${attemptId}/verify`, {
            method: 'POST',
            body: JSON.stringify({
                location: locationData,
                deviceId: null,
                signature: null,
                challengeId: null
            })
        });

        showResult(true, "Attendance Confirmed!", verifyResponse.message || "Your attendance has been successfully recorded as PRESENT.");
        showToast("Attendance Marked Successfully!", "success");

        // Supabase Realtime Broadcast & Direct Database Persistence
        try {
            const scanContent = document.getElementById('scan-content');
            const studentName = scanContent ? scanContent.getAttribute('data-student-name') : 'Student';
            const registerNumber = scanContent ? scanContent.getAttribute('data-register-number') : '';
            const sessionId = (scanResponse && scanResponse.details) ? scanResponse.details.sessionId : null;
            const studentId = (verifyResponse && verifyResponse.details) ? verifyResponse.details.studentId : 1;

            if (typeof SupabaseRealtime !== 'undefined' && sessionId) {
                // 1. Broadcast live event to faculty screen
                SupabaseRealtime.broadcastStudentVerified(sessionId, {
                    studentName: studentName,
                    registerNumber: registerNumber,
                    markedAt: new Date().toISOString()
                });

                // 2. Direct table insert into Supabase attendance_records
                SupabaseRealtime.saveAttendanceRecord({
                    student_id: studentId,
                    session_id: sessionId,
                    attendance_status: 'PRESENT',
                    marked_at: new Date().toISOString(),
                    latitude: locationData ? locationData.latitude : null,
                    longitude: locationData ? locationData.longitude : null,
                    gps_accuracy_meters: locationData ? locationData.accuracy : null,
                    distance_from_classroom: (verifyResponse && verifyResponse.details) ? verifyResponse.details.distanceMeters : null,
                    verification_method: 'QR + GPS',
                    created_at: new Date().toISOString()
                });
            }
        } catch (rtErr) {
            console.warn('[Supabase Realtime & DB Save] Non-fatal notification error:', rtErr);
        }

    } catch (error) {
        console.error("Attendance verification flow failed:", error);
        
        if (error.message && (error.message.includes('GPS') || error.message.includes('Geofence') || error.message.includes('Proxy') || error.message.includes('Location'))) {
            updateStepStatus(3, 'failed', 'Geofence Breach Detected');
        }

        showResult(false, "Verification Failed", error.message || "An unexpected error occurred during attendance verification.");
        showToast(error.message, "danger");

        // Allow retry after 3 seconds
        setTimeout(() => {
            if (html5QrScanner) html5QrScanner.resume();
            isProcessing = false;
        }, 3000);
    }
}

