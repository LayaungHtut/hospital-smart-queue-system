package com.hospitalqueue.controller;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.repository.AdminUserRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.AuthService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAuthController {

    private final AuthService authService;
    private final AdminUserRepository adminUserRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final QueueRepository queueRepository;

    public AdminAuthController(AuthService authService, AdminUserRepository adminUserRepository,
                               PatientRepository patientRepository, DoctorRepository doctorRepository,
                               QueueRepository queueRepository) {
        this.authService = authService;
        this.adminUserRepository = adminUserRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.queueRepository = queueRepository;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, Model model) {
        AdminUser admin = authService.loginAdmin(username, password);
        if (admin == null) {
            model.addAttribute("error", "Invalid username or password.");
            return "admin/login";
        }
        SessionUtil.setAdmin(session, adminUserRepository.findByUsername(admin.getUsername()));
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpSession session, Model model) {
        AdminUser admin = SessionUtil.getAdmin(session);
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("admin", admin);
        model.addAttribute("totalPatients", patientRepository.count());
        model.addAttribute("totalDoctors", doctorRepository.findAll().size());
        model.addAttribute("queuesToday", queueRepository.countQueuesToday());
        model.addAttribute("waitingNow", queueRepository.countWaiting());
        model.addAttribute("servingNow", queueRepository.countServing());
        model.addAttribute("recentQueues", queueRepository.findRecent(10));
        return "admin/dashboard";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        SessionUtil.logout(session);
        return "redirect:/";
    }
}
