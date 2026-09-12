// Queue tracking polling for the patient queue page with status change notifications.
// Shows in-page toast notifications and browser Notification API popups for status changes.
(function () {
    var queueDataEl = document.getElementById('queue-data');
    var previousStatus = null;
    var toastContainer = null;

    function createToastContainer() {
        if (toastContainer) return;
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        document.body.appendChild(toastContainer);
    }

    function showToast(message, type) {
        createToastContainer();
        var isUrgent = type === 'error';
        var toast = document.createElement('div');
        toast.className = 'toast ' + (isUrgent ? 'error' : 'success');
        var iconName = isUrgent ? 'error' : 'check_circle';
        toast.innerHTML =
            '<span class="material-symbols-outlined" style="font-size:20px;flex-shrink:0">' + iconName + '</span>' +
            '<span>' + message + '</span>';
        toast.style.cursor = 'pointer';
        toast.onclick = function () { toast.remove(); };
        toastContainer.appendChild(toast);
        setTimeout(function () {
            if (toast.parentElement) {
                toast.style.opacity = '0';
                toast.style.transform = 'translateX(100%)';
                toast.style.transition = 'all 0.3s';
                setTimeout(function () { toast.remove(); }, 300);
            }
        }, 8000);
    }

    // Show browser notification popup
    function showBrowserNotification(title, body) {
        if (!('Notification' in window)) return;
        if (Notification.permission === 'granted') {
            try {
                var notif = new Notification(title, { body: body, tag: 'hqs-queue-' + Date.now() });
                notif.onclick = function () { window.focus(); notif.close(); };
                setTimeout(function () { notif.close(); }, 8000);
            } catch (e) { /* fallback silently */ }
        }
    }

    function refresh() {
        if (!queueDataEl) return;
        fetch(queueDataEl.getAttribute('data-url'))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var statusEl = document.getElementById('queue-status');
                var positionEl = document.getElementById('queue-position');
                var waitEl = document.getElementById('queue-wait');
                var numberEl = document.getElementById('queue-number');

                if (statusEl && data.status) {
                    statusEl.textContent = data.status;
                    statusEl.className = 'badge ' + 'badge-' + data.status.toLowerCase();

                    // Show notification on status change
                    if (previousStatus && previousStatus !== data.status) {
                        var message = '';
                        var toastType = 'info';
                        switch (data.status) {
                            case 'CALLED':
                                message = 'Your turn! Queue number ' + (data.queueNumber || '') + ' - Please proceed to the doctor now.';
                                toastType = 'success';
                                break;
                            case 'SERVING':
                                message = 'Consultation has started for queue number ' + (data.queueNumber || '') + '.';
                                toastType = 'info';
                                break;
                            case 'CANCELLED':
                                message = 'Your queue number ' + (data.queueNumber || '') + ' has been cancelled.';
                                toastType = 'error';
                                break;
                            case 'EXPIRED':
                                message = 'Your queue number ' + (data.queueNumber || '') + ' has expired. Please register again.';
                                toastType = 'error';
                                break;
                            case 'COMPLETED':
                                message = 'Consultation completed for queue number ' + (data.queueNumber || '') + '. Thank you!';
                                toastType = 'success';
                                break;
                        }
                        if (message) {
                            showToast(message, toastType);
                            showBrowserNotification('CareFlow', message);
                        }
                    }
                    previousStatus = data.status;
                }
                if (positionEl) positionEl.textContent = data.position;
                if (waitEl) waitEl.textContent = data.estimatedWaitingTime + ' min';
                if (numberEl) numberEl.textContent = data.queueNumber;
            })
            .catch(function () {});
    }

    // Request browser notification permission
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }

    // Initial fetch
    refresh();
    setInterval(refresh, 8000);
})();
