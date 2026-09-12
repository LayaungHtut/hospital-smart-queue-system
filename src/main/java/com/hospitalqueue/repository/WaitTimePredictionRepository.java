package com.hospitalqueue.repository;

import com.hospitalqueue.model.WaitTimePrediction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WaitTimePredictionRepository {

    private final JdbcTemplate jdbc;

    public WaitTimePredictionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<WaitTimePrediction> MAPPER = new RowMapper<>() {
        @Override
        public WaitTimePrediction mapRow(ResultSet rs, int rowNum) throws SQLException {
            WaitTimePrediction p = new WaitTimePrediction();
            p.setPredictionId(rs.getLong("prediction_id"));
            p.setQueueId(rs.getLong("queue_id"));
            p.setPatientId(rs.getString("patient_id"));
            p.setDoctorId(rs.getString("doctor_id"));
            p.setDepartmentId(rs.getInt("department_id"));
            p.setModelId(rs.getLong("model_id"));
            p.setFeaturesJson(rs.getString("features_json"));
            p.setPredictedWaitMin(rs.getDouble("predicted_wait_min"));
            var actual = rs.getObject("actual_wait_min", Double.class);
            p.setActualWaitMin(actual);
            p.setPredictedAt(rs.getTimestamp("predicted_at").toLocalDateTime());
            var resolved = rs.getTimestamp("resolved_at");
            p.setResolvedAt(resolved != null ? resolved.toLocalDateTime() : null);
            return p;
        }
    };

    public WaitTimePrediction save(WaitTimePrediction prediction) {
        String sql = """
            INSERT INTO wait_time_prediction (queue_id, patient_id, doctor_id, department_id, model_id,
                                              features_json, predicted_wait_min)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
            RETURNING prediction_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                prediction.getQueueId(), prediction.getPatientId(), prediction.getDoctorId(),
                prediction.getDepartmentId(), prediction.getModelId(),
                prediction.getFeaturesJson(), prediction.getPredictedWaitMin());
        prediction.setPredictionId(id);
        prediction.setPredictedAt(LocalDateTime.now());
        return prediction;
    }

    public void updateActualWait(Long predictionId, double actualWaitMin) {
        String sql = "UPDATE wait_time_prediction SET actual_wait_min = ?, resolved_at = CURRENT_TIMESTAMP WHERE prediction_id = ?";
        jdbc.update(sql, actualWaitMin, predictionId);
    }

    public Optional<WaitTimePrediction> findByQueueId(Long queueId) {
        String sql = "SELECT * FROM wait_time_prediction WHERE queue_id = ? ORDER BY predicted_at DESC LIMIT 1";
        List<WaitTimePrediction> results = jdbc.query(sql, MAPPER, queueId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<WaitTimePrediction> findByDoctorAndDateRange(String doctorId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM wait_time_prediction WHERE doctor_id = ? AND predicted_at BETWEEN ? AND ? ORDER BY predicted_at";
        return jdbc.query(sql, MAPPER, doctorId, start, end);
    }

    public List<WaitTimePrediction> findRecentWithActuals(int limit) {
        String sql = "SELECT * FROM wait_time_prediction WHERE actual_wait_min IS NOT NULL ORDER BY resolved_at DESC LIMIT ?";
        return jdbc.query(sql, MAPPER, limit);
    }
}