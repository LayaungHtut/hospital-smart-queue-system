package com.hospitalqueue.repository;

import com.hospitalqueue.model.DoctorSchedule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Time;
import java.util.List;

@Repository
public class DoctorScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public DoctorScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<DoctorSchedule> mapper = (RowMapper<DoctorSchedule>) (ResultSet rs, int rowNum) -> {
        DoctorSchedule s = new DoctorSchedule();
        s.setScheduleId(rs.getInt("schedule_id"));
        s.setDoctorId(rs.getString("doctor_id"));
        s.setDayOfWeek(rs.getString("day_of_week"));
        s.setStartTime(rs.getTime("start_time") != null ? rs.getTime("start_time").toLocalTime() : null);
        s.setEndTime(rs.getTime("end_time") != null ? rs.getTime("end_time").toLocalTime() : null);
        Time breakStart = rs.getTime("break_start");
        s.setBreakStart(breakStart != null ? breakStart.toLocalTime() : null);
        Time breakEnd = rs.getTime("break_end");
        s.setBreakEnd(breakEnd != null ? breakEnd.toLocalTime() : null);
        s.setRoom(rs.getString("room"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    };

    public List<DoctorSchedule> findAllActive() {
        return jdbcTemplate.query(
                "SELECT * FROM doctor_schedule WHERE is_active = TRUE ORDER BY doctor_id, schedule_id",
                (RowMapper<DoctorSchedule>) mapper);
    }

    public List<DoctorSchedule> findByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM doctor_schedule WHERE doctor_id = ? AND is_active = TRUE ORDER BY schedule_id",
                (RowMapper<DoctorSchedule>) mapper, doctorId);
    }

    public void insert(DoctorSchedule s) {
        jdbcTemplate.update(
                "INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, break_start, break_end, room) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.getDoctorId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime(),
                s.getBreakStart(), s.getBreakEnd(), s.getRoom());
    }

    /** Soft-deletes a schedule row. Returns true if a row existed and was deactivated. */
    public boolean deactivate(int scheduleId) {
        int updated = jdbcTemplate.update(
                "UPDATE doctor_schedule SET is_active = FALSE WHERE schedule_id = ? AND is_active = TRUE",
                scheduleId);
        return updated > 0;
    }
}
