package com.hospitalqueue.repository;

import com.hospitalqueue.model.Queue;
import com.hospitalqueue.model.QueueHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class QueueRepository {

    private final JdbcTemplate jdbcTemplate;

    public QueueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Queue> mapper = (RowMapper<Queue>) (ResultSet rs, int rowNum) -> mapQueue(rs);

    private Queue mapQueue(ResultSet rs) throws SQLException {
        Queue q = new Queue();
        q.setQueueId(rs.getLong("queue_id"));
        q.setQueueNumber(rs.getString("queue_number"));
        q.setPatientId(rs.getString("patient_id"));
        q.setDoctorId(rs.getString("doctor_id"));
        q.setDepartmentId(rs.getInt("department_id"));
        q.setPriority(rs.getString("priority"));
        q.setStatus(rs.getString("status"));
        q.setPosition(rs.getInt("position"));
        q.setEstimatedWaitingTime(rs.getLong("estimated_waiting_time"));
        q.setSource(rs.getString("source"));
        q.setEmergency(rs.getBoolean("is_emergency"));
        q.setEmergencyConfirmed(rs.getBoolean("emergency_confirmed"));
        q.setCancelReason(rs.getString("cancel_reason"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null)
            q.setCreatedAt(t.toLocalDateTime());
        t = rs.getTimestamp("checked_in_at");
        if (t != null)
            q.setCheckedInAt(t.toLocalDateTime());
        t = rs.getTimestamp("called_at");
        if (t != null)
            q.setCalledAt(t.toLocalDateTime());
        t = rs.getTimestamp("started_at");
        if (t != null)
            q.setStartedAt(t.toLocalDateTime());
        t = rs.getTimestamp("paused_at");
        if (t != null)
            q.setPausedAt(t.toLocalDateTime());
        t = rs.getTimestamp("completed_at");
        if (t != null)
            q.setCompletedAt(t.toLocalDateTime());
        t = rs.getTimestamp("cancelled_at");
        if (t != null)
            q.setCancelledAt(t.toLocalDateTime());
        return q;
    }

    public void insert(Queue queue) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO queue (queue_number, patient_id, doctor_id, department_id, priority, status, position, "
                            + "estimated_waiting_time, source, is_emergency, emergency_confirmed, created_at, checked_in_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "queue_id" });
            ps.setString(1, queue.getQueueNumber());
            ps.setString(2, queue.getPatientId());
            ps.setString(3, queue.getDoctorId());
            ps.setInt(4, queue.getDepartmentId());
            ps.setString(5, queue.getPriority());
            ps.setString(6, queue.getStatus());
            ps.setInt(7, queue.getPosition());
            ps.setLong(8, queue.getEstimatedWaitingTime());
            ps.setString(9, queue.getSource());
            ps.setBoolean(10, queue.isEmergency());
            ps.setBoolean(11, queue.isEmergencyConfirmed());
            ps.setTimestamp(12, queue.getCreatedAt() != null ? Timestamp.valueOf(queue.getCreatedAt()) : null);
            ps.setTimestamp(13, queue.getCheckedInAt() != null ? Timestamp.valueOf(queue.getCheckedInAt()) : null);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            queue.setQueueId(key.longValue());
        }
    }

    public Queue findActiveQueueByPatientId(String patientId) {
        List<Queue> list = jdbcTemplate.query(
                "SELECT * FROM queue WHERE patient_id = ? AND status IN ('WAITING', 'CALLED', 'SERVING') ORDER BY queue_id DESC LIMIT 1",
                (RowMapper<Queue>) mapper, patientId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Queue> findQueuesByPatientId(String patientId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE patient_id = ? ORDER BY queue_id DESC",
                (RowMapper<Queue>) mapper, patientId);
    }

    public Queue findById(long queueId) {
        List<Queue> list = jdbcTemplate.query(
                "SELECT * FROM queue WHERE queue_id = ?", (RowMapper<Queue>) mapper, queueId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Queue findByNumber(String queueNumber) {
        List<Queue> list = jdbcTemplate.query(
                "SELECT * FROM queue WHERE queue_number = ?", (RowMapper<Queue>) mapper, queueNumber);
        return list.isEmpty() ? null : list.get(0);
    }

    public String findLatestQueueNumber(String prefix) {
        List<String> list = jdbcTemplate.queryForList(
                "SELECT queue_number FROM queue WHERE queue_number LIKE ? ORDER BY queue_id DESC LIMIT 1",
                String.class, prefix + "%");
        return list.isEmpty() ? null : list.get(0);
    }

    public int countWaitingByDoctor(String doctorId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND status = 'WAITING'", Integer.class, doctorId);
        return c == null ? 0 : c;
    }

    public int countWaitingByDepartment(int departmentId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE department_id = ? AND status = 'WAITING'", Integer.class,
                departmentId);
        return c == null ? 0 : c;
    }

    public java.util.Map<String, Integer> countWaitingByAllDoctors() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        jdbcTemplate.query("SELECT doctor_id, COUNT(*) AS total FROM queue WHERE status = 'WAITING' GROUP BY doctor_id",
                rs -> {
                    counts.put(rs.getString("doctor_id"), rs.getInt("total"));
                });
        return counts;
    }

    public List<Queue> findWaitingQueuesByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE doctor_id = ? AND status = 'WAITING' ORDER BY "
                        + "CASE priority WHEN 'EMERGENCY' THEN 1 WHEN 'APPOINTMENT' THEN 2 ELSE 3 END, position",
                (RowMapper<Queue>) mapper, doctorId);
    }

    public List<Queue> findServingByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE doctor_id = ? AND status = 'SERVING' ORDER BY started_at LIMIT 1",
                (RowMapper<Queue>) mapper, doctorId);
    }

    public boolean updateStatus(long queueId, String status) {
        int rows = jdbcTemplate.update("UPDATE queue SET status = ? WHERE queue_id = ?", status, queueId);
        if (rows > 0) {
            Queue q = findById(queueId);
            if (q != null) {
                insertHistory(q, status);
            }
        }
        return rows > 0;
    }

    public void updateEstimatedWaitingTime(long queueId, long waitingTime) {
        jdbcTemplate.update("UPDATE queue SET estimated_waiting_time = ? WHERE queue_id = ?", waitingTime, queueId);
    }

    public void callNext(long queueId) {
        jdbcTemplate.update("UPDATE queue SET status = 'CALLED', called_at = CURRENT_TIMESTAMP WHERE queue_id = ?",
                queueId);
        Queue q = findById(queueId);
        if (q != null)
            insertHistory(q, "CALLED");
    }

    public void start(long queueId) {
        jdbcTemplate.update("UPDATE queue SET status = 'SERVING', started_at = CURRENT_TIMESTAMP WHERE queue_id = ?",
                queueId);
        Queue q = findById(queueId);
        if (q != null)
            insertHistory(q, "SERVING");
    }

    public void complete(long queueId) {
        jdbcTemplate.update(
                "UPDATE queue SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE queue_id = ?", queueId);
        Queue q = findById(queueId);
        if (q != null)
            insertHistory(q, "COMPLETED");
    }

    public void cancel(long queueId, String reason) {
        jdbcTemplate.update(
                "UPDATE queue SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP, cancel_reason = ? WHERE queue_id = ?",
                reason, queueId);
        Queue q = findById(queueId);
        if (q != null)
            insertHistory(q, "CANCELLED");
    }

    public void expire(long queueId) {
        jdbcTemplate.update("UPDATE queue SET status = 'EXPIRED', cancelled_at = CURRENT_TIMESTAMP WHERE queue_id = ?",
                queueId);
        Queue q = findById(queueId);
        if (q != null)
            insertHistory(q, "EXPIRED");
    }

    public void pause(long queueId) {
        jdbcTemplate.update("UPDATE queue SET status = 'PAUSED', paused_at = CURRENT_TIMESTAMP WHERE queue_id = ?",
                queueId);
    }

    public void resume(long queueId) {
        jdbcTemplate.update("UPDATE queue SET status = 'SERVING', paused_at = NULL WHERE queue_id = ?", queueId);
    }

    public void confirmEmergency(long queueId) {
        jdbcTemplate.update("UPDATE queue SET emergency_confirmed = TRUE, priority = 'EMERGENCY' WHERE queue_id = ?",
                queueId);
    }

    public void reassignDoctor(long queueId, String doctorId) {
        jdbcTemplate.update("UPDATE queue SET doctor_id = ? WHERE queue_id = ?", doctorId, queueId);
    }

    private void insertHistory(Queue q, String status) {
        jdbcTemplate.update(
                "INSERT INTO queue_history (queue_id, queue_number, patient_id, doctor_id, status) VALUES (?, ?, ?, ?, ?)",
                q.getQueueId(), q.getQueueNumber(), q.getPatientId(), q.getDoctorId(), status);
    }

    public List<QueueHistory> findHistoryForDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue_history WHERE doctor_id = ? ORDER BY history_id DESC LIMIT 50",
                (RowMapper<QueueHistory>) (ResultSet rs, int rowNum) -> {
                    QueueHistory h = new QueueHistory();
                    h.setHistoryId(rs.getLong("history_id"));
                    h.setQueueId(rs.getLong("queue_id"));
                    h.setQueueNumber(rs.getString("queue_number"));
                    h.setPatientId(rs.getString("patient_id"));
                    h.setDoctorId(rs.getString("doctor_id"));
                    h.setStatus(rs.getString("status"));
                    h.setChangeReason(rs.getString("change_reason"));
                    if (rs.getTimestamp("created_at") != null) {
                        h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    return h;
                }, doctorId);
    }

    public List<Queue> findRecent(int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM queue ORDER BY queue_id DESC LIMIT " + limit, (RowMapper<Queue>) mapper);
    }

    public List<Queue> findAllByStatus(String status) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE (? IS NULL OR status = ?) ORDER BY queue_id DESC",
                (RowMapper<Queue>) mapper, status, status);
    }

    // ---- Statistics helpers ----

    public long countStatus(String status) {
        Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM queue WHERE status = ?", Long.class, status);
        return c == null ? 0 : c;
    }

    public long countWaiting() {
        return countStatus("WAITING");
    }

    public long countServing() {
        return countStatus("SERVING");
    }

    public long countQueuesToday() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE created_at::DATE = CURRENT_DATE", Long.class);
        return c == null ? 0 : c;
    }

    public long countCompletedTodayForDoctor(String doctorId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND status = 'COMPLETED' AND completed_at::DATE = CURRENT_DATE",
                Long.class, doctorId);
        return c == null ? 0 : c;
    }

    public long countCancelledTodayForDoctor(String doctorId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND status = 'CANCELLED' AND cancelled_at::DATE = CURRENT_DATE",
                Long.class, doctorId);
        return c == null ? 0 : c;
    }

    public long countAll() {
        Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM queue", Long.class);
        return c == null ? 0 : c;
    }

    public long countEmergencyToday() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE (is_emergency = TRUE OR priority = 'EMERGENCY') AND created_at::DATE = CURRENT_DATE",
                Long.class);
        return c == null ? 0 : c;
    }

    public long countAppointmentToday() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE priority = 'APPOINTMENT' AND created_at::DATE = CURRENT_DATE",
                Long.class);
        return c == null ? 0 : c;
    }

    public long countNormalToday() {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue WHERE (priority IS NULL OR priority = 'NORMAL') AND is_emergency = FALSE AND created_at::DATE = CURRENT_DATE",
                Long.class);
        return c == null ? 0 : c;
    }

    public java.util.Map<String, Long> getDashboardStatistics() {
        String sql = "SELECT "
                + "COUNT(*) AS total_today, "
                + "COUNT(CASE WHEN is_emergency = TRUE AND emergency_confirmed = FALSE AND status = 'WAITING' THEN 1 END) AS emergency_pending, "
                + "COUNT(CASE WHEN status = 'WAITING' THEN 1 END) AS waiting, "
                + "COUNT(CASE WHEN status = 'COMPLETED' AND completed_at::DATE = CURRENT_DATE THEN 1 END) AS completed, "
                + "COUNT(CASE WHEN (is_emergency = TRUE OR priority = 'EMERGENCY') AND created_at::DATE = CURRENT_DATE THEN 1 END) AS emerg_today, "
                + "COUNT(CASE WHEN priority = 'APPOINTMENT' AND created_at::DATE = CURRENT_DATE THEN 1 END) AS appt_today, "
                + "COUNT(CASE WHEN (priority IS NULL OR priority = 'NORMAL') AND is_emergency = FALSE AND created_at::DATE = CURRENT_DATE THEN 1 END) AS norm_today "
                + "FROM queue WHERE created_at::DATE = CURRENT_DATE OR status = 'WAITING'";

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                java.util.Map<String, Long> map = new java.util.HashMap<>();
                map.put("totalToday", rs.getLong("total_today"));
                map.put("emergencyPending", rs.getLong("emergency_pending"));
                map.put("waiting", rs.getLong("waiting"));
                map.put("completed", rs.getLong("completed"));
                map.put("emergToday", rs.getLong("emerg_today"));
                map.put("apptToday", rs.getLong("appt_today"));
                map.put("normToday", rs.getLong("norm_today"));
                return map;
            });
        } catch (Exception e) {
            java.util.Map<String, Long> fallback = new java.util.HashMap<>();
            fallback.put("totalToday", 0L);
            fallback.put("emergencyPending", 0L);
            fallback.put("waiting", 0L);
            fallback.put("completed", 0L);
            fallback.put("emergToday", 0L);
            fallback.put("apptToday", 0L);
            fallback.put("normToday", 0L);
            return fallback;
        }
    }

    public List<java.util.Map<String, Object>> getWeeklyQueueStatistics() {
        return jdbcTemplate.query(
                "SELECT TO_CHAR(d.day, 'DD Mon') AS date, "
                        + "COUNT(q.queue_id) AS queues, "
                        + "COUNT(CASE WHEN q.status = 'COMPLETED' THEN 1 END) AS completed "
                        + "FROM (SELECT CURRENT_DATE - i AS day FROM generate_series(6, 0, -1) AS i) d "
                        + "LEFT JOIN queue q ON q.created_at::DATE = d.day "
                        + "GROUP BY d.day ORDER BY d.day ASC",
                (RowMapper<java.util.Map<String, Object>>) (rs, rowNum) -> java.util.Map.of(
                        "date", rs.getString("date"),
                        "queues", rs.getLong("queues"),
                        "completed", rs.getLong("completed")));
    }

    public List<java.util.Map<String, Object>> getDepartmentDistribution() {
        return jdbcTemplate.query(
                "SELECT d.department_name AS name, COUNT(q.queue_id) AS count "
                        + "FROM department d "
                        + "LEFT JOIN queue q ON d.department_id = q.department_id AND q.created_at::DATE = CURRENT_DATE "
                        + "GROUP BY d.department_name ORDER BY count DESC",
                (RowMapper<java.util.Map<String, Object>>) (rs, rowNum) -> java.util.Map.of(
                        "name", rs.getString("name"),
                        "count", rs.getLong("count")));
    }

    public List<java.util.Map<String, Object>> getDepartmentReports(java.time.LocalDate date) {
        String dateFilter = (date != null) ? "AND q.created_at::DATE = ?" : "";
        String sql = "SELECT d.department_name, "
                + "COUNT(q.queue_id) AS total, "
                + "COUNT(CASE WHEN q.status = 'COMPLETED' THEN 1 END) AS completed, "
                + "COUNT(CASE WHEN q.status = 'CANCELLED' THEN 1 END) AS cancelled, "
                + "COUNT(CASE WHEN q.status = 'MISSED' THEN 1 END) AS missed, "
                + "COALESCE(AVG(q.estimated_waiting_time), 0) AS avg_wait "
                + "FROM department d "
                + "LEFT JOIN queue q ON d.department_id = q.department_id " + dateFilter + " "
                + "GROUP BY d.department_name ORDER BY d.department_name ASC";

        if (date != null) {
            return jdbcTemplate.query(sql, (RowMapper<java.util.Map<String, Object>>) (rs, rowNum) -> java.util.Map.of(
                    "department", rs.getString("department_name"),
                    "totalQueues", rs.getLong("total"),
                    "completed", rs.getLong("completed"),
                    "cancelled", rs.getLong("cancelled"),
                    "missed", rs.getLong("missed"),
                    "avgWaitingMinutes", Math.round(rs.getDouble("avg_wait"))),
                    java.sql.Date.valueOf(date));
        } else {
            return jdbcTemplate.query(sql, (RowMapper<java.util.Map<String, Object>>) (rs, rowNum) -> java.util.Map.of(
                    "department", rs.getString("department_name"),
                    "totalQueues", rs.getLong("total"),
                    "completed", rs.getLong("completed"),
                    "cancelled", rs.getLong("cancelled"),
                    "missed", rs.getLong("missed"),
                    "avgWaitingMinutes", Math.round(rs.getDouble("avg_wait"))));
        }
    }

    public double getAverageWaitingTimeToday() {
        String sql = "SELECT COALESCE(AVG(estimated_waiting_time), 0) FROM queue WHERE created_at::DATE = CURRENT_DATE";
        Double result = jdbcTemplate.queryForObject(sql, Double.class);
        return result != null ? result : 0.0;
    }

    public List<Queue> findPendingEmergencies() {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE is_emergency = TRUE AND emergency_confirmed = FALSE AND status = 'WAITING' "
                        + "ORDER BY queue_id",
                (RowMapper<Queue>) mapper);
    }

    public List<Queue> findAllWaiting() {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE status = 'WAITING' ORDER BY "
                        + "CASE priority WHEN 'EMERGENCY' THEN 1 WHEN 'APPOINTMENT' THEN 2 ELSE 3 END, position",
                (RowMapper<Queue>) mapper);
    }

    /**
     * Missed-queue detection: waiting queues whose registration is older than the
     * cutoff
     * (patient abandoned the queue without being called).
     */
    public List<Queue> findWaitingQueuesExpiredBefore(java.time.LocalDateTime cutoff) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE status = 'WAITING' AND created_at < ? ORDER BY created_at",
                (RowMapper<Queue>) mapper, java.sql.Timestamp.valueOf(cutoff));
    }

    /**
     * Missed-queue detection: called queues whose patient did not show up within
     * the cutoff
     * (no-show after being called).
     */
    public List<Queue> findCalledQueuesExpiredBefore(java.time.LocalDateTime cutoff) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE status = 'CALLED' AND called_at < ? ORDER BY called_at",
                (RowMapper<Queue>) mapper, java.sql.Timestamp.valueOf(cutoff));
    }

    public List<Queue> findCalledByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE doctor_id = ? AND status = 'CALLED' ORDER BY called_at LIMIT 1",
                (RowMapper<Queue>) mapper, doctorId);
    }

    public List<Queue> findActiveQueuesForDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM queue WHERE doctor_id = ? AND status IN ('CALLED', 'SERVING', 'WAITING') "
                        + "ORDER BY CASE status WHEN 'SERVING' THEN 1 WHEN 'CALLED' THEN 2 ELSE 3 END, "
                        + "CASE priority WHEN 'EMERGENCY' THEN 1 WHEN 'APPOINTMENT' THEN 2 ELSE 3 END, position",
                (RowMapper<Queue>) mapper, doctorId);
    }

    public java.util.Map<String, Queue> findServingByAllDoctors() {
        java.util.Map<String, Queue> map = new java.util.HashMap<>();
        List<Queue> list = jdbcTemplate.query(
                "SELECT * FROM queue WHERE status IN ('SERVING', 'CALLED') ORDER BY queue_id ASC",
                (RowMapper<Queue>) mapper);
        for (Queue q : list) {
            map.putIfAbsent(q.getDoctorId(), q);
        }
        return map;
    }

    public java.util.Map<String, Long> countCompletedTodayByAllDoctors() {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        jdbcTemplate.query(
                "SELECT doctor_id, COUNT(*) AS total FROM queue WHERE status = 'COMPLETED' AND completed_at::DATE = CURRENT_DATE GROUP BY doctor_id",
                rs -> {
                    counts.put(rs.getString("doctor_id"), rs.getLong("total"));
                });
        return counts;
    }

    public Double getPreviousDayAverageConsultationMinutes(String doctorId) {
        try {
            String sql = "SELECT EXTRACT(EPOCH FROM AVG(completed_at - started_at))/60 FROM queue "
                    + "WHERE doctor_id = ? AND status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL "
                    + "AND completed_at::DATE = CURRENT_DATE - INTERVAL '1 day'";
            Double avg = jdbcTemplate.queryForObject(sql, Double.class, doctorId);
            if (avg == null || avg <= 0) {
                String sqlFallback = "SELECT EXTRACT(EPOCH FROM AVG(completed_at - started_at))/60 FROM queue "
                        + "WHERE doctor_id = ? AND status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL";
                avg = jdbcTemplate.queryForObject(sqlFallback, Double.class, doctorId);
            }
            return avg;
        } catch (Exception e) {
            return null;
        }
    }
}
