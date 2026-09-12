package com.hospitalqueue.repository;

import com.hospitalqueue.model.NoShowPrediction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NoShowPredictionRepository {

    private final JdbcTemplate jdbc;

    public NoShowPredictionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<NoShowPrediction> MAPPER = new RowMapper<>() {
        @Override
        public NoShowPrediction mapRow(ResultSet rs, int rowNum) throws SQLException {
            NoShowPrediction p = new NoShowPrediction();
            p.setPredictionId(rs.getLong("prediction_id"));
            p.setAppointmentId(rs.getLong("appointment_id"));
            p.setPatientId(rs.getString("patient_id"));
            p.setDoctorId(rs.getString("doctor_id"));
            p.setModelId(rs.getLong("model_id"));
            p.setFeaturesJson(rs.getString("features_json"));
            p.setPredictedProbability(rs.getDouble("predicted_probability"));
            p.setRiskLevel(rs.getString("risk_level"));
            p.setThresholdUsed(rs.getDouble("threshold_used"));
            p.setPredictedNoShow(rs.getBoolean("predicted_noshow"));
            var actual = rs.getObject("actual_noshow", Boolean.class);
            p.setActualNoShow(actual);
            p.setPredictedAt(rs.getTimestamp("predicted_at").toLocalDateTime());
            var resolved = rs.getTimestamp("resolved_at");
            p.setResolvedAt(resolved != null ? resolved.toLocalDateTime() : null);
            return p;
        }
    };

    public NoShowPrediction save(NoShowPrediction prediction) {
        String sql = """
            INSERT INTO noshow_prediction (appointment_id, patient_id, doctor_id, model_id,
                                           features_json, predicted_probability, risk_level, 
                                           threshold_used, predicted_noshow)
            VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            RETURNING prediction_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                prediction.getAppointmentId(), prediction.getPatientId(), prediction.getDoctorId(),
                prediction.getModelId(), prediction.getFeaturesJson(),
                prediction.getPredictedProbability(), prediction.getRiskLevel(),
                prediction.getThresholdUsed(), prediction.isPredictedNoShow());
        prediction.setPredictionId(id);
        prediction.setPredictedAt(LocalDateTime.now());
        return prediction;
    }

    public void updateActualOutcome(Long predictionId, boolean actualNoShow) {
        String sql = "UPDATE noshow_prediction SET actual_noshow = ?, resolved_at = CURRENT_TIMESTAMP WHERE prediction_id = ?";
        jdbc.update(sql, actualNoShow, predictionId);
    }

    public Optional<NoShowPrediction> findByAppointmentId(Long appointmentId) {
        String sql = "SELECT * FROM noshow_prediction WHERE appointment_id = ? ORDER BY predicted_at DESC LIMIT 1";
        List<NoShowPrediction> results = jdbc.query(sql, MAPPER, appointmentId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<NoShowPrediction> findByPatientId(String patientId) {
        String sql = "SELECT * FROM noshow_prediction WHERE patient_id = ? ORDER BY predicted_at DESC";
        return jdbc.query(sql, MAPPER, patientId);
    }

    public List<NoShowPrediction> findHighRiskAppointments(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT * FROM noshow_prediction 
            WHERE risk_level = 'HIGH' AND predicted_at BETWEEN ? AND ?
            ORDER BY predicted_probability DESC
            """;
        return jdbc.query(sql, MAPPER, from, to);
    }
}