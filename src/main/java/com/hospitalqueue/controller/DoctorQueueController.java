package com.hospitalqueue.controller;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Doctor consultation workflow: start, pause, resume, call next, complete.
 */
@Controller
public class DoctorQueueController {

    private final QueueService queueService;
    private final QueueRepository queueRepository;

    public DoctorQueueController(QueueService queueService, QueueRepository queueRepository) {
        this.queueService = queueService;
        this.queueRepository = queueRepository;
    }

    @GetMapping("/doctor/queue")
    public String queuePage(HttpSession session, Model model) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        model.addAttribute("doctor", doctor);
        model.addAttribute("called", queueService.called(doctor.getDoctorId()));
        model.addAttribute("serving", queueService.serving(doctor.getDoctorId()));
        model.addAttribute("waiting", queueService.waitingForDoctor(doctor.getDoctorId()));
        return "doctor/queue";
    }

    @PostMapping("/doctor/queue/call-next")
    public String callNext(HttpSession session, RedirectAttributes ra) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        if (queueService.currentConsultation(doctor.getDoctorId()) != null) {
            ra.addFlashAttribute("error", "A patient is already called or being served. Start or complete the current consultation first.");
        } else {
            Queue called = queueService.callNext(doctor.getDoctorId());
            ra.addFlashAttribute("message", called != null ? "Patient called. Please start the consultation when they arrive." : "No patients waiting.");
        }
        return "redirect:/doctor/queue";
    }

    @PostMapping("/doctor/queue/start")
    public String start(HttpSession session, RedirectAttributes ra) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        if (queueService.startConsultation(doctor.getDoctorId())) {
            ra.addFlashAttribute("message", "Consultation started.");
        } else {
            ra.addFlashAttribute("error", "No called patient to start.");
        }
        return "redirect:/doctor/queue";
    }

    @PostMapping("/doctor/queue/complete")
    public String complete(HttpSession session, RedirectAttributes ra) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        queueService.completeCurrent(doctor.getDoctorId());
        ra.addFlashAttribute("message", "Consultation completed.");
        return "redirect:/doctor/queue";
    }

    @PostMapping("/doctor/queue/pause")
    public String pause(HttpSession session, RedirectAttributes ra) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        queueService.pauseCurrent(doctor.getDoctorId());
        ra.addFlashAttribute("message", "Consultation paused.");
        return "redirect:/doctor/queue";
    }

    @PostMapping("/doctor/queue/resume")
    public String resume(HttpSession session, RedirectAttributes ra) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        queueService.resumeCurrent(doctor.getDoctorId());
        ra.addFlashAttribute("message", "Consultation resumed.");
        return "redirect:/doctor/queue";
    }

    @GetMapping("/doctor/history")
    public String history(HttpSession session, Model model) {
        Doctor doctor = SessionUtil.getDoctor(session);
        if (doctor == null) {
            return "redirect:/doctor/login";
        }
        model.addAttribute("doctor", doctor);
        model.addAttribute("history", queueRepository.findHistoryForDoctor(doctor.getDoctorId()));
        return "doctor/history";
    }
}
