package com.hospitalqueue.service;

import com.hospitalqueue.model.Notification;
import com.hospitalqueue.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(String patientId, String message) {
        notificationRepository.insert(patientId, message);
    }

    public int getUnreadCount(String patientId) {
        return notificationRepository.countUnread(patientId);
    }

    public List<Notification> getNotifications(String patientId) {
        return notificationRepository.findByPatient(patientId);
    }

    public void markAllRead(String patientId) {
        notificationRepository.markAllRead(patientId);
    }
}
