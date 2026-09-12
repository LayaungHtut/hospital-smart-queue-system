package com.hospitalqueue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PatientQueueController {

    private final QueueService queueService;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatientQueueController(QueueService queueService,
                                  DoctorRepository doctorRepository,
                                  DepartmentRepository departmentRepository,
                                  AppointmentRepository appointmentRepository) {
        this.queueService = queueService;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/patient/queue")
    public String queuePage(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("activeQueue", queueService.getActiveQueue(patient.getPatientId()));
        return "patient/queue";
    }

    @PostMapping("/patient/queue/create")
    public String createQueue(@RequestParam String doctorId,
                              @RequestParam int departmentId,
                              @RequestParam(required = false) String priority,
                              @RequestParam(defaultValue = "false") boolean emergency,
                              @RequestParam(required = false) List<Integer> symptomIds,
                              HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        if (priority == null || priority.isBlank()) {
            priority = emergency ? Queue.PRIORITY_EMERGENCY : Queue.PRIORITY_NORMAL;
        }
        try {
            queueService.createQueue(patient.getPatientId(), doctorId, departmentId, priority, emergency, Queue.SOURCE_ONLINE, symptomIds != null ? symptomIds : List.of());
            model.addAttribute("message", "Queue created successfully.");
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("patient", patient);
        model.addAttribute("activeQueue", queueService.getActiveQueue(patient.getPatientId()));
        return "patient/queue";
    }

    @PostMapping("/patient/queue/checkin")
    public String checkIn(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        try {
            queueService.checkInAppointment(patient.getPatientId());
            model.addAttribute("message", "Check-in successful. You have appointment priority.");
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("patient", patient);
        model.addAttribute("activeQueue", queueService.getActiveQueue(patient.getPatientId()));
        return "patient/queue";
    }

    @PostMapping("/patient/queue/cancel")
    public String cancel(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        try {
            queueService.cancelQueue(patient.getPatientId());
            model.addAttribute("message", "Queue cancelled successfully.");
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("patient", patient);
        model.addAttribute("activeQueue", queueService.getActiveQueue(patient.getPatientId()));
        return "patient/queue";
    }

    @GetMapping(value = "/patient/queue/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String queueJson(HttpSession session) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "{\"hasQueue\":false}";
        }
        Queue queue = queueService.getActiveQueue(patient.getPatientId());
        boolean hasAppointment = appointmentRepository.findTodaysAppointment(patient.getPatientId()) != null;

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("hasQueue", queue != null);
        if (queue != null) {
            Doctor doctor = doctorRepository.findById(queue.getDoctorId());
            Department department = departmentRepository.findById(queue.getDepartmentId());
            json.put("queueNumber", queue.getQueueNumber());
            json.put("status", queue.getStatus());
            json.put("priority", queue.getPriority());
            json.put("position", queue.getPosition());
            json.put("estimatedWaitingTime", queue.getEstimatedWaitingTime());
            json.put("doctorName", doctor != null ? doctor.getName() : "");
            json.put("departmentName", department != null ? department.getDepartmentName() : "");
        }
        json.put("hasAppointmentToday", hasAppointment);
        try {
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            return "{\"hasQueue\":false}";
        }
    }
}
