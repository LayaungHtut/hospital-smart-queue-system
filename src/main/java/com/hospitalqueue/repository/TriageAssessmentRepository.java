package com.hospitalqueue.repository;

import com.hospitalqueue.model.TriageAssessment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TriageAssessmentRepository {

    private final JdbcTemplate jdbc;

    public TriageAssessmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<TriageAssessment> MAPPER = new RowMapper<>() {
        @Override
        public TriageAssessment mapRow(ResultSet rs, int rowNum) throws SQLException {
            TriageAssessment a = new TriageAssessment();
            a.setAssessmentId(rs.getLong("assessment_id"));
            a.setPatientId(rs.getString("patient_id"));
            a.setQueueId(rs.getLong("queue_id"));
            a.setDepartmentId(rs.getInt("department_id"));
            a.setSymptomsText(rs.getString("symptoms_text"));
            a.setRecommendedDeptCode(rs.getString("recommended_dept_code"));
            a.setEmergencyFlag(rs.getBoolean("emergency_flag"));
            a.setAcuityScore(rs.getInt("acuity_score"));
            a.setDisposition(rs.getString("disposition"));
            a.setRecommendedTests(rs.getString("recommended_tests"));
            a.setRecommendedLabs(rs.getString("recommended_labs"));
            a.setReason(rs.getString("reason"));
            a.setAiUsed(rs.getBoolean("ai_used"));
            a.setModelVersion(rs.getString("model_version"));
            a.setStaffAcuityScore(rs.getInt("staff_acuity_score"));
            a.setStaffDisposition(rs.getString("staff_disposition"));
            a.setStaffNotes(rs.getString("staff_notes"));
            a.setReviewedBy(rs.getString("reviewed_by"));
            var reviewed = rs.getTimestamp("reviewed_at");
            a.setReviewedAt(reviewed != null ? reviewed.toLocalDateTime() : null);
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return a;
        }
    };

    public TriageAssessment save(TriageAssessment assessment) {
        String sql = """
            INSERT INTO triage_assessment (patient_id, queue_id, department_id, symptoms_text,
                                           recommended_dept_code, emergency_flag, acuity_score,
                                           disposition, recommended_tests, recommended_labs,
                                           reason, ai_used, model_version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            RETURNING assessment_id
            """;
        Long id = jdbc.queryForObject(sql, Long.class,
                assessment.getPatientId(), assessment.getQueueId(), assessment.getDepartmentId(),
                assessment.getSymptomsText(), assessment.getRecommendedDeptCode(),
                assessment.isEmergencyFlag(), assessment.getAcuityScore(),
                assessment.getDisposition(), assessment.getRecommendedTests(),
                assessment.getRecommendedLabs(), assessment.getReason(),
                assessment.isAiUsed(), assessment.getModelVersion());
        assessment.setAssessmentId(id);
        assessment.setCreatedAt(LocalDateTime.now());
        return assessment;
    }

    public void updateStaffReview(Long assessmentId, int staffAcuity, String staffDisposition, 
                                   String staffNotes, String reviewedBy) {
        String sql = """
            UPDATE triage_assessment 
            SET staff_acuity_score = ?, staff_disposition = ?, staff_notes = ?, 
                reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
            WHERE assessment_id = ?
            """;
        jdbc.update(sql, staffAcuity, staffDisposition, staffNotes, reviewedBy, assessmentId);
    }

    public Optional<TriageAssessment> findByQueueId(Long queueId) {
        String sql = "SELECT * FROM triage_assessment WHERE queue_id = ? ORDER BY created_at DESC LIMIT 1";
        List<TriageAssessment> results = jdbc.query(sql, MAPPER, queueId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TriageAssessment> findByPatientId(String patientId) {
        String sql = "SELECT * FROM triage_assessment WHERE patient_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, MAPPER, patientId);
    }

    public List<TriageAssessment> findHighAcuityUnreviewed(int limit) {
        String sql = """
            SELECT * FROM triage_assessment 
            WHERE acuity_score >= 4 AND reviewed_at IS NULL
            ORDER BY created_at DESC LIMIT ?
            """;
        return jdbc.query(sql, MAPPER, limit);
    }
}