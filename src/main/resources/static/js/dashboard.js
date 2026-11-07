/**
 * CampusPass - Academic Attendance & Verification Portal
 * Core Client Utilities & Security Orchestrator
 */

// CSRF Helpers
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
}

function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
}

/**
 * Standard fetch wrapper that automatically attaches CSRF headers and JSON content-type
 */
async function fetchWithCsrf(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    const config = {
        ...options,
        headers
    };

    const response = await fetch(url, config);
    if (!response.ok) {
        let errMessage = 'Request failed with status ' + response.status;
        try {
            const errData = await response.json();
            if (errData.message) errMessage = errData.message;
        } catch (_) {}
        throw new Error(errMessage);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return response;
}

/**
 * Clean institutional toast notifications
 */
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; display: flex; flex-direction: column; gap: 8px; pointer-events: none;';
        document.body.appendChild(container);
    }

    const configs = {
        success: { bg: '#059669', color: '#ffffff', svg: '<svg class="heroicon-sm" style="flex-shrink:0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>' },
        danger:  { bg: '#dc2626', color: '#ffffff', svg: '<svg class="heroicon-sm" style="flex-shrink:0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>' },
        warning: { bg: '#b45309', color: '#ffffff', svg: '<svg class="heroicon-sm" style="flex-shrink:0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" /></svg>' },
        info:    { bg: '#1d4ed8', color: '#ffffff', svg: '<svg class="heroicon-sm" style="flex-shrink:0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" /></svg>' }
    };

    const cfg = configs[type] || configs.info;
    const toast = document.createElement('div');
    toast.style.cssText = `
        background-color: ${cfg.bg};
        color: ${cfg.color};
        padding: 10px 16px;
        border-radius: 6px;
        box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.15), 0 4px 6px -4px rgba(15, 23, 42, 0.1);
        font-weight: 500;
        font-size: 0.85rem;
        min-width: 260px;
        max-width: 400px;
        opacity: 0;
        transform: translateY(-8px);
        transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        display: flex;
        align-items: center;
        gap: 10px;
        pointer-events: auto;
    `;
    toast.innerHTML = `${cfg.svg} <span>${escapeHtml(message)}</span>`;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
    }, 10);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-8px)';
        setTimeout(() => toast.remove(), 250);
    }, 3800);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Generate unique client request ID
function generateUUID() {
    if (crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return 'req-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now().toString(36);
}

// Mobile Drawer & Accessibility Navigation Setup
document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('dashboardSidebar');
    const toggleBtn = document.getElementById('sidebarToggle');
    const closeBtn = document.getElementById('sidebarCloseBtn');
    const backdrop = document.getElementById('sidebarBackdrop');

    function openSidebar() {
        if (!sidebar) return;
        sidebar.classList.add('show');
        if (backdrop) backdrop.classList.add('show');
        if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'true');
        if (closeBtn) closeBtn.focus();
    }

    function closeSidebar() {
        if (!sidebar) return;
        sidebar.classList.remove('show');
        if (backdrop) backdrop.classList.remove('show');
        if (toggleBtn) {
            toggleBtn.setAttribute('aria-expanded', 'false');
            toggleBtn.focus();
        }
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (sidebar && sidebar.classList.contains('show')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
            e.preventDefault();
            closeSidebar();
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', () => {
            closeSidebar();
        });
    }

    // Keyboard accessibility: Close drawer on Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && sidebar && sidebar.classList.contains('show')) {
            closeSidebar();
        }
    });

    // Auto-close sidebar when clicking links on mobile
    if (sidebar) {
        const navLinks = sidebar.querySelectorAll('.nav-link-custom');
        navLinks.forEach(link => {
            link.addEventListener('click', () => {
                if (window.innerWidth < 992) {
                    closeSidebar();
                }
            });
        });
    }
});
