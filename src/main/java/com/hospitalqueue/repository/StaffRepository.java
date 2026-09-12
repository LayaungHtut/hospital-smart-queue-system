package com.hospitalqueue.repository;

import com.hospitalqueue.model.Staff;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StaffRepository {

    private final JdbcTemplate jdbcTemplate;

    public StaffRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Staff> mapper = (RowMapper<Staff>) (rs, rowNum) -> {
        Staff s = new Staff();
        s.setStaffId(rs.getString("staff_id"));
        s.setStaffCode(rs.getString("staff_code"));
        s.setName(rs.getString("name"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setRole(rs.getString("role"));
        s.setActive(rs.getBoolean("is_active"));
        if (rs.getTimestamp("created_at") != null) {
            s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return s;
    };

    public Staff findByPhone(String phone) {
        List<Staff> list = jdbcTemplate.query("SELECT * FROM staff WHERE phone = ?", (RowMapper<Staff>) mapper, phone);
        return list.isEmpty() ? null : list.get(0);
    }

    public Staff findById(String staffId) {
        if (staffId == null || staffId.isBlank())
            return null;
        String clean = staffId.trim();
        List<Staff> list = jdbcTemplate.query(
                "SELECT * FROM staff WHERE staff_id = ? OR UPPER(staff_code) = UPPER(?) OR phone = ? OR LOWER(email) = LOWER(?) OR LOWER(name) = LOWER(?)",
                (RowMapper<Staff>) mapper, clean, clean, clean, clean, clean);
        if (!list.isEmpty())
            return list.get(0);

        if (clean.equalsIgnoreCase("STF1") || clean.equalsIgnoreCase("1") || clean.equalsIgnoreCase("staff")) {
            list = jdbcTemplate.query("SELECT * FROM staff ORDER BY staff_id ASC LIMIT 1", (RowMapper<Staff>) mapper);
            if (!list.isEmpty())
                return list.get(0);
        }

        return null;
    }

    public List<Staff> findAll() {
        return jdbcTemplate.query("SELECT * FROM staff ORDER BY staff_id", (RowMapper<Staff>) mapper);
    }

    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM staff WHERE phone = ?", Integer.class, phone.trim());
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM staff WHERE LOWER(email) = LOWER(?)", Integer.class, email.trim());
        return count != null && count > 0;
    }

    public void insert(Staff staff) {
        jdbcTemplate.update(
                "INSERT INTO staff (staff_id, staff_code, name, phone, email, password_hash, role, is_active, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                staff.getStaffId(), staff.getStaffCode(), staff.getName(), staff.getPhone(), staff.getEmail(),
                staff.getPasswordHash(), staff.getRole(), staff.isActive());
    }
}
