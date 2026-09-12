package com.hospitalqueue.repository;

import com.hospitalqueue.model.Doctor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Time;
import java.util.List;

@Repository
public class DoctorRepository {

    private final JdbcTemplate jdbcTemplate;

    public DoctorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Doctor> mapper = (RowMapper<Doctor>) (ResultSet rs, int rowNum) -> {
        Doctor d = new Doctor();
        d.setDoctorId(rs.getString("doctor_id"));
        d.setDoctorCode(rs.getString("doctor_code"));
        d.setName(rs.getString("name"));
        d.setDepartmentId(rs.getInt("department_id"));
        d.setSpecialization(rs.getString("specialization"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setPasswordHash(rs.getString("password_hash"));
        d.setMaxQueueSize(rs.getLong("max_queue_size"));
        Time open = rs.getTime("queue_open_time");
        if (open != null) {
            d.setQueueOpenTime(open.toLocalTime());
        }
        Time close = rs.getTime("queue_close_time");
        if (close != null) {
            d.setQueueCloseTime(close.toLocalTime());
        }
        d.setAverageConsultationMinutes(rs.getLong("average_consultation_minutes"));
        try {
            d.setQualification(rs.getString("qualification"));
            d.setYearsOfExperience(rs.getInt("years_of_experience"));
        } catch (Exception ignored) {
            // fallback if columns not yet migrated
        }
        d.setAvailable(rs.getBoolean("is_available"));
        d.setActive(rs.getBoolean("is_active"));
        return d;
    };

    public Doctor findById(String doctorId) {
        if (doctorId == null || doctorId.isBlank())
            return null;
        String clean = doctorId.trim();

        // 1. Exact match on doctor_id
        List<Doctor> list = jdbcTemplate.query(
                "SELECT * FROM doctor WHERE doctor_id = ?", (RowMapper<Doctor>) mapper, clean);
        if (!list.isEmpty())
            return list.get(0);

        // 2. Numeric match: "1" -> "D001", "2" -> "D002", etc.
        if (clean.matches("\\d+")) {
            String formatted = String.format("D%03d", Integer.parseInt(clean));
            list = jdbcTemplate.query(
                    "SELECT * FROM doctor WHERE doctor_id = ?", (RowMapper<Doctor>) mapper, formatted);
            if (!list.isEmpty())
                return list.get(0);
        }

        // 3. Short code: "D1" -> "D001", "DOC1" -> "D001"
        if (clean.toUpperCase().startsWith("D") && clean.substring(1).matches("\\d+")) {
            String formatted = String.format("D%03d", Integer.parseInt(clean.substring(1)));
            list = jdbcTemplate.query(
                    "SELECT * FROM doctor WHERE doctor_id = ?", (RowMapper<Doctor>) mapper, formatted);
            if (!list.isEmpty())
                return list.get(0);
        }

        // 4. By Doctor Code (e.g. "CAR1", "GEN1", "D101")
        Doctor byCode = findByCode(clean.toUpperCase());
        if (byCode != null)
            return byCode;

        // 5. By Phone or Email
        list = jdbcTemplate.query(
                "SELECT * FROM doctor WHERE LOWER(email) = LOWER(?) OR phone = ?", (RowMapper<Doctor>) mapper, clean,
                clean);
        if (!list.isEmpty())
            return list.get(0);

        // 6. By Name match
        list = jdbcTemplate.query(
                "SELECT * FROM doctor WHERE LOWER(name) = LOWER(?) OR LOWER(name) = LOWER(?) ORDER BY doctor_id LIMIT 1",
                (RowMapper<Doctor>) mapper, clean, "Dr. " + clean);
        if (!list.isEmpty())
            return list.get(0);

        return null;
    }

    public Doctor findByCode(String doctorCode) {
        if (doctorCode == null || doctorCode.isBlank())
            return null;
        List<Doctor> list = jdbcTemplate.query(
                "SELECT * FROM doctor WHERE UPPER(doctor_code) = ?", (RowMapper<Doctor>) mapper,
                doctorCode.trim().toUpperCase());
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Doctor> findByDepartment(int departmentId) {
        return jdbcTemplate.query(
                "SELECT * FROM doctor WHERE department_id = ? AND is_active = TRUE ORDER BY name",
                (RowMapper<Doctor>) mapper, departmentId);
    }

    public List<Doctor> findAll() {
        return jdbcTemplate.query("SELECT * FROM doctor ORDER BY name", (RowMapper<Doctor>) mapper);
    }

    public boolean existsByPhone(String phone, String excludeDoctorId) {
        if (phone == null || phone.isBlank())
            return false;
        String sql = excludeDoctorId != null && !excludeDoctorId.isBlank()
                ? "SELECT COUNT(*) FROM doctor WHERE phone = ? AND doctor_id <> ?"
                : "SELECT COUNT(*) FROM doctor WHERE phone = ?";
        Integer count = excludeDoctorId != null && !excludeDoctorId.isBlank()
                ? jdbcTemplate.queryForObject(sql, Integer.class, phone.trim(), excludeDoctorId.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, phone.trim());
        return count != null && count > 0;
    }

    public void insert(Doctor doctor) {
        jdbcTemplate.update(
                "INSERT INTO doctor (doctor_id, doctor_code, name, department_id, specialization, phone, email, password_hash, "
                        + "max_queue_size, queue_open_time, queue_close_time, average_consultation_minutes, qualification, years_of_experience, is_available, is_active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                doctor.getDoctorId(), doctor.getDoctorCode(), doctor.getName(), doctor.getDepartmentId(),
                doctor.getSpecialization(), doctor.getPhone(), doctor.getEmail(), doctor.getPasswordHash(),
                doctor.getMaxQueueSize(), doctor.getQueueOpenTime(), doctor.getQueueCloseTime(),
                doctor.getAverageConsultationMinutes(), doctor.getQualification(), doctor.getYearsOfExperience(),
                doctor.isAvailable(), doctor.isActive());
    }

    public void update(Doctor doctor) {
        jdbcTemplate.update(
                "UPDATE doctor SET doctor_code = ?, name = ?, department_id = ?, specialization = ?, phone = ?, email = ?, "
                        + "max_queue_size = ?, queue_open_time = ?, queue_close_time = ?, average_consultation_minutes = ?, "
                        + "qualification = ?, years_of_experience = ?, is_available = ?, is_active = ? WHERE doctor_id = ?",
                doctor.getDoctorCode(), doctor.getName(), doctor.getDepartmentId(), doctor.getSpecialization(),
                doctor.getPhone(), doctor.getEmail(), doctor.getMaxQueueSize(), doctor.getQueueOpenTime(),
                doctor.getQueueCloseTime(), doctor.getAverageConsultationMinutes(), doctor.getQualification(),
                doctor.getYearsOfExperience(), doctor.isAvailable(), doctor.isActive(), doctor.getDoctorId());
    }

    public void setAvailability(String doctorId, boolean available) {
        jdbcTemplate.update("UPDATE doctor SET is_available = ? WHERE doctor_id = ?", available, doctorId);
    }
}
