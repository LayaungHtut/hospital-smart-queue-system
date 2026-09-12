package com.hospitalqueue.api;

import com.hospitalqueue.ai.AIRecommendationService;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Symptom;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.SymptomRepository;
import com.hospitalqueue.service.QueueService;
import com.hospitalqueue.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class LookupApiController {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final RecommendationService recommendationService;
    private final QueueService queueService;
    private final SymptomRepository symptomRepository;
    private final JdbcTemplate jdbcTemplate;

    public LookupApiController(DepartmentRepository departmentRepository,
            DoctorRepository doctorRepository,
            RecommendationService recommendationService,
            QueueService queueService,
            SymptomRepository symptomRepository,
            JdbcTemplate jdbcTemplate) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.recommendationService = recommendationService;
        this.queueService = queueService;
        this.symptomRepository = symptomRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/departments")
    public List<Map<String, Object>> getDepartments() {
        List<Department> list = departmentRepository.findAll();
        return list.stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", d.getDepartmentId());
            map.put("departmentCode", d.getDepartmentCode());
            map.put("name", d.getDepartmentName());
            map.put("location", "Building " + (char) ('A' + (d.getDepartmentId() % 3)) + ", Floor "
                    + (1 + (d.getDepartmentId() % 4)));
            map.put("status", d.isActive() ? "ACTIVE" : "INACTIVE");
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/doctors")
    public List<Map<String, Object>> getDoctors(@RequestParam(required = false) Integer departmentId) {
        List<Doctor> doctors;
        if (departmentId != null && departmentId > 0) {
            doctors = recommendationService.recommendDoctors(departmentId);
        } else {
            doctors = queueService.enrichAvailability(doctorRepository.findAll());
        }

        Map<String, Long> waitingTimeMap = recommendationService.buildWaitingTimeMap(doctors);
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));

        return doctors.stream().map(doc -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", doc.getDoctorId());
            map.put("doctorCode", doc.getDoctorCode());
            map.put("name", doc.getName());
            Department dept = deptMap.get(doc.getDepartmentId());
            map.put("department", dept != null ? dept.getDepartmentName() : "General");
            map.put("departmentId", doc.getDepartmentId());
            map.put("qualification", doc.getQualification() != null ? doc.getQualification() : "MBBS, M.Med.Sc");
            map.put("specialization", doc.getSpecialization() != null ? doc.getSpecialization() : "General Medicine");
            map.put("currentPatientId", 0);
            map.put("experienceYears", doc.getYearsOfExperience());
            map.put("estimatedWaitingMinutes", waitingTimeMap.getOrDefault(doc.getDoctorId(), 0L));
            map.put("status", doc.isActive() ? "ACTIVE" : "INACTIVE");
            map.put("phone", doc.getPhone());
            map.put("available", doc.isAvailable());
            map.put("availabilityStatus", doc.isAvailable() ? "CONSULTING" : "ON_BREAK");
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/system/settings")
    public Map<String, Object> getPublicSystemSettings() {
        // Same system_setting table the admin "System Settings" page reads/writes
        // (AdminApiController#getSystemSettings/saveSystemSettings) — public callers
        // see whatever the admin has actually configured, not invented values.
        Map<String, String> dbSettings = new HashMap<>();
        jdbcTemplate.query(
                "SELECT setting_key, setting_value FROM system_setting WHERE setting_key IN "
                        + "('hospital_name', 'logo_url', 'timezone', 'contact_phone', 'contact_email', 'operating_hours')",
                (RowCallbackHandler) rs -> dbSettings.put(rs.getString("setting_key"), rs.getString("setting_value")));

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("hospitalName", dbSettings.getOrDefault("hospital_name", "CareQueue General Hospital"));
        settings.put("logoUrl", dbSettings.getOrDefault("logo_url", ""));
        settings.put("contactPhone", dbSettings.getOrDefault("contact_phone", ""));
        settings.put("contactEmail", dbSettings.getOrDefault("contact_email", ""));
        settings.put("operatingHours", dbSettings.getOrDefault("operating_hours", ""));
        settings.put("timeZone", dbSettings.getOrDefault("timezone", "(GMT+06:30) Yangon"));
        return settings;
    }

    @GetMapping("/symptoms")
    public List<Map<String, Object>> getSymptoms() {
        List<Symptom> symptoms = symptomRepository.findAll();
        return symptoms.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getSymptomId());
            map.put("name", s.getSymptomName());
            map.put("code", s.getSymptomCode());
            map.put("category", s.getCategory());
            return map;
        }).collect(Collectors.toList());
    }

    public static class RecommendRequest {
        public String symptoms;
        public Integer departmentId;
    }

    @PostMapping("/patient/recommend")
    public ResponseEntity<?> recommend(@RequestBody RecommendRequest req) {
        if (req == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }

        Department recommended = null;
        boolean emergency = false;
        String reason = null;
        boolean aiUsed = false;
        int acuityScore = 3;
        String disposition = "routine";
        List<String> recommendedTests = List.of();
        List<String> recommendedLabs = List.of();

        if (req.departmentId != null && req.departmentId > 0) {
            recommended = departmentRepository.findById(req.departmentId);
        } else if (req.symptoms != null && !req.symptoms.isBlank()) {
            AIRecommendationService.Recommendation rec = recommendationService.recommendDepartmentAI(req.symptoms);
            recommended = rec.getDepartment();
            emergency = rec.isEmergency();
            reason = rec.getReason();
            aiUsed = rec.isAiUsed();
            acuityScore = rec.getAcuityScore();
            disposition = rec.getDisposition();
            recommendedTests = rec.getRecommendedTests() != null ? rec.getRecommendedTests() : List.of();
            recommendedLabs = rec.getRecommendedLabs() != null ? rec.getRecommendedLabs() : List.of();
        }

        if (recommended == null) {
            recommended = departmentRepository.findByName("General Medicine");
            if (recommended == null) {
                recommended = departmentRepository.findById(4);
            }
        }

        List<Doctor> doctors = recommendationService.recommendDoctors(recommended.getDepartmentId());
        Map<String, Long> waitingTimeMap = recommendationService.buildWaitingTimeMap(doctors);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("department", Map.of(
                "id", recommended.getDepartmentId(),
                "departmentCode", recommended.getDepartmentCode(),
                "name", recommended.getDepartmentName()));
        result.put("emergency", emergency);
        result.put("acuityScore", acuityScore);
        result.put("disposition", disposition);
        result.put("recommendedTests", recommendedTests);
        result.put("recommendedLabs", recommendedLabs);
        result.put("reason", reason != null ? reason : "Recommended based on symptom profile");
        result.put("aiUsed", aiUsed);

        final Department finalDept = recommended;
        result.put("doctors", doctors.stream().map(d -> {
            Map<String, Object> docInfo = new LinkedHashMap<>();
            docInfo.put("id", d.getDoctorId());
            docInfo.put("doctorCode", d.getDoctorCode());
            docInfo.put("name", d.getName());
            docInfo.put("department", finalDept.getDepartmentName());
            docInfo.put("qualification", d.getQualification() != null ? d.getQualification() : "MBBS, M.Med.Sc");
            docInfo.put("specialization", d.getSpecialization() != null ? d.getSpecialization() : "General Medicine");
            docInfo.put("experienceYears", d.getYearsOfExperience() > 0 ? d.getYearsOfExperience() : 5);
            docInfo.put("estimatedWaitingMinutes", waitingTimeMap.getOrDefault(d.getDoctorId(), 15L));
            docInfo.put("available", d.isAvailable());
            docInfo.put("availabilityStatus", d.isAvailable() ? "CONSULTING" : "ON_BREAK");
            return docInfo;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }
}
