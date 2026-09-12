package com.hospitalqueue.repository;

import com.hospitalqueue.model.Priority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PriorityRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriorityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Priority> mapper = (RowMapper<Priority>) (rs, rowNum) -> {
        Priority p = new Priority();
        p.setPriorityId(rs.getInt("priority_id"));
        p.setPriorityCode(rs.getString("priority_code"));
        p.setPriorityName(rs.getString("priority_name"));
        p.setSortOrder(rs.getInt("sort_order"));
        p.setWeight(rs.getBigDecimal("weight"));
        p.setActive(rs.getBoolean("is_active"));
        return p;
    };

    public List<Priority> findAllActive() {
        return jdbcTemplate.query("SELECT * FROM priority WHERE is_active = TRUE ORDER BY sort_order", (RowMapper<Priority>) mapper);
    }

    public Priority findByCode(String code) {
        List<Priority> list = jdbcTemplate.query(
                "SELECT * FROM priority WHERE priority_code = ?", (RowMapper<Priority>) mapper, code);
        return list.isEmpty() ? null : list.get(0);
    }
}
