package com.hospitalqueue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalqueue.model.Notification;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.service.NotificationService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PatientNotificationController {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatientNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/patient/notifications/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String notificationsJson(HttpSession session) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "{\"count\":0,\"list\":[]}";
        }
        int unread = notificationService.getUnreadCount(patient.getPatientId());
        List<Notification> notifications = notificationService.getNotifications(patient.getPatientId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getNotificationId());
            item.put("message", n.getMessage());
            item.put("read", n.isRead());
            item.put("createdAt", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString());
            list.add(item);
        }
        try {
            return objectMapper.writeValueAsString(Map.of("count", unread, "list", list));
        } catch (Exception e) {
            return "{\"count\":0,\"list\":[]}";
        }
    }

    @PostMapping(value = "/patient/notifications/mark-read", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String markRead(HttpSession session) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "{\"success\":false}";
        }
        notificationService.markAllRead(patient.getPatientId());
        return "{\"success\":true}";
    }
}
