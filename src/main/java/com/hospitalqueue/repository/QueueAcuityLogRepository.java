package com.hospitalqueue.repository;

import com.hospitalqueue.model.QueueAcuityLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class QueueAcuityLogRepository {

    private final JdbcTemplate jdbc;

    public QueueAcuityLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<QueueAcuityLog> MAPPER = new RowMapper<>() {
        @Override
        public QueueAcuityLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            QueueAcuityLog log = new QueueAcuityLog();
            log.setLogId(rs.getLong("log_id"));
            log.setQueueId(rs.getLong("queue_id"));
            log.setTriageId(rs.getLong("triage_id"));
            log.setOriginalPriority(rs.getInt("original_priority"));
            log.setEscalatedPriority(rs.getInt("escalated_priority"));
            log.setAcuityScore(rs.getInt("acuity_score"));
            log.setDisposition(rs.getString("disposition"));
            log.setEscalationReason(rs.getString("escalation_reason"));
            log.setTriggeredBy(rs.getString("triggered_by"));
            log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return log;
        }
    };

    public QueueAcuityLog save(QueueAcuityLog log) {
        String sql = """
            INSERT INTO queue_acuity_log (queue_id, triage_id, original_priority, escalated_priority,
                                          acuity_score, disposition, escalation_reason, triggered_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING log_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                log.getQueueId(), log.getTriageId(), log.getOriginalPriority(),
                log.getEscalatedPriority(), log.getAcuityScore(), log.getDisposition(),
                log.getEscalationReason(), log.getTriggeredBy());
        log.setLogId(id);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    public List<QueueAcuityLog> findByQueueId(Long queueId) {
        String sql = "SELECT * FROM queue_acuity_log WHERE queue_id = ? ORDER BY created_at";
        return jdbc.query(sql, MAPPER, queueId);
    }

    public List<QueueAcuityLog> findRecentEscalations(int limit) {
        String sql = "SELECT * FROM queue_acuity_log WHERE escalated_priority < original_priority ORDER BY created_at DESC LIMIT ?";
        return jdbc.query(sql, MAPPER, limit);
    }
}