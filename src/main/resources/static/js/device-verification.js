/**
 * QR Attendance Management System - Web Crypto Device Verification Module
 * Generates ECDSA P-256 keypairs, stores private key in IndexedDB, signs challenges.
 */

const DeviceAuthManager = {
    DB_NAME: 'QRAttendanceCryptoDB',
    DB_VERSION: 1,
    STORE_NAME: 'device_keys',
    KEY_ID: 'active_student_key',

    // IndexedDB Helper
    openDB: function() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains(this.STORE_NAME)) {
                    db.createObjectStore(this.STORE_NAME, { keyPath: 'id' });
                }
            };
            request.onsuccess = (event) => resolve(event.target.result);
            request.onerror = (event) => reject(event.target.error);
        });
    },

    // Save keypair to IndexedDB
    storeKeyRecord: async function(keyRecord) {
        const db = await this.openDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORE_NAME, 'readwrite');
            const store = tx.objectStore(this.STORE_NAME);
            const req = store.put({ id: this.KEY_ID, ...keyRecord });
            req.onsuccess = () => resolve();
            req.onerror = () => reject(req.error);
        });
    },

    // Retrieve active key record from IndexedDB
    loadKeyRecord: async function() {
        const db = await this.openDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORE_NAME, 'readonly');
            const store = tx.objectStore(this.STORE_NAME);
            const req = store.get(this.KEY_ID);
            req.onsuccess = () => resolve(req.result || null);
            req.onerror = () => reject(req.error);
        });
    },

    // Convert ArrayBuffer to Base64
    arrayBufferToBase64: function(buffer) {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    },

    /**
     * Check if this browser already has a registered cryptographic key pair
     */
    isDeviceRegisteredLocally: async function() {
        try {
            const record = await this.loadKeyRecord();
            return !!(record && record.privateKey && record.deviceId);
        } catch (e) {
            console.error('Error checking local device registration:', e);
            return false;
        }
    },

    /**
     * Generates a new ECDSA key pair and registers it with the Spring Boot backend
     */
    registerCurrentDevice: async function(deviceName) {
        console.log('Generating ECDSA P-256 key pair using Web Crypto API...');
        const keyPair = await window.crypto.subtle.generateKey(
            {
                name: 'ECDSA',
                namedCurve: 'P-256'
            },
            false, // Private key is non-extractable from memory
            ['sign']
        );

        // Export public key in standard SPKI format
        const spkiBuffer = await window.crypto.subtle.exportKey('spki', keyPair.publicKey);
        const publicKeyBase64 = this.arrayBufferToBase64(spkiBuffer);

        console.log('Registering public key with backend...');
        const deviceData = await fetchWithCsrf('/api/student/devices/register', {
            method: 'POST',
            body: JSON.stringify({
                deviceName: deviceName || navigator.userAgent.substring(0, 50),
                publicKey: publicKeyBase64,
                keyAlgorithm: 'EC'
            })
        });

        // Store private key and backend deviceId securely in IndexedDB
        await this.storeKeyRecord({
            privateKey: keyPair.privateKey,
            publicKeyBase64: publicKeyBase64,
            deviceId: deviceData.deviceId,
            deviceName: deviceData.deviceName,
            registeredAt: Date.now()
        });

        console.log('Device registered successfully! Device ID:', deviceData.deviceId);
        return deviceData;
    },

    /**
     * Obtains challenge from backend, signs it with the local private key, and returns verification payload
     */
    solveDeviceChallenge: async function() {
        const record = await this.loadKeyRecord();
        if (!record || !record.privateKey || !record.deviceId) {
            throw new Error('Device is not registered yet. Please click "Register This Device" on your dashboard first.');
        }

        console.log('Requesting device verification challenge for deviceId:', record.deviceId);
        const challengeRes = await fetchWithCsrf('/api/student/devices/challenge', {
            method: 'POST',
            body: JSON.stringify({ deviceId: record.deviceId })
        });

        const challengeValue = challengeRes.challenge;
        console.log('Signing challenge using private key...');

        const encoder = new TextEncoder();
        const dataBytes = encoder.encode(challengeValue);

        const signatureBuffer = await window.crypto.subtle.sign(
            {
                name: 'ECDSA',
                hash: { name: 'SHA-256' }
            },
            record.privateKey,
            dataBytes
        );

        const signatureBase64 = this.arrayBufferToBase64(signatureBuffer);

        return {
            deviceId: record.deviceId,
            challengeId: challengeRes.challengeId,
            signature: signatureBase64
        };
    }
};

