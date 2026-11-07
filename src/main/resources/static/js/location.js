/**
 * QR Attendance Management System - HTML5 GPS Geolocation Module
 */

const GPSLocationManager = {
    /**
     * Obtains the student's high-accuracy GPS coordinates using HTML5 Geolocation.
     * @returns {Promise<{latitude: number, longitude: number, accuracy: number, timestamp: number}>}
     */
    getCurrentLocation: function() {
        return new Promise((resolve, reject) => {
            if (!('geolocation' in navigator)) {
                reject(new Error('Geolocation is not supported by your browser. Please use a modern browser on mobile.'));
                return;
            }

            const options = {
                enableHighAccuracy: true,
                timeout: 12000,
                maximumAge: 0 // Do not use cached GPS data
            };

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    const coords = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                        accuracy: position.coords.accuracy,
                        timestamp: position.timestamp || Date.now()
                    };
                    console.log('GPS coordinates acquired:', coords);
                    resolve(coords);
                },
                (error) => {
                    let errMsg = 'Failed to obtain GPS location.';
                    switch (error.code) {
                        case error.PERMISSION_DENIED:
                            errMsg = 'Location permission was denied. Please allow location access in your browser settings to verify attendance.';
                            break;
                        case error.POSITION_UNAVAILABLE:
                            errMsg = 'Location information is unavailable. Ensure GPS/Location service is turned on in your device settings.';
                            break;
                        case error.TIMEOUT:
                            errMsg = 'Location request timed out. Move closer to a window or outdoors and retry.';
                            break;
                    }
                    console.error('Geolocation error:', error);
                    reject(new Error(errMsg));
                },
                options
            );
        });
    }
};

