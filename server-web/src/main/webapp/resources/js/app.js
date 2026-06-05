// Idle timeout handler
function handleIdle() {
    if (typeof PF !== 'undefined' && PF('timeoutWarning')) {
        PF('timeoutWarning').show();
    } else {
        // Get context path from meta tag or body attribute
        var contextPath = document.querySelector('meta[name="context-path"]');
        if (contextPath) {
            window.location.href = contextPath.getAttribute('content');
        } else {
            window.location.href = '/';
        }
    }
}

// Sidebar toggle — persists state in localStorage
function toggleSidebar() {
    var container = document.querySelector('.app-container');
    var collapsed = container.classList.toggle('sidebar-collapsed');
    localStorage.setItem('sidebarCollapsed', collapsed ? '1' : '0');
}

// Restore sidebar state on page load
document.addEventListener('DOMContentLoaded', function () {
    if (localStorage.getItem('sidebarCollapsed') === '1') {
        var container = document.querySelector('.app-container');
        if (container) container.classList.add('sidebar-collapsed');
    }
});