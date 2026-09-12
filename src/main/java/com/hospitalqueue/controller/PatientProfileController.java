package com.hospitalqueue.controller;

import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.util.SessionUtil;
import com.hospitalqueue.util.Validator;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class PatientProfileController {

    private final PatientRepository patientRepository;

    public PatientProfileController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping("/patient/profile")
    public String profile(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patientRepository.findById(patient.getPatientId()));
        return "patient/profile";
    }

    @PostMapping("/patient/profile")
    public String update(@RequestParam String name,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String dateOfBirth,
                         @RequestParam(required = false) String gender,
                         HttpSession session, Model model) {
        Patient sessionPatient = SessionUtil.getPatient(session);
        if (sessionPatient == null) {
            return "redirect:/patient/login";
        }
        Patient patient = patientRepository.findById(sessionPatient.getPatientId());
        if (patient == null) {
            return "redirect:/patient/login";
        }

        if (!Validator.isNotEmpty(name)) {
            model.addAttribute("error", "Full name is required.");
            model.addAttribute("patient", patient);
            return "patient/profile";
        }
        if (!Validator.isValidEmail(email)) {
            model.addAttribute("error", "Please enter a valid email address.");
            model.addAttribute("patient", patient);
            return "patient/profile";
        }

        patient.setName(name.trim());
        patient.setEmail(email);
        patient.setGender(gender);
        if (Validator.isNotEmpty(dateOfBirth)) {
            try {
                patient.setDateOfBirth(LocalDate.parse(dateOfBirth));
            } catch (Exception ignored) {
            }
        }
        patientRepository.update(patient);

        SessionUtil.setPatient(session, patient);
        model.addAttribute("message", "Profile updated successfully.");
        model.addAttribute("patient", patient);
        return "patient/profile";
    }
}
