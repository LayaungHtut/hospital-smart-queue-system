package com.hospitalqueue.api;

import com.hospitalqueue.ai.AIRecommendationService;
import com.hospitalqueue.ai.AiInteractiveTriageService;
import com.hospitalqueue.ai.AiQueueLoadBalancerService;
import com.hospitalqueue.ai.AiSoapService;
import com.hospitalqueue.ml.ConsultationDurationService;
import com.hospitalqueue.ml.FollowUpInstructionService;
import com.hospitalqueue.ml.NoShowPredictionService;
import com.hospitalqueue.ml.QueueFlowPredictionService;
import com.hospitalqueue.ml.RagChatbotService;
import com.hospitalqueue.ml.SmartDoctorAssignmentService;
import com.hospitalqueue.ml.WaitTimePredictionService;
import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.MlModelRepository;
import com.hospitalqueue.repository.NoShowPredictionRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.repository.WaitTimePredictionRepository;
import com.hospitalqueue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private static final Logger log = LoggerFactory.getLogger(AiApiController.class);

    private final AIRecommendationService aiRecommendationService;
    private final WaitTimePredictionService waitTimePredictionService;
    private final NoShowPredictionService noShowPredictionService;
    private final RagChatbotService ragChatbotService;
    private final AiSoapService aiSoapService;
    private final AiInteractiveTriageService aiInteractiveTriageService;
    private final AiQueueLoadBalancerService aiQueueLoadBalancerService;
    private final ConsultationDurationService consultationDurationService;
    private final SmartDoctorAssignmentService smartDoctorAssignmentService;
    private final QueueFlowPredictionService queueFlowPredictionService;
    private final FollowUpInstructionService followUpInstructionService;
    private final MlModelRepository mlModelRepository;
    private final WaitTimePredictionRepository waitTimePredictionRepository;
    private final NoShowPredictionRepository noShowPredictionRepository;
    private final QueueRepository queueRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final QueueService queueService;

    public AiApiController(AIRecommendationService aiRecommendationService,
                           WaitTimePredictionService waitTimePredictionService,
                           NoShowPredictionService noShowPredictionService,
                           RagChatbotService ragChatbotService,
                           AiSoapService aiSoapService,
                           AiInteractiveTriageService aiInteractiveTriageService,
                           AiQueueLoadBalancerService aiQueueLoadBalancerService,
                           ConsultationDurationService consultationDurationService,
                           SmartDoctorAssignmentService smartDoctorAssignmentService,
                           QueueFlowPredictionService queueFlowPredictionService,
                           FollowUpInstructionService followUpInstructionService,
                           MlModelRepository mlModelRepository,
                           WaitTimePredictionRepository waitTimePredictionRepository,
                           NoShowPredictionRepository noShowPredictionRepository,
                           QueueRepository queueRepository,
                           AppointmentRepository appointmentRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           DepartmentRepository departmentRepository,
                           QueueService queueService) {
        this.aiRecommendationService = aiRecommendationService;
        this.waitTimePredictionService = waitTimePredictionService;
        this.noShowPredictionService = noShowPredictionService;
        this.ragChatbotService = ragChatbotService;
        this.aiSoapService = aiSoapService;
        this.aiInteractiveTriageService = aiInteractiveTriageService;
        this.aiQueueLoadBalancerService = aiQueueLoadBalancerService;
        this.consultationDurationService = consultationDurationService;
        this.smartDoctorAssignmentService = smartDoctorAssignmentService;
        this.queueFlowPredictionService = queueFlowPredictionService;
        this.followUpInstructionService = followUpInstructionService;
        this.mlModelRepository = mlModelRepository;
        this.waitTimePredictionRepository = waitTimePredictionRepository;
        this.noShowPredictionRepository = noShowPredictionRepository;
        this.queueRepository = queueRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.queueService = queueService;
    }

    // ==================== TRIAGE & RECOMMENDATION ====================

    @PostMapping("/triage")
    public ResponseEntity<?> triage(@RequestBody TriageRequest request) {
        try {
            AIRecommendationService.Recommendation rec = aiRecommendationService.recommend(request.symptoms);
            
            Map<String, Object> response = new HashMap<>();
            response.put("department", rec.getDepartment() != null ?
                    Map.of("id", rec.getDepartment().getDepartmentId(), "code", rec.getDepartment().getDepartmentCode(), "name", rec.getDepartment().getDepartmentName()) : null);
            response.put("emergency", rec.isEmergency());
            response.put("acuityScore", rec.getAcuityScore());
            response.put("disposition", rec.getDisposition());
            response.put("recommendedTests", rec.getRecommendedTests());
            response.put("recommendedLabs", rec.getRecommendedLabs());
            response.put("reason", rec.getReason());
            response.put("aiUsed", rec.isAiUsed());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Triage error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Triage failed"));
        }
    }

    @PostMapping("/triage/{departmentCode}")
    public ResponseEntity<?> triageForDepartment(@PathVariable String departmentCode, @RequestBody TriageRequest request) {
        Department dept = departmentRepository.findByCode(departmentCode.toUpperCase());
        if (dept == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid department code"));
        }
        
        AIRecommendationService.Recommendation rec = aiRecommendationService.triageOnly(request.symptoms, dept);
        
        Map<String, Object> response = new HashMap<>();
        response.put("department", Map.of("id", dept.getDepartmentId(), "code", dept.getDepartmentCode(), "name", dept.getDepartmentName()));
        response.put("emergency", rec.isEmergency());
        response.put("acuityScore", rec.getAcuityScore());
        response.put("disposition", rec.getDisposition());
        response.put("recommendedTests", rec.getRecommendedTests());
        response.put("recommendedLabs", rec.getRecommendedLabs());
        response.put("reason", rec.getReason());
        response.put("aiUsed", rec.isAiUsed());
        
        return ResponseEntity.ok(response);
    }

    // ==================== WAIT TIME PREDICTION ====================

    @GetMapping("/wait-time/{doctorId}")
    public ResponseEntity<?> predictWaitTime(@PathVariable String doctorId,
                                             @RequestParam(required = false) String patientId,
                                             @RequestParam(required = false) Integer departmentId) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Doctor not found"));
        }

        Department department = departmentId != null ? departmentRepository.findById(departmentId) 
                : departmentRepository.findById(doctor.getDepartmentId());
        if (department == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department not found"));
        }

        Patient patient = patientId != null ? patientRepository.findById(String.valueOf(patientId)) : null;
        int patientAge = patient != null ? calculateAge(patient.getDateOfBirth()) : 30;
        int patientPriority = 3; // default
        boolean isNewPatient = patient == null;

        // Get current queue length
        Map<String, Integer> queueCounts = queueRepository.countWaitingByAllDoctors();
        int queueLength = queueCounts.getOrDefault(doctorId, 0);

        Double predictedWait = waitTimePredictionService.predictWaitTime(
                doctor, department, queueLength, patientAge, patientPriority, isNewPatient);

        long avgConsult = doctor.getAverageConsultationMinutes();
        Double fallbackWait = queueLength * (avgConsult > 0 ? avgConsult : 15.0);

        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctorId);
        response.put("doctorName", doctor.getName());
        response.put("department", department.getDepartmentCode());
        response.put("currentQueueLength", queueLength);
        response.put("predictedWaitMinutes", predictedWait);
        response.put("fallbackWaitMinutes", fallbackWait);
        response.put("modelUsed", waitTimePredictionService.isModelLoaded());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wait-time/department/{departmentCode}")
    public ResponseEntity<?> predictWaitTimesForDepartment(@PathVariable String departmentCode,
                                                            @RequestParam(required = false) String patientId) {
        Department department = departmentRepository.findByCode(departmentCode.toUpperCase());
        if (department == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department not found"));
        }

        List<Doctor> doctors = doctorRepository.findByDepartment(department.getDepartmentId());
        Patient patient = patientId != null ? patientRepository.findById(patientId) : null;
        int patientAge = patient != null ? calculateAge(patient.getDateOfBirth()) : 30;
        int patientPriority = 3;
        boolean isNewPatient = patient == null;

        Map<String, Double> predictions = waitTimePredictionService.predictWaitTimesForDepartment(
                department, doctors, patientAge, patientPriority, isNewPatient);

        Map<String, Object> response = new HashMap<>();
        response.put("department", Map.of("id", department.getDepartmentId(), "code", department.getDepartmentCode(), "name", department.getDepartmentName()));
        response.put("predictions", predictions);
        response.put("modelUsed", waitTimePredictionService.isModelLoaded());

        return ResponseEntity.ok(response);
    }

    // ==================== NO-SHOW PREDICTION ====================

    @PostMapping("/noshow/appointment/{appointmentId}")
    public ResponseEntity<?> predictNoShow(@PathVariable Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId);
        if (appointment == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Appointment not found"));
        }

        Double probability = noShowPredictionService.predictNoShowProbability(appointment);
        boolean predictedNoShow = noShowPredictionService.predictNoShow(appointment);
        String riskLevel = noShowPredictionService.getRiskLevel(appointment);

        Map<String, Object> response = new HashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("predictedProbability", probability);
        response.put("predictedNoShow", predictedNoShow);
        response.put("riskLevel", riskLevel);
        response.put("modelUsed", noShowPredictionService.isModelLoaded());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/noshow/high-risk")
    public ResponseEntity<?> getHighRiskAppointments(@RequestParam(required = false) Integer daysAhead) {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(daysAhead != null ? daysAhead : 7);
        
        List<com.hospitalqueue.model.NoShowPrediction> predictions = 
                noShowPredictionRepository.findHighRiskAppointments(from, to);

        return ResponseEntity.ok(Map.of(
                "count", predictions.size(),
                "predictions", predictions,
                "period", Map.of("from", from, "to", to)
        ));
    }

    // ==================== CHATBOT ====================

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (!ragChatbotService.isReady()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Chatbot not available",
                    "message", "Please configure OpenRouter API key and ChromaDB"
            ));
        }

        long start = System.currentTimeMillis();
        String response = ragChatbotService.chat(request.message());
        int responseTime = (int) (System.currentTimeMillis() - start);

        return ResponseEntity.ok(Map.of(
                "response", response,
                "responseTimeMs", responseTime,
                "sessionId", request.sessionId()
        ));
    }

    @GetMapping("/chat/status")
    public ResponseEntity<?> chatbotStatus() {
        return ResponseEntity.ok(ragChatbotService.getStatus());
    }

    // ==================== MODEL MANAGEMENT ====================

    @GetMapping("/models")
    public ResponseEntity<?> listModels() {
        return ResponseEntity.ok(mlModelRepository.findByType("wait_time"));
    }

    @GetMapping("/models/{modelType}")
    public ResponseEntity<?> getModelsByType(@PathVariable String modelType) {
        return ResponseEntity.ok(mlModelRepository.findByType(modelType));
    }

    @GetMapping("/models/active/{modelType}")
    public ResponseEntity<?> getActiveModel(@PathVariable String modelType) {
        return mlModelRepository.findActiveByType(modelType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== CLINICAL COPILOT & SOAP NOTES ====================

    @PostMapping("/soap-note")
    public ResponseEntity<?> generateSoapNote(@RequestBody SoapNoteRequest request) {
        try {
            AiSoapService.SoapNoteResult result = aiSoapService.generateSoapNote(
                    request.patientName(),
                    request.patientAge(),
                    request.gender(),
                    request.symptoms(),
                    request.vitals(),
                    request.observations(),
                    request.department(),
                    request.specialization()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("SOAP note generation error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate SOAP note"));
        }
    }

    // ==================== INTERACTIVE MULTI-TURN TRIAGE ====================

    @PostMapping("/triage/interactive/questions")
    public ResponseEntity<?> getInteractiveTriageQuestions(@RequestBody TriageQuestionsRequest request) {
        try {
            AiInteractiveTriageService.InteractiveTriageResponse response =
                    aiInteractiveTriageService.generateQuestions(request.symptoms());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Interactive triage questions error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate triage questions"));
        }
    }

    @PostMapping("/triage/interactive/finalize")
    public ResponseEntity<?> finalizeInteractiveTriage(@RequestBody FinalizeTriageRequest request) {
        try {
            AiInteractiveTriageService.FinalizedTriageResult result =
                    aiInteractiveTriageService.finalizeTriage(request.symptoms(), request.answers());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Finalize interactive triage error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to finalize triage"));
        }
    }

    // ==================== SMART QUEUE LOAD BALANCER ====================

    @GetMapping("/load-balancer/suggestions")
    public ResponseEntity<?> getLoadBalancerSuggestions(@RequestParam(required = false) String department) {
        try {
            AiQueueLoadBalancerService.LoadBalancerReport report =
                    aiQueueLoadBalancerService.analyzeAndSuggest(department);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Load balancer suggestion error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate load balance report"));
        }
    }

    @PostMapping("/load-balancer/apply")
    public ResponseEntity<?> applyLoadBalancerPlan(@RequestBody ApplyLoadBalancerRequest request) {
        try {
            Map<String, Object> result = aiQueueLoadBalancerService.applyLoadBalancerPlan(
                    request.queueIds(),
                    request.staffId() != null ? request.staffId() : "STAFF"
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Apply load balancer plan error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to apply load balance plan"));
        }
    }

    // ==================== CONSULTATION DURATION PREDICTION ====================

    @GetMapping("/consultation-duration/{doctorId}")
    public ResponseEntity<?> predictConsultationDuration(@PathVariable String doctorId,
                                                         @RequestParam(required = false) String patientId,
                                                         @RequestParam(required = false) String symptoms,
                                                         @RequestParam(required = false) String appointmentType) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Doctor not found"));
        }

        Patient patient = patientId != null ? patientRepository.findById(patientId) : null;
        int patientAge = patient != null ? calculateAge(patient.getDateOfBirth()) : 30;
        boolean isNewPatient = patient == null;

        double predicted = consultationDurationService.predictDuration(
                doctor, patientAge, symptoms, appointmentType, isNewPatient);

        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctorId);
        response.put("doctorName", doctor.getName());
        response.put("predictedMinutes", predicted);
        response.put("doctorAverageMinutes", doctor.getAverageConsultationMinutes());
        response.put("patientAge", patientAge);
        response.put("isNewPatient", isNewPatient);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/consultation-duration/department/{departmentCode}")
    public ResponseEntity<?> predictDurationsForDepartment(@PathVariable String departmentCode,
                                                            @RequestParam(required = false) String patientId,
                                                            @RequestParam(required = false) String symptoms,
                                                            @RequestParam(required = false) String appointmentType) {
        Department department = departmentRepository.findByCode(departmentCode.toUpperCase());
        if (department == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department not found"));
        }

        Patient patient = patientId != null ? patientRepository.findById(patientId) : null;
        int patientAge = patient != null ? calculateAge(patient.getDateOfBirth()) : 30;
        boolean isNewPatient = patient == null;

        Map<String, Double> predictions = consultationDurationService.predictDurationsForDepartment(
                department.getDepartmentId(), patientAge, symptoms, appointmentType, isNewPatient);

        Map<String, Object> response = new HashMap<>();
        response.put("department", Map.of("id", department.getDepartmentId(), "code", department.getDepartmentCode(), "name", department.getDepartmentName()));
        response.put("predictions", predictions);

        return ResponseEntity.ok(response);
    }

    // ==================== SMART DOCTOR ASSIGNMENT ====================

    @GetMapping("/smart-assignment/{departmentCode}")
    public ResponseEntity<?> getSmartAssignment(@PathVariable String departmentCode,
                                                 @RequestParam(required = false) String symptoms,
                                                 @RequestParam(required = false) String patientId) {
        Department department = departmentRepository.findByCode(departmentCode.toUpperCase());
        if (department == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department not found"));
        }

        List<SmartDoctorAssignmentService.DoctorRecommendation> recommendations =
                smartDoctorAssignmentService.recommendDoctors(
                        department.getDepartmentId(), symptoms, patientId);

        List<Map<String, Object>> recList = recommendations.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", r.doctor().getDoctorId());
            m.put("doctorName", r.doctor().getName());
            m.put("score", Math.round(r.score() * 10.0) / 10.0);
            m.put("rank", r.rank());
            m.put("queueLoadScore", Math.round(r.queueLoadScore() * 10.0) / 10.0);
            m.put("availabilityScore", Math.round(r.availabilityScore() * 10.0) / 10.0);
            m.put("specializationScore", Math.round(r.specializationScore() * 10.0) / 10.0);
            m.put("historyScore", Math.round(r.historyScore() * 10.0) / 10.0);
            m.put("estimatedWaitMinutes", Math.round(r.waitTimeMinutes()));
            m.put("reason", r.reason());
            return m;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("department", Map.of("code", department.getDepartmentCode(), "name", department.getDepartmentName()));
        response.put("recommendations", recList);
        response.put("symptoms", symptoms);

        return ResponseEntity.ok(response);
    }

    // ==================== QUEUE FLOW PREDICTION ====================

    @GetMapping("/queue-flow/predict")
    public ResponseEntity<?> predictQueueFlow(@RequestParam(defaultValue = "3") int hoursAhead) {
        try {
            QueueFlowPredictionService.FlowPrediction prediction =
                    queueFlowPredictionService.predictNextHours(Math.min(hoursAhead, 8));
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("Queue flow prediction error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Prediction failed"));
        }
    }

    @GetMapping("/queue-flow/by-department")
    public ResponseEntity<?> predictQueueFlowByDepartment(@RequestParam(defaultValue = "3") int hoursAhead) {
        try {
            QueueFlowPredictionService.DepartmentFlowPrediction prediction =
                    queueFlowPredictionService.predictByDepartment(Math.min(hoursAhead, 8));
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            log.error("Queue flow by department prediction error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Prediction failed"));
        }
    }

    @GetMapping("/queue-flow/peak-hours")
    public ResponseEntity<?> getPeakHours(@RequestParam(required = false) String dayOfWeek) {
        try {
            java.time.DayOfWeek dow = dayOfWeek != null
                    ? java.time.DayOfWeek.valueOf(dayOfWeek.toUpperCase())
                    : java.time.DayOfWeek.from(java.time.LocalDate.now());
            QueueFlowPredictionService.PeakHoursReport report =
                    queueFlowPredictionService.getPeakHours(dow);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Peak hours analysis error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Analysis failed"));
        }
    }

    @GetMapping("/queue-flow/staffing")
    public ResponseEntity<?> getStaffingRecommendation(@RequestParam(defaultValue = "3") int hoursAhead) {
        try {
            QueueFlowPredictionService.StaffingRecommendation rec =
                    queueFlowPredictionService.getStaffingRecommendation(Math.min(hoursAhead, 8));
            return ResponseEntity.ok(rec);
        } catch (Exception e) {
            log.error("Staffing recommendation error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Recommendation failed"));
        }
    }

    // ==================== FOLLOW-UP INSTRUCTIONS ====================

    @PostMapping("/follow-up/generate")
    public ResponseEntity<?> generateFollowUpInstructions(@RequestBody FollowUpRequest request) {
        try {
            FollowUpInstructionService.FollowUpResult result =
                    followUpInstructionService.generateInstructions(
                            request.diagnosis(),
                            request.medications(),
                            request.patientAge(),
                            request.patientGender(),
                            request.department(),
                            request.additionalNotes());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Follow-up instruction generation error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate instructions"));
        }
    }

    // ==================== HELPER RECORDS ====================

    public record TriageRequest(String symptoms) {}
    public record ChatRequest(String message, String sessionId) {}
    public record SoapNoteRequest(String patientName, String patientAge, String gender,
                                  String symptoms, String vitals, String observations,
                                  String department, String specialization) {}
    public record TriageQuestionsRequest(String symptoms) {}
    public record FinalizeTriageRequest(String symptoms, Map<String, String> answers) {}
    public record ApplyLoadBalancerRequest(List<String> queueIds, String staffId) {}
    public record FollowUpRequest(String diagnosis, String medications, String patientAge,
                                   String patientGender, String department, String additionalNotes) {}

    private int calculateAge(java.time.LocalDate dob) {
        if (dob == null) return 30;
        return (int) java.time.temporal.ChronoUnit.YEARS.between(dob, java.time.LocalDate.now());
    }
}