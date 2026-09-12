package com.hospitalqueue.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures the demo users can log in with the documented password ("123456").
 * Runs after Flyway migrations and corrects the seeded BCrypt hashes.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "123456";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        // Doctors D001..D008
        for (int i = 1; i <= 8; i++) {
            fixDoctorHash("D00" + i, hash);
        }

        fixPatientHash("P202601010001", hash);
        fixStaffHash("S202601010001", hash);
        fixAdminHash("admin", hash);
    }

    private void fixDoctorHash(String doctorId, String hash) {
        String current = queryString("SELECT password_hash FROM doctor WHERE doctor_id = ?", doctorId);
        if (current != null && !passwordEncoder.matches(DEMO_PASSWORD, current)) {
            jdbcTemplate.update("UPDATE doctor SET password_hash = ? WHERE doctor_id = ?", hash, doctorId);
        }
    }

    private void fixPatientHash(String patientId, String hash) {
        String current = queryString("SELECT password_hash FROM patient WHERE patient_id = ?", patientId);
        if (current != null && !passwordEncoder.matches(DEMO_PASSWORD, current)) {
            jdbcTemplate.update("UPDATE patient SET password_hash = ? WHERE patient_id = ?", hash, patientId);
        }
    }

    private void fixStaffHash(String staffId, String hash) {
        String current = queryString("SELECT password_hash FROM staff WHERE staff_id = ?", staffId);
        if (current != null && !passwordEncoder.matches(DEMO_PASSWORD, current)) {
            jdbcTemplate.update("UPDATE staff SET password_hash = ? WHERE staff_id = ?", hash, staffId);
        }
    }

    private void fixAdminHash(String username, String hash) {
        String current = queryString("SELECT password_hash FROM admin_user WHERE username = ?", username);
        if (current != null && !passwordEncoder.matches(DEMO_PASSWORD, current)) {
            jdbcTemplate.update("UPDATE admin_user SET password_hash = ? WHERE username = ?", hash, username);
        }
    }

    private String queryString(String sql, Object... args) {
        List<String> list = jdbcTemplate.queryForList(sql, String.class, args);
        return list.isEmpty() ? null : list.get(0);
    }
}
