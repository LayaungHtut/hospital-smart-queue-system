package com.hospitalqueue.repository;

import com.hospitalqueue.model.Patient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PatientRepository {

    private final JdbcTemplate jdbcTemplate;

    public PatientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Patient> mapper = new RowMapper<Patient>() {
        @Override
        public Patient mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            Patient p = new Patient();
            p.setPatientId(rs.getString("patient_id"));
            p.setName(rs.getString("name"));
            p.setPhone(rs.getString("phone"));
            p.setPasswordHash(rs.getString("password_hash"));
            p.setEmail(rs.getString("email"));
            p.setAddress(rs.getString("address"));
            if (rs.getDate("date_of_birth") != null) {
                p.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
            }
            p.setGender(rs.getString("gender"));
            p.setNewPatient(rs.getBoolean("is_new_patient"));
            if (rs.getTimestamp("registered_at") != null) {
                p.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
            }
            return p;
        }
    };

    public Patient findByPhone(String phone) {
        if (phone == null || phone.isBlank())
            return null;
        List<Patient> list = jdbcTemplate.query(
                "SELECT * FROM patient WHERE phone = ? ORDER BY registered_at ASC", (RowMapper<Patient>) mapper,
                phone.trim());
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Patient> findAllByPhone(String phone) {
        if (phone == null || phone.isBlank())
            return List.of();
        return jdbcTemplate.query(
                "SELECT * FROM patient WHERE phone = ? ORDER BY registered_at ASC", (RowMapper<Patient>) mapper,
                phone.trim());
    }

    public int countByPhone(String phone) {
        if (phone == null || phone.isBlank())
            return 0;
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM patient WHERE phone = ?", Integer.class, phone.trim());
        return c == null ? 0 : c;
    }

    public Patient findById(String patientId) {
        if (patientId == null || patientId.isBlank())
            return null;
        String clean = patientId.trim();
        List<Patient> list = jdbcTemplate.query(
                "SELECT * FROM patient WHERE patient_id = ?", (RowMapper<Patient>) mapper, clean);
        if (!list.isEmpty())
            return list.get(0);

        // Fallback 1: try finding by phone
        Patient byPhone = findByPhone(clean);
        if (byPhone != null)
            return byPhone;

        // Fallback 2: try finding by email
        list = jdbcTemplate.query(
                "SELECT * FROM patient WHERE LOWER(email) = LOWER(?)", (RowMapper<Patient>) mapper, clean);
        if (!list.isEmpty())
            return list.get(0);

        // Fallback 3: try finding by name
        list = jdbcTemplate.query(
                "SELECT * FROM patient WHERE LOWER(name) = LOWER(?) ORDER BY registered_at DESC LIMIT 1",
                (RowMapper<Patient>) mapper, clean);
        if (!list.isEmpty())
            return list.get(0);

        // Fallback 4: numeric or shorthand like "1", "P1", "P001", "P2026..."
        list = jdbcTemplate.query(
                "SELECT * FROM patient WHERE patient_id LIKE ? ORDER BY registered_at DESC LIMIT 1",
                (RowMapper<Patient>) mapper, "%" + clean.replaceAll("[^0-9]", ""));
        if (!list.isEmpty())
            return list.get(0);

        return null;
    }

    public void insert(Patient patient) {
        jdbcTemplate.update(
                "INSERT INTO patient (patient_id, name, phone, password_hash, email, address, date_of_birth, gender, is_new_patient, registered_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                patient.getPatientId(), patient.getName(), patient.getPhone(), patient.getPasswordHash(),
                patient.getEmail(), patient.getAddress(), patient.getDateOfBirth(), patient.getGender(),
                patient.isNewPatient());
    }

    public void update(Patient patient) {
        jdbcTemplate.update(
                "UPDATE patient SET name = ?, email = ?, address = ?, date_of_birth = ?, gender = ?, is_new_patient = ? "
                        + "WHERE patient_id = ?",
                patient.getName(), patient.getEmail(), patient.getAddress(), patient.getDateOfBirth(),
                patient.getGender(), patient.isNewPatient(), patient.getPatientId());
    }

    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patient", Integer.class);
        return c == null ? 0 : c;
    }

    public List<Patient> findAll() {
        return jdbcTemplate.query("SELECT * FROM patient ORDER BY registered_at DESC", (RowMapper<Patient>) mapper);
    }

    public List<Patient> findByIds(java.util.Collection<String> ids) {
        if (ids == null || ids.isEmpty())
            return java.util.Collections.emptyList();
        String inSql = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.query(
                "SELECT * FROM patient WHERE patient_id IN (" + inSql + ")",
                (RowMapper<Patient>) mapper, ids.toArray());
    }

    public List<Patient> search(String term) {
        String like = "%" + term + "%";
        return jdbcTemplate.query(
                "SELECT * FROM patient WHERE name ILIKE ? OR phone ILIKE ? OR email ILIKE ? ORDER BY registered_at DESC",
                (RowMapper<Patient>) mapper, like, like, like);
    }
}
