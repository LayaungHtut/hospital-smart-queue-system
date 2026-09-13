package com.hospitalqueue.repository;

import com.hospitalqueue.model.Appointment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final @NonNull RowMapper<Appointment> mapper = (RowMapper<Appointment>) (ResultSet rs, int rowNum) -> {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getLong("appointment_id"));
        a.setPatientId(rs.getString("patient_id"));
        a.setDoctorId(rs.getString("doctor_id"));
        a.setDepartmentId(rs.getInt("department_id"));
        if (rs.getDate("appointment_date") != null) {
            a.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        }
        Time t = rs.getTime("appointment_time");
        if (t != null) {
            a.setAppointmentTime(t.toLocalTime());
        }
        a.setStatus(rs.getString("status"));
        long qid = rs.getLong("queue_id");
        a.setQueueId(rs.wasNull() ? null : qid);
        a.setNotes(rs.getString("notes"));
        try {
            a.setDepartmentCode(rs.getString("department_code"));
            a.setDepartmentName(rs.getString("department_name"));
        } catch (Exception ignored) {
            // Column may not exist in all queries
        }
        return a;
    };

    public Appointment findTodaysAppointment(String patientId) {
        List<Appointment> list = jdbcTemplate.query(
                "SELECT * FROM appointment WHERE patient_id = ? AND appointment_date = ? AND status = 'SCHEDULED' "
                        + "ORDER BY appointment_id DESC LIMIT 1",
                (RowMapper<Appointment>) mapper, patientId, LocalDate.now());
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Appointment> findByPatient(String patientId) {
        return jdbcTemplate.query(
                "SELECT * FROM appointment WHERE patient_id = ? ORDER BY appointment_date DESC",
                (RowMapper<Appointment>) mapper, patientId);
    }

    public List<Appointment> findTodayAll() {
        return jdbcTemplate.query(
                "SELECT * FROM appointment WHERE appointment_date = ? ORDER BY appointment_time",
                (RowMapper<Appointment>) mapper, LocalDate.now());
    }

    public List<Appointment> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM appointment ORDER BY appointment_date DESC, appointment_time DESC",
                (RowMapper<Appointment>) mapper);
    }

    public List<Appointment> findByPatientId(String patientId) {
        return findByPatient(patientId);
    }

    public List<Appointment> findByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM appointment WHERE doctor_id = ? ORDER BY appointment_date DESC, appointment_time DESC",
                (RowMapper<Appointment>) mapper, doctorId);
    }

    public List<Appointment> findTodayByDoctor(String doctorId) {
        return jdbcTemplate.query(
                "SELECT * FROM appointment WHERE doctor_id = ? AND appointment_date = ? ORDER BY appointment_time",
                (RowMapper<Appointment>) mapper, doctorId, LocalDate.now());
    }

    public void insert(Appointment a) {
        jdbcTemplate.update(
                "INSERT INTO appointment (patient_id, doctor_id, department_id, appointment_date, appointment_time, status, notes) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                a.getPatientId(), a.getDoctorId(), a.getDepartmentId(), a.getAppointmentDate(),
                a.getAppointmentTime(), a.getStatus(), a.getNotes());
    }

    public void updateStatus(long appointmentId, String status) {
        jdbcTemplate.update("UPDATE appointment SET status = ? WHERE appointment_id = ?", status, appointmentId);
    }

    public void updateStatus(long appointmentId, String status, Long queueId) {
        jdbcTemplate.update("UPDATE appointment SET status = ?, queue_id = ? WHERE appointment_id = ?",
                status, queueId, appointmentId);
    }

    public Appointment findById(Long appointmentId) {
        List<Appointment> list = jdbcTemplate.query(
                "SELECT * FROM appointment WHERE appointment_id = ?",
                (RowMapper<Appointment>) mapper, appointmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    public long countByPatientId(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE patient_id = ?", Long.class, patientId);
    }

    public long countNoShowsByPatientId(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE patient_id = ? AND status = 'NOSHOW'", Long.class, patientId);
    }

    public long countCancellationsByPatientId(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE patient_id = ? AND status = 'CANCELLED'", Long.class, patientId);
    }

    public Integer daysSinceLastVisit(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT (CURRENT_DATE - MAX(appointment_date))::int " +
                "FROM appointment WHERE patient_id = ? AND status IN ('COMPLETED', 'NOSHOW')", Integer.class, patientId);
    }

    public Integer averageLeadTime(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT AVG(appointment_date - CURRENT_DATE)::int " +
                "FROM appointment WHERE patient_id = ? AND status = 'SCHEDULED'", Integer.class, patientId);
    }

    /**
     * All patient-history aggregates used by the ML feature builders in a single
     * round trip, instead of 5 separate queries (countByPatientId,
     * countNoShowsByPatientId, countCancellationsByPatientId, daysSinceLastVisit,
     * averageLeadTime).
     */
    public PatientHistoryStats getPatientHistoryStats(String patientId) {
        return jdbcTemplate.queryForObject(
                "SELECT " +
                "  COUNT(*) AS total, " +
                "  COUNT(*) FILTER (WHERE status = 'NOSHOW') AS noshows, " +
                "  COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancellations, " +
                "  (SELECT (CURRENT_DATE - MAX(appointment_date))::int FROM appointment " +
                "     WHERE patient_id = ? AND status IN ('COMPLETED', 'NOSHOW')) AS days_since_last, " +
                "  (SELECT AVG(appointment_date - CURRENT_DATE)::int FROM appointment " +
                "     WHERE patient_id = ? AND status = 'SCHEDULED') AS avg_lead_time " +
                "FROM appointment WHERE patient_id = ?",
                (rs, rowNum) -> new PatientHistoryStats(
                        rs.getInt("total"),
                        rs.getInt("noshows"),
                        rs.getInt("cancellations"),
                        rs.getObject("days_since_last") != null ? rs.getInt("days_since_last") : null,
                        rs.getObject("avg_lead_time") != null ? rs.getInt("avg_lead_time") : null),
                patientId, patientId, patientId);
    }

    public record PatientHistoryStats(int totalAppointments, int pastNoshows, int pastCancellations,
                                       Integer daysSinceLastVisit, Integer averageLeadTime) {}
}
