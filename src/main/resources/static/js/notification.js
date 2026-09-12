// Notification badge polling and popup notifications for the patient header.
// Supports both in-page toast notifications and browser Notification API popups.
(function () {
    var badge = document.getElementById('notification-badge');
    var bellLink = document.getElementById('notification-bell');
    if (!bellLink) return;

    var lastCount = 0;
    var toastContainer = null;

    function createToastContainer() {
        if (toastContainer) return;
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        document.body.appendChild(toastContainer);
    }

    function showToast(message, type) {
        createToastContainer();
        var isUrgent = type === 'emergency' || type === 'error';
        var toast = document.createElement('div');
        toast.className = 'toast ' + (isUrgent ? 'error' : 'success');
        var iconName = isUrgent ? 'notification_important' : 'check_circle';
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
        }, 6000);
    }

    // Show browser notification popup (requires permission)
    function showBrowserNotification(title, body, type) {
        if (!('Notification' in window)) return;
        if (Notification.permission === 'granted') {
            try {
                var notif = new Notification(title, { body: body, tag: 'hqs-' + Date.now() });
                notif.onclick = function () { window.focus(); notif.close(); };
                setTimeout(function () { notif.close(); }, 8000);
            } catch (e) {
                // Fallback: some browsers don't support Notification in certain contexts
            }
        }
    }

    // Request browser notification permission
    function requestNotificationPermission() {
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }

    // Determine notification type from message content
    function getNotificationType(message) {
        if (!message) return 'info';
        var lower = message.toLowerCase();
        if (lower.indexOf('emergency') !== -1) return 'emergency';
        if (lower.indexOf('turn') !== -1 || lower.indexOf('called') !== -1) return 'success';
        if (lower.indexOf('cancel') !== -1 || lower.indexOf('expired') !== -1) return 'warning';
        if (lower.indexOf('completed') !== -1) return 'success';
        return 'info';
    }

    function refresh() {
        fetch(bellLink.getAttribute('data-url'))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var currentCount = data.count || 0;
                if (badge) {
                    badge.textContent = currentCount || '';
                    badge.style.display = currentCount > 0 ? 'inline-block' : 'none';
                }

                // Fetch actual notification messages if there are new notifications
                // Only show popups when count increases (new notifications arriving), not on first load
                if (currentCount > lastCount && lastCount > 0) {
                    var notificationsUrl = bellLink.getAttribute('data-url');
                    fetch(notificationsUrl)
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var notifications = data.list || data;
                            if (Array.isArray(notifications)) {
                                var newNotifs = notifications.filter(function (n) { return !n.read; });
                                newNotifs.slice(0, 3).forEach(function (n) {
                                    var type = getNotificationType(n.message);
                                    showToast(n.message, type);
                                    showBrowserNotification('CareFlow', n.message, type);
                                });
                            }
                        })
                        .catch(function () {
                            var newNotifCount = currentCount - lastCount;
                            showToast('You have ' + newNotifCount + ' new notification' + (newNotifCount > 1 ? 's' : ''), 'info');
                        });
                }
                lastCount = currentCount;
            })
            .catch(function () {});
    }

    // Request browser notification permission on load
    requestNotificationPermission();

    refresh();
    setInterval(refresh, 15000);
})();
