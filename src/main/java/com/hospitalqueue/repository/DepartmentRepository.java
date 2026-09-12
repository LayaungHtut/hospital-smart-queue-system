package com.hospitalqueue.repository;

import com.hospitalqueue.model.Department;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

@Repository
public class DepartmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Department> mapper = (RowMapper<Department>) (ResultSet rs, int rowNum) -> {
        Department d = new Department();
        d.setDepartmentId(rs.getInt("department_id"));
        d.setDepartmentCode(rs.getString("department_code"));
        d.setDepartmentName(rs.getString("department_name"));
        d.setActive(rs.getBoolean("is_active"));
        return d;
    };

    public Department findById(int departmentId) {
        List<Department> list = jdbcTemplate.query(
                "SELECT * FROM department WHERE department_id = ?", (RowMapper<Department>) mapper, departmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Department> findAll() {
        return jdbcTemplate.query("SELECT * FROM department WHERE is_active = TRUE ORDER BY department_name", (RowMapper<Department>) mapper);
    }

    public Department findByCode(String code) {
        List<Department> list = jdbcTemplate.query(
                "SELECT * FROM department WHERE department_code = ?", (RowMapper<Department>) mapper, code);
        return list.isEmpty() ? null : list.get(0);
    }

    public Department findByName(String name) {
        List<Department> list = jdbcTemplate.query(
                "SELECT * FROM department WHERE department_name = ?", (RowMapper<Department>) mapper, name);
        return list.isEmpty() ? null : list.get(0);
    }

    public void insert(Department department) {
        jdbcTemplate.update(
                "INSERT INTO department (department_code, department_name, is_active) VALUES (?, ?, ?)",
                department.getDepartmentCode(), department.getDepartmentName(), department.isActive());
    }

    public void update(Department department) {
        jdbcTemplate.update(
                "UPDATE department SET department_code = ?, department_name = ?, is_active = ? WHERE department_id = ?",
                department.getDepartmentCode(), department.getDepartmentName(), department.isActive(),
                department.getDepartmentId());
    }
}
