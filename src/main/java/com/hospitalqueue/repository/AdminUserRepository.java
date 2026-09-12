package com.hospitalqueue.repository;

import com.hospitalqueue.model.AdminUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<AdminUser> mapper = (RowMapper<AdminUser>) (rs, rowNum) -> {
        AdminUser a = new AdminUser();
        a.setAdminId(rs.getString("admin_id"));
        a.setUsername(rs.getString("username"));
        a.setName(rs.getString("name"));
        a.setEmail(rs.getString("email"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setRole(rs.getString("role"));
        a.setActive(rs.getBoolean("is_active"));
        if (rs.getTimestamp("created_at") != null) {
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return a;
    };

    public AdminUser findByUsername(String username) {
        if (username == null || username.isBlank())
            return null;
        List<AdminUser> list = jdbcTemplate.query(
                "SELECT * FROM admin_user WHERE LOWER(username) = LOWER(?)", (RowMapper<AdminUser>) mapper,
                username.trim());
        return list.isEmpty() ? null : list.get(0);
    }

    public AdminUser findById(String adminId) {
        if (adminId == null || adminId.isBlank())
            return null;
        String clean = adminId.trim();
        List<AdminUser> list = jdbcTemplate.query(
                "SELECT * FROM admin_user WHERE admin_id = ? OR LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)",
                (RowMapper<AdminUser>) mapper, clean, clean, clean);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<AdminUser> findAll() {
        return jdbcTemplate.query("SELECT * FROM admin_user ORDER BY admin_id", (RowMapper<AdminUser>) mapper);
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM admin_user WHERE LOWER(email) = LOWER(?)", Integer.class, email.trim());
        return count != null && count > 0;
    }

    public void insert(AdminUser user) {
        jdbcTemplate.update(
                "INSERT INTO admin_user (admin_id, username, name, email, password_hash, role, is_active, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                user.getAdminId(), user.getUsername(), user.getName(), user.getEmail(), user.getPasswordHash(),
                user.getRole(), user.isActive());
    }
}
