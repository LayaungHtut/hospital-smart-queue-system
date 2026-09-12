package com.hospitalqueue.controller;

import com.hospitalqueue.ai.AIRecommendationService;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.SymptomRepository;
import com.hospitalqueue.service.RecommendationService;
import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Patient symptom entry -> AI department recommendation -> doctor selection.
 */
@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final DepartmentRepository departmentRepository;
    private final SymptomRepository symptomRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    DepartmentRepository departmentRepository,
                                    SymptomRepository symptomRepository) {
        this.recommendationService = recommendationService;
        this.departmentRepository = departmentRepository;
        this.symptomRepository = symptomRepository;
    }

    @GetMapping("/patient/symptoms")
    public String symptomEntry(HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("allDepartments", departmentRepository.findAll());
        model.addAttribute("allSymptoms", symptomRepository.findAll());
        return "patient/symptoms";
    }

    @PostMapping("/patient/recommend")
    public String recommend(@RequestParam(required = false) String symptoms,
                            @RequestParam(required = false) List<Integer> symptomIds,
                            @RequestParam(required = false) String departmentId,
                            HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("allDepartments", departmentRepository.findAll());
        model.addAttribute("allSymptoms", symptomRepository.findAll());

        // Build symptoms text from selected symptom IDs if no free-text provided
        String symptomsText = symptoms;
        if ((symptomsText == null || symptomsText.isBlank()) && symptomIds != null && !symptomIds.isEmpty()) {
            List<com.hospitalqueue.model.Symptom> selectedSymptoms = symptomRepository.findByIds(symptomIds);
            StringBuilder sb = new StringBuilder();
            for (com.hospitalqueue.model.Symptom s : selectedSymptoms) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(s.getSymptomName());
            }
            symptomsText = sb.toString();
            model.addAttribute("selectedSymptomIds", symptomIds);
        }

        Department recommended = null;
        boolean emergency = false;
        String reason = null;
        boolean aiUsed = false;

        if (departmentId != null && !departmentId.isBlank()) {
            recommended = departmentRepository.findById(Integer.parseInt(departmentId));
            if (recommended == null) {
                model.addAttribute("error", "Invalid department selected.");
                return "patient/symptoms";
            }
        } else if (symptomsText != null && !symptomsText.isBlank()) {
            AIRecommendationService.Recommendation rec = recommendationService.recommendDepartmentAI(symptomsText);
            recommended = rec.getDepartment();
            emergency = rec.isEmergency();
            reason = rec.getReason();
            aiUsed = rec.isAiUsed();
        } else {
            model.addAttribute("error", "Please select or describe your symptoms.");
            return "patient/symptoms";
        }

        if (recommended == null) {
            model.addAttribute("error", "We could not recommend a department. Please describe your symptoms.");
            return "patient/symptoms";
        }

        List<Doctor> doctors = recommendationService.recommendDoctors(recommended.getDepartmentId());
        Map<String, Long> waitingTimeMap = recommendationService.buildWaitingTimeMap(doctors);

        model.addAttribute("recommendedDepartment", recommended);
        model.addAttribute("recommendedDoctors", doctors);
        model.addAttribute("waitingTimeMap", waitingTimeMap);
        model.addAttribute("emergency", emergency);
        model.addAttribute("aiReason", reason);
        model.addAttribute("aiUsed", aiUsed);
        model.addAttribute("symptomIds", symptomIds != null ? symptomIds : List.of());
        return "patient/choose-doctor";
    }

    @GetMapping("/patient/choose-doctor")
    public String chooseDoctor(@RequestParam(required = false) Integer departmentId,
                               HttpSession session, Model model) {
        Patient patient = SessionUtil.getPatient(session);
        if (patient == null) {
            return "redirect:/patient/login";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("allDepartments", departmentRepository.findAll());

        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId);
            if (department != null) {
                List<Doctor> doctors = recommendationService.recommendDoctors(departmentId);
                model.addAttribute("recommendedDepartment", department);
                model.addAttribute("recommendedDoctors", doctors);
                model.addAttribute("waitingTimeMap", recommendationService.buildWaitingTimeMap(doctors));
            }
        }
        return "patient/choose-doctor";
    }
}
