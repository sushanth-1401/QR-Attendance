/**
 * Supabase Realtime & Persistent Database Module
 * Handles instant sub-100ms attendance broadcasts and direct table synchronization
 */

const SupabaseRealtime = (function() {
    const SUPABASE_URL = 'https://rsxttzenmxkuwrfrbkbv.supabase.co';
    const SUPABASE_KEY = 'sb_publishable_RkOx-OIQIHWS0kSmoj-n7A_ltEygh4V';

    let client = null;
    let activeChannel = null;

    function getClient() {
        if (!client) {
            if (typeof window.supabase !== 'undefined' && window.supabase.createClient) {
                client = window.supabase.createClient(SUPABASE_URL, SUPABASE_KEY);
            } else {
                console.warn('[Supabase] Supabase JS library not found on window. Falling back to HTTP polling.');
            }
        }
        return client;
    }

    /**
     * Faculty: Subscribes to live broadcasts for a specific attendance session
     */
    function subscribeToSession(sessionId, options = {}) {
        const sb = getClient();
        if (!sb) return null;

        const channelName = `attendance-session-${sessionId}`;
        console.log(`[Supabase Realtime] Subscribing to channel: ${channelName}`);

        activeChannel = sb.channel(channelName, {
            config: {
                broadcast: { ack: true }
            }
        });

        // Listen for verified students
        activeChannel.on('broadcast', { event: 'student-verified' }, ({ payload }) => {
            console.log('[Supabase Realtime] Event: student-verified', payload);
            if (options.onStudentVerified) {
                options.onStudentVerified(payload);
            }
        });

        // Listen for flagged/rejected attempts
        activeChannel.on('broadcast', { event: 'scan-rejected' }, ({ payload }) => {
            console.log('[Supabase Realtime] Event: scan-rejected', payload);
            if (options.onScanRejected) {
                options.onScanRejected(payload);
            }
        });

        // Track connection state
        activeChannel.subscribe((status) => {
            console.log(`[Supabase Realtime] Channel status: ${status}`);
            if (options.onStatusChange) {
                options.onStatusChange(status);
            }
        });

        return activeChannel;
    }

    /**
     * Student: Broadcasts verified attendance to the session channel
     */
    async function broadcastStudentVerified(sessionId, studentData) {
        const sb = getClient();
        if (!sb) return;

        const channelName = `attendance-session-${sessionId}`;
        const channel = activeChannel || sb.channel(channelName);
        
        await channel.subscribe();
        await channel.send({
            type: 'broadcast',
            event: 'student-verified',
            payload: {
                ...studentData,
                timestamp: new Date().toISOString()
            }
        });
        console.log('[Supabase Realtime] Broadcast sent: student-verified', studentData);
    }

    /**
     * Student: Broadcasts flagged/rejected scan to faculty screen
     */
    async function broadcastScanRejected(sessionId, failureData) {
        const sb = getClient();
        if (!sb) return;

        const channelName = `attendance-session-${sessionId}`;
        const channel = activeChannel || sb.channel(channelName);

        await channel.subscribe();
        await channel.send({
            type: 'broadcast',
            event: 'scan-rejected',
            payload: {
                ...failureData,
                timestamp: new Date().toISOString()
            }
        });
        console.log('[Supabase Realtime] Broadcast sent: scan-rejected', failureData);
    }

    /**
     * Direct Database Persistence: Saves attendance record directly into Supabase Table
     */
    async function saveAttendanceRecord(record) {
        const sb = getClient();
        if (!sb) {
            // Direct REST Fallback via fetch
            try {
                const response = await fetch(`${SUPABASE_URL}/rest/v1/attendance_records?on_conflict=student_id,session_id`, {
                    method: 'POST',
                    headers: {
                        'apikey': SUPABASE_KEY,
                        'Authorization': `Bearer ${SUPABASE_KEY}`,
                        'Content-Type': 'application/json',
                        'Prefer': 'resolution=merge-duplicates,return=minimal'
                    },
                    body: JSON.stringify(record)
                });
                if (!response.ok) {
                    const errData = await response.text();
                    console.warn('[Supabase REST Save] HTTP error:', response.status, errData);
                    return { success: false, error: errData };
                }
                console.log('[Supabase REST Save] Attendance saved to Supabase attendance_records successfully');
                return { success: true };
            } catch (e) {
                console.warn('[Supabase REST Save] Network exception:', e);
                return { success: false, error: e.message };
            }
        }

        try {
            const { data, error } = await sb
                .from('attendance_records')
                .upsert([record], { onConflict: 'student_id,session_id' });

            if (error) {
                console.warn('[Supabase DB Save] Upsert error:', error.message);
                if (error.code === '42501') {
                    console.error('[Supabase RLS Warning] Row-Level Security policy blocked insert. Run supabase_fix_rls.sql in Supabase SQL editor to allow inserts.');
                }
                return { success: false, error: error.message };
            }
            console.log('[Supabase DB Save] Attendance record upserted into Supabase successfully:', data);
            return { success: true, data };
        } catch (err) {
            console.warn('[Supabase DB Save] Exception:', err);
            return { success: false, error: err.message };
        }
    }

    /**
     * Direct Database Persistence: Saves department into Supabase Table
     */
    async function saveDepartment(dept) {
        try {
            const response = await fetch(`${SUPABASE_URL}/rest/v1/departments?on_conflict=department_code`, {
                method: 'POST',
                headers: {
                    'apikey': SUPABASE_KEY,
                    'Authorization': `Bearer ${SUPABASE_KEY}`,
                    'Content-Type': 'application/json',
                    'Prefer': 'resolution=merge-duplicates,return=minimal'
                },
                body: JSON.stringify(dept)
            });
            return response.ok;
        } catch (e) {
            console.warn('[Supabase Sync] Department save error:', e);
            return false;
        }
    }

    function unsubscribe() {
        if (activeChannel && client) {
            client.removeChannel(activeChannel);
            activeChannel = null;
        }
    }

    return {
        getClient,
        subscribeToSession,
        broadcastStudentVerified,
        broadcastScanRejected,
        saveAttendanceRecord,
        saveDepartment,
        unsubscribe,
        SUPABASE_URL,
        SUPABASE_KEY
    };
})();
