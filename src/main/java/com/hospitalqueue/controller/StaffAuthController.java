package com.hospitalqueue.controller;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.repository.StaffRepository;
import com.hospitalqueue.service.AuthService;
import com.hospitalqueue.util.SessionUtil;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaffAuthController {

    private final AuthService authService;
    private final StaffRepository staffRepository;
    private final QueueRepository queueRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public StaffAuthController(AuthService authService, StaffRepository staffRepository,
                               QueueRepository queueRepository, DoctorRepository doctorRepository,
                               PatientRepository patientRepository) {
        this.authService = authService;
        this.staffRepository = staffRepository;
        this.queueRepository = queueRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/staff/login")
    public String loginPage() {
        return "staff/login";
    }

    @PostMapping("/staff/login")
    public String login(@RequestParam String phone, @RequestParam String password,
                        HttpSession session, Model model) {
        Staff staff = authService.loginStaff(phone, password);
        if (staff == null) {
            model.addAttribute("error", "Invalid staff credentials.");
            return "staff/login";
        }
        SessionUtil.setStaff(session, staffRepository.findById(staff.getStaffId()));
        return "redirect:/staff/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Staff staff = SessionUtil.getStaff(session);
        if (staff == null) {
            return "redirect:/staff/login";
        }
        model.addAttribute("staff", staff);
        List<Queue> pending = queueRepository.findPendingEmergencies();
        enrich(pending);
        model.addAttribute("pendingEmergencies", pending);
        model.addAttribute("waitingCount", queueRepository.countWaiting());
        model.addAttribute("servingCount", queueRepository.countServing());
        model.addAttribute("queuesToday", queueRepository.countQueuesToday());
        return "staff/dashboard";
    }

    private void enrich(List<Queue> queues) {
        for (Queue q : queues) {
            Doctor d = doctorRepository.findById(q.getDoctorId());
            if (d != null) {
                q.setDoctorName(d.getName());
            }
            if (q.getPatientId() != null && patientRepository.findById(q.getPatientId()) != null) {
                q.setPatientName(patientRepository.findById(q.getPatientId()).getName());
            }
        }
    }

    @GetMapping("/staff/logout")
    public String logout(HttpSession session) {
        SessionUtil.logout(session);
        return "redirect:/";
    }
}
