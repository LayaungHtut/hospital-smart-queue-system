package com.hospitalqueue.controller;

import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.IDGenerator;
import com.hospitalqueue.util.SessionUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AdminManageController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final QueueService queueService;

    public AdminManageController(DoctorRepository doctorRepository,
                                 DepartmentRepository departmentRepository,
                                 PasswordEncoder passwordEncoder,
                                 JdbcTemplate jdbcTemplate,
                                 QueueService queueService) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.queueService = queueService;
    }

    private boolean isAdmin(HttpSession session) {
        return SessionUtil.getAdmin(session) != null;
    }

    // ---------- Doctors ----------

    @GetMapping("/admin/doctors")
    public String doctors(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin", SessionUtil.getAdmin(session));
        model.addAttribute("doctors", queueService.enrichAvailability(doctorRepository.findAll()));
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/doctors";
    }

    @PostMapping("/admin/doctors/toggle")
    public String toggleAvailability(@RequestParam String doctorId, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            ra.addFlashAttribute("error", "Doctor not found.");
            return "redirect:/admin/doctors";
        }
        doctorRepository.setAvailability(doctorId, !doctor.isAvailable());
        ra.addFlashAttribute("message", "Availability updated for " + doctor.getName() + ".");
        return "redirect:/admin/doctors";
    }

    @GetMapping("/admin/doctors/form")
    public String doctorForm(@RequestParam(required = false) String id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin", SessionUtil.getAdmin(session));
        model.addAttribute("departments", departmentRepository.findAll());
        if (id != null && !id.isBlank()) {
            model.addAttribute("doctor", doctorRepository.findById(id));
        }
        return "admin/doctorForm";
    }

    @PostMapping("/admin/doctors/save")
    public String saveDoctor(@RequestParam(required = false) String id,
                             @RequestParam String doctorCode,
                             @RequestParam String name,
                             @RequestParam int departmentId,
                             @RequestParam(required = false) String specialization,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false) String password,
                             @RequestParam(defaultValue = "20") long maxQueueSize,
                             @RequestParam(defaultValue = "15") long averageConsultationMinutes,
                             @RequestParam(required = false) String queueOpenTime,
                             @RequestParam(required = false) String queueCloseTime,
                             @RequestParam(required = false) boolean available,
                             HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        Doctor doctor = new Doctor();
        if (id != null && !id.isBlank()) {
            Doctor existing = doctorRepository.findById(id);
            if (existing != null) {
                doctor = existing;
            }
        } else {
            doctor.setDoctorId(IDGenerator.generateDoctorId(doctorRepository.findAll().size()));
        }
        doctor.setDoctorCode(doctorCode);
        doctor.setName(name);
        doctor.setDepartmentId(departmentId);
        doctor.setSpecialization(specialization);
        doctor.setPhone(phone);
        doctor.setEmail(email);
        doctor.setMaxQueueSize(maxQueueSize);
        doctor.setAverageConsultationMinutes(averageConsultationMinutes);
        if (queueOpenTime != null && !queueOpenTime.isBlank()) {
            doctor.setQueueOpenTime(java.time.LocalTime.parse(queueOpenTime));
        }
        if (queueCloseTime != null && !queueCloseTime.isBlank()) {
            doctor.setQueueCloseTime(java.time.LocalTime.parse(queueCloseTime));
        }
        doctor.setActive(true);
        doctor.setAvailable(available);

        if (id == null || id.isBlank()) {
            doctor.setPasswordHash(passwordEncoder.encode(password != null && !password.isBlank() ? password : "123456"));
            doctorRepository.insert(doctor);
        } else {
            doctorRepository.update(doctor);
        }
        ra.addFlashAttribute("message", "Doctor saved successfully.");
        return "redirect:/admin/doctors";
    }

    // ---------- Departments ----------

    @GetMapping("/admin/departments")
    public String departments(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin", SessionUtil.getAdmin(session));
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/departments";
    }

    @PostMapping("/admin/departments/save")
    public String saveDepartment(@RequestParam(required = false) Integer id,
                                 @RequestParam String departmentCode,
                                 @RequestParam String departmentName,
                                 HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        Department department = new Department();
        department.setDepartmentCode(departmentCode);
        department.setDepartmentName(departmentName);
        department.setActive(true);
        if (id != null) {
            department.setDepartmentId(id);
            departmentRepository.update(department);
        } else {
            departmentRepository.insert(department);
        }
        ra.addFlashAttribute("message", "Department saved successfully.");
        return "redirect:/admin/departments";
    }

    // ---------- Settings ----------

    @GetMapping("/admin/settings")
    public String settings(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("admin", SessionUtil.getAdmin(session));
        model.addAttribute("settings", loadSettings());
        return "admin/settings";
    }

    @PostMapping("/admin/settings/save")
    public String saveSettings(@RequestParam Map<String, String> params, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("setting_")) {
                String settingKey = key.substring("setting_".length());
                jdbcTemplate.update(
                        "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP",
                        settingKey, entry.getValue());
            }
        }
        ra.addFlashAttribute("message", "Settings saved successfully.");
        return "redirect:/admin/settings";
    }

    private Map<String, String> loadSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT setting_key, setting_value FROM system_setting ORDER BY setting_key",
                rs -> {
                    settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
                });
        return settings;
    }
}
