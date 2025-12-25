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