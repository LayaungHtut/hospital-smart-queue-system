package com.hospitalqueue.repository;

import com.hospitalqueue.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Notification> mapper = (RowMapper<Notification>) (rs, rowNum) -> {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setPatientId(rs.getString("patient_id"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        if (rs.getTimestamp("created_at") != null) {
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return n;
    };

    public void insert(String patientId, String message) {
        jdbcTemplate.update(
                "INSERT INTO notification (patient_id, message, is_read) VALUES (?, ?, FALSE)",
                patientId, message);
    }

    public int countUnread(String patientId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE patient_id = ? AND is_read = FALSE", Integer.class, patientId);
        return c == null ? 0 : c;
    }

    public List<Notification> findByPatient(String patientId) {
        return jdbcTemplate.query(
                "SELECT * FROM notification WHERE patient_id = ? ORDER BY notification_id DESC LIMIT 20",
                (RowMapper<Notification>) mapper, patientId);
    }

    public void markAllRead(String patientId) {
        jdbcTemplate.update("UPDATE notification SET is_read = TRUE WHERE patient_id = ? AND is_read = FALSE", patientId);
    }
}
