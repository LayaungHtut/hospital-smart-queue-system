package com.hospitalqueue.controller;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StaffController {

    private final QueueRepository queueRepository;
    private final QueueService queueService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public StaffController(QueueRepository queueRepository, QueueService queueService,
                           DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.queueRepository = queueRepository;
        this.queueService = queueService;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/staff/queues")
    public String queueMonitor(HttpSession session, Model model) {
        Staff staff = SessionUtil.getStaff(session);
        if (staff == null) {
            return "redirect:/staff/login";
        }
        List<Queue> waiting = queueRepository.findAllWaiting();
        enrich(waiting);
        Map<String, Integer> waitingCounts = new LinkedHashMap<>();
        for (Doctor doctor : doctorRepository.findAll()) {
            waitingCounts.put(doctor.getDoctorId(), queueRepository.countWaitingByDoctor(doctor.getDoctorId()));
        }
        model.addAttribute("staff", staff);
        model.addAttribute("waiting", waiting);
        model.addAttribute("waitingCounts", waitingCounts);
        model.addAttribute("doctors", queueService.enrichAvailability(doctorRepository.findAll()));
        return "staff/queueMonitor";
    }

    @PostMapping("/staff/emergency/confirm")
    public String confirmEmergency(@RequestParam long queueId, HttpSession session, RedirectAttributes ra) {
        if (SessionUtil.getStaff(session) == null) {
            return "redirect:/staff/login";
        }
        queueService.confirmEmergency(queueId);
        ra.addFlashAttribute("message", "Emergency confirmed. Patient moved to the front of the queue.");
        return "redirect:/staff/dashboard";
    }

    @PostMapping("/staff/reassign")
    public String reassign(@RequestParam long queueId, @RequestParam String doctorId,
                           HttpSession session, RedirectAttributes ra) {
        if (SessionUtil.getStaff(session) == null) {
            return "redirect:/staff/login";
        }
        queueService.reassignDoctor(queueId, doctorId);
        ra.addFlashAttribute("message", "Queue reassigned to another doctor.");
        return "redirect:/staff/queues";
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
}
