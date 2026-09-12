package com.hospitalqueue.controller;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class AdminReportsController {

    private final PatientRepository patientRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminReportsController(PatientRepository patientRepository, JdbcTemplate jdbcTemplate) {
        this.patientRepository = patientRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/reports")
    public String reports(HttpSession session, Model model) {
        AdminUser admin = SessionUtil.getAdmin(session);
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("admin", admin);
        model.addAttribute("statusBreakdown", statusBreakdown());
        model.addAttribute("queuesByDay", queuesByDay());
        return "admin/reports";
    }

    @GetMapping("/admin/patients")
    public String patients(@RequestParam(required = false) String search, HttpSession session, Model model) {
        AdminUser admin = SessionUtil.getAdmin(session);
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("admin", admin);
        if (search != null && !search.isBlank()) {
            model.addAttribute("patients", patientRepository.search(search));
        } else {
            model.addAttribute("patients", patientRepository.findAll());
        }
        model.addAttribute("search", search);
        return "admin/patients";
    }

    private List<Map<String, Object>> statusBreakdown() {
        return jdbcTemplate.query(
                "SELECT status, COUNT(*) AS total FROM queue GROUP BY status ORDER BY total DESC",
                (rs, rowNum) -> Map.of(
                        "status", rs.getString("status"),
                        "total", rs.getLong("total")));
    }

    private List<Map<String, Object>> queuesByDay() {
        LocalDate start = LocalDate.now().minusDays(6);
        List<Map<String, Object>> rows = new ArrayList<>();
        jdbcTemplate.query(
                "SELECT created_at::DATE AS day, COUNT(*) AS total FROM queue WHERE created_at::DATE >= ? GROUP BY created_at::DATE ORDER BY day",
                rs -> {
                    rows.add(Map.of("day", rs.getDate("day").toLocalDate(), "total", rs.getLong("total")));
                },
                java.sql.Date.valueOf(start));
        return rows;
    }
}
