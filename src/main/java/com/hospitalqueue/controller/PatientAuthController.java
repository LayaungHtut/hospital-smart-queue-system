package com.hospitalqueue.controller;

import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.service.NotificationService;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PatientAuthController {

    private final com.hospitalqueue.service.AuthService authService;
    private final NotificationService notificationService;
    private final QueueService queueService;
    private final AppointmentRepository appointmentRepository;

    public PatientAuthController(com.hospitalqueue.service.AuthService authService,
                                 NotificationService notificationService,
                                 QueueService queueService,
                                 AppointmentRepository appointmentRepository) {
        this.authService = authService;
        this.notificationService = notificationService;
        this.queueService = queueService;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/patient/login")
    public String loginPage() {
        return "patient/login";
    }

    @PostMapping("/patient/login")
    public String login(@RequestParam String phone, @RequestParam String password,
                        HttpSession session, Model model) {
        Patient patient = authService.loginPatient(phone, password);
        if (patient == null) {
            model.addAttribute("error", "Incorrect phone number or password.");
            return "patient/login";
        }
        SessionUtil.setPatient(session, patient);
        notificationService.notify(patient.getPatientId(), "Welcome back, " + patient.getName() + "!");
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/patient/register")
    public String registerPage() {
        return "patient/register";
    }

    @PostMapping("/patient/register")
    public String register(@RequestParam(required = false) String name,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String confirmPassword,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String dateOfBirth,
                           @RequestParam(required = false) String gender,
                           HttpSession session, Model model) {
        if (!java.util.Objects.equals(password, confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "patient/register";
        }
        try {
            Patient patient = authService.registerPatient(name, phone, password, email, address, dateOfBirth, gender);
            if (patient == null) {
                model.addAttribute("error", "Please check your details. Fields are invalid.");
                return "patient/register";
            }
            SessionUtil.setPatient(session, patient);
            return "redirect:/patient/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "patient/register";
        } catch (IllegalStateException e) {
            model.addAttribute("info", e.getMessage());
            return "patient/register";
        }
    }

    @GetMapping("/patient/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("activeQueue", queueService.getActiveQueue(patient.getPatientId()));
        model.addAttribute("hasAppointmentToday", appointmentRepository.findTodaysAppointment(patient.getPatientId()) != null);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(patient.getPatientId()));
        return "patient/dashboard";
    }

    @GetMapping("/patient/logout")
    public String logout(HttpSession session) {
        SessionUtil.logout(session);
        return "redirect:/";
    }
}
