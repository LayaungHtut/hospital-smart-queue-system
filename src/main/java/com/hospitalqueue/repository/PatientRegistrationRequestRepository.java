package com.hospitalqueue.repository;

import com.hospitalqueue.model.PatientRegistrationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class PatientRegistrationRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public PatientRegistrationRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<PatientRegistrationRequest> mapper = (RowMapper<PatientRegistrationRequest>) (ResultSet rs, int rowNum) -> {
        PatientRegistrationRequest r = new PatientRegistrationRequest();
        r.setRequestId(rs.getLong("request_id"));
        r.setName(rs.getString("name"));
        r.setPhone(rs.getString("phone"));
        r.setPasswordHash(rs.getString("password_hash"));
        r.setEmail(rs.getString("email"));
        r.setAddress(rs.getString("address"));
        if (rs.getDate("date_of_birth") != null) {
            r.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        }
        r.setGender(rs.getString("gender"));
        r.setStatus(rs.getString("status"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null)
            r.setCreatedAt(t.toLocalDateTime());
        t = rs.getTimestamp("reviewed_at");
        if (t != null)
            r.setReviewedAt(t.toLocalDateTime());
        r.setReviewedBy(rs.getString("reviewed_by"));
        r.setRejectionReason(rs.getString("rejection_reason"));
        return r;
    };

    public void insert(PatientRegistrationRequest req) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO patient_registration_request (name, phone, password_hash, email, address, date_of_birth, gender, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING request_id",
                Long.class,
                req.getName(), req.getPhone(), req.getPasswordHash(), req.getEmail(), req.getAddress(),
                req.getDateOfBirth(), req.getGender(), req.getStatus());
        if (id != null) {
            req.setRequestId(id);
        }
    }

    public PatientRegistrationRequest findLatestByPhone(String phone) {
        List<PatientRegistrationRequest> list = jdbcTemplate.query(
                "SELECT * FROM patient_registration_request WHERE phone = ? ORDER BY request_id DESC LIMIT 1",
                (RowMapper<PatientRegistrationRequest>) mapper, phone);
        return list.isEmpty() ? null : list.get(0);
    }

    public PatientRegistrationRequest findById(long requestId) {
        List<PatientRegistrationRequest> list = jdbcTemplate.query(
                "SELECT * FROM patient_registration_request WHERE request_id = ?",
                (RowMapper<PatientRegistrationRequest>) mapper, requestId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<PatientRegistrationRequest> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM patient_registration_request ORDER BY request_id DESC",
                (RowMapper<PatientRegistrationRequest>) mapper);
    }

    public List<PatientRegistrationRequest> findPending() {
        return jdbcTemplate.query(
                "SELECT * FROM patient_registration_request WHERE status = 'PENDING' ORDER BY request_id DESC",
                (RowMapper<PatientRegistrationRequest>) mapper);
    }

    public void updateStatus(long requestId, String status, String reviewedBy, String reason) {
        jdbcTemplate.update(
                "UPDATE patient_registration_request SET status = ?, reviewed_by = ?, rejection_reason = ?, reviewed_at = CURRENT_TIMESTAMP "
                        + "WHERE request_id = ?",
                status, reviewedBy, reason, requestId);
    }
}
