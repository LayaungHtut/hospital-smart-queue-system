package com.hospitalqueue.controller;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.AuthService;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DoctorAuthController {

    private final AuthService authService;
    private final QueueService queueService;
    private final QueueRepository queueRepository;

    public DoctorAuthController(AuthService authService, QueueService queueService, QueueRepository queueRepository) {
        this.authService = authService;
        this.queueService = queueService;
        this.queueRepository = queueRepository;
    }

    @GetMapping("/doctor/login")
    public String loginPage() {
        return "doctor/login";
    }

    @PostMapping("/doctor/login")
    public String login(@RequestParam String doctorId, @RequestParam String password,
                        HttpSession session, Model model) {
        Doctor doctor = authService.loginDoctor(doctorId, password);
        if (doctor == null) {
            model.addAttribute("error", "Invalid doctor ID or password.");
            return "doctor/login";
        }
        SessionUtil.setDoctor(session, doctor);
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/doctor/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        model.addAttribute("doctor", doctor);
        model.addAttribute("called", queueService.called(doctor.getDoctorId()));
        model.addAttribute("serving", queueService.serving(doctor.getDoctorId()));
        model.addAttribute("waiting", queueService.waitingForDoctor(doctor.getDoctorId()));
        model.addAttribute("waitingCount", queueRepository.countWaitingByDoctor(doctor.getDoctorId()));
        model.addAttribute("completedToday", queueRepository.countCompletedTodayForDoctor(doctor.getDoctorId()));
        model.addAttribute("cancelledToday", queueRepository.countCancelledTodayForDoctor(doctor.getDoctorId()));
        return "doctor/dashboard";
    }

    @GetMapping("/doctor/logout")
    public String logout(HttpSession session) {
        SessionUtil.logout(session);
        return "redirect:/";
    }
}
