package com.hospitalqueue.api;

import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.QueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctor")
public class DoctorApiController {

    private final QueueService queueService;
    private final QueueRepository queueRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorApiController(QueueService queueService,
            QueueRepository queueRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DepartmentRepository departmentRepository,
            AppointmentRepository appointmentRepository) {
        this.queueService = queueService;
        this.queueRepository = queueRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/{doctorId}/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Doctor not found"));
        }

        Department dept = departmentRepository.findById(doctor.getDepartmentId());
        String deptName = dept != null ? dept.getDepartmentName() : "General Medicine";

        List<Queue> activeQueues = queueRepository.findActiveQueuesForDoctor(doctor.getDoctorId());
        Queue called = activeQueues.stream().filter(q -> "CALLED".equals(q.getStatus())).findFirst().orElse(null);
        Queue serving = activeQueues.stream().filter(q -> "SERVING".equals(q.getStatus())).findFirst().orElse(null);
        List<Queue> waiting = activeQueues.stream().filter(q -> "WAITING".equals(q.getStatus()))
                .collect(Collectors.toList());

        List<Appointment> todayAppointments = appointmentRepository.findTodayByDoctor(doctor.getDoctorId());
        int completedToday = (int) queueRepository.countCompletedTodayForDoctor(doctor.getDoctorId());

        Set<String> patientIds = new HashSet<>();
        for (Queue q : activeQueues) {
            if (q.getPatientId() != null)
                patientIds.add(q.getPatientId());
        }
        for (Appointment a : todayAppointments) {
            if (a.getPatientId() != null)
                patientIds.add(a.getPatientId());
        }

        Map<String, Patient> ptMap = patientIds.isEmpty()
                ? Collections.emptyMap()
                : patientRepository.findByIds(patientIds).stream()
                        .collect(Collectors.toMap(p -> p.getPatientId(), p -> p, (a, b) -> a));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPatientsToday",
                waiting.size() + (called != null ? 1 : 0) + (serving != null ? 1 : 0) + completedToday);
        stats.put("waitingCount", waiting.size());
        stats.put("completedCount", completedToday);
        stats.put("averageConsultationMinutes",
                doctor.getAverageConsultationMinutes() > 0 ? doctor.getAverageConsultationMinutes() : 15);

        Map<String, Object> doctorInfo = new LinkedHashMap<>();
        doctorInfo.put("id", doctor.getDoctorId());
        doctorInfo.put("name", doctor.getName());
        doctorInfo.put("doctorCode", doctor.getDoctorCode());
        doctorInfo.put("department", deptName);
        doctorInfo.put("departmentId", doctor.getDepartmentId());
        doctorInfo.put("specialization",
                doctor.getSpecialization() != null ? doctor.getSpecialization() : "Consultant");
        doctorInfo.put("available", doctor.isAvailable());
        doctorInfo.put("phone", doctor.getPhone());
        doctorInfo.put("email", doctor.getEmail());
        doctorInfo.put("maxQueueSize", doctor.getMaxQueueSize());
        doctorInfo.put("averageConsultationMinutes", doctor.getAverageConsultationMinutes());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("doctor", doctorInfo);
        res.put("stats", stats);
        res.put("called", called != null ? mapQueueWithPatients(called, ptMap, doctor, dept) : null);
        res.put("serving", serving != null ? mapQueueWithPatients(serving, ptMap, doctor, dept) : null);
        res.put("waiting",
                waiting.stream().map(q -> mapQueueWithPatients(q, ptMap, doctor, dept)).collect(Collectors.toList()));
        res.put("todayAppointments", todayAppointments.stream().map(a -> mapAppointmentWithPatients(a, ptMap, deptName))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/{doctorId}/queue")
    public ResponseEntity<?> getQueueState(@PathVariable String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Doctor not found"));
        }

        Queue called = queueService.called(doctorId);
        Queue serving = queueService.serving(doctorId);
        List<Queue> waiting = queueService.waitingForDoctor(doctorId);

        // All entries belong to this one doctor: resolve the doctor's department
        // once and batch-load every patient in one query, instead of the 3
        // separate DB round trips (patient/doctor/department) mapQueue() used to
        // make per queue row — this endpoint is polled frequently by the live
        // queue screen.
        Department dept = departmentRepository.findById(doctor.getDepartmentId());
        Set<String> patientIds = new HashSet<>();
        if (called != null)
            patientIds.add(called.getPatientId());
        if (serving != null)
            patientIds.add(serving.getPatientId());
        for (Queue q : waiting) {
            if (q.getPatientId() != null)
                patientIds.add(q.getPatientId());
        }
        Map<String, Patient> ptMap = patientIds.isEmpty()
                ? Collections.emptyMap()
                : patientRepository.findByIds(patientIds).stream()
                        .collect(Collectors.toMap(Patient::getPatientId, p -> p, (a, b) -> a));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("doctor", Map.of(
                "id", doctor.getDoctorId(),
                "name", doctor.getName(),
                "doctorCode", doctor.getDoctorCode(),
                "available", doctor.isAvailable()));
        res.put("called", called != null ? mapQueueWithPatients(called, ptMap, doctor, dept) : null);
        res.put("serving", serving != null ? mapQueueWithPatients(serving, ptMap, doctor, dept) : null);
        res.put("waiting",
                waiting.stream().map(q -> mapQueueWithPatients(q, ptMap, doctor, dept)).collect(Collectors.toList()));

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{doctorId}/availability")
    public ResponseEntity<?> toggleAvailability(@PathVariable String doctorId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Doctor not found"));
        }

        boolean newAvailability;
        if (payload != null && payload.containsKey("available")) {
            newAvailability = Boolean.parseBoolean(payload.get("available").toString());
        } else {
            newAvailability = !doctor.isAvailable();
        }

        doctorRepository.setAvailability(doctorId, newAvailability);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "available", newAvailability,
                "message", "Availability updated to " + (newAvailability ? "AVAILABLE" : "UNAVAILABLE")));
    }

    @PostMapping("/{doctorId}/queue/call-next")
    public ResponseEntity<?> callNext(@PathVariable String doctorId) {
        if (queueService.currentConsultation(doctorId) != null) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "A patient is already called or in consultation. Complete or start the current consultation first."));
        }
        Queue next = queueService.callNext(doctorId);
        if (next == null) {
            return ResponseEntity.ok(Map.of("message", "No patients waiting in queue.", "called", null));
        }
        return ResponseEntity.ok(Map.of("message", "Patient called successfully.", "called", mapQueue(next)));
    }

    @PostMapping("/{doctorId}/queue/start")
    public ResponseEntity<?> startConsultation(@PathVariable String doctorId) {
        boolean started = queueService.startConsultation(doctorId);
        if (!started) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "No called patient to start consultation with."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Consultation started."));
    }

    @PostMapping("/{doctorId}/queue/complete")
    public ResponseEntity<?> completeConsultation(@PathVariable String doctorId) {
        boolean completed = queueService.completeCurrent(doctorId);
        if (!completed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "No active consultation to complete."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Consultation completed."));
    }

    @PostMapping("/{doctorId}/queue/pause")
    public ResponseEntity<?> pauseConsultation(@PathVariable String doctorId) {
        boolean paused = queueService.pauseCurrent(doctorId);
        if (!paused) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to pause."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Consultation paused."));
    }

    @PostMapping("/{doctorId}/queue/resume")
    public ResponseEntity<?> resumeConsultation(@PathVariable String doctorId) {
        boolean resumed = queueService.resumeCurrent(doctorId);
        if (!resumed) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to resume."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Consultation resumed."));
    }

    @GetMapping("/{doctorId}/appointments")
    public List<Map<String, Object>> getAppointments(@PathVariable String doctorId) {
        List<Appointment> list = appointmentRepository.findByDoctor(doctorId);

        Doctor doctor = doctorRepository.findById(doctorId);
        Department dept = doctor != null ? departmentRepository.findById(doctor.getDepartmentId()) : null;
        String deptName = dept != null ? dept.getDepartmentName() : null;

        Set<String> patientIds = list.stream().map(Appointment::getPatientId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Patient> ptMap = patientIds.isEmpty()
                ? Collections.emptyMap()
                : patientRepository.findByIds(patientIds).stream()
                        .collect(Collectors.toMap(Patient::getPatientId, p -> p, (a, b) -> a));

        return list.stream().map(a -> mapAppointmentWithPatients(a, ptMap, deptName)).collect(Collectors.toList());
    }

    @GetMapping("/{doctorId}/history")
    public List<Map<String, Object>> getHistory(@PathVariable String doctorId) {
        return queueRepository.findHistoryForDoctor(doctorId).stream().map(h -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", h.getHistoryId());
            map.put("queueNumber", h.getQueueNumber());
            map.put("patientId", h.getPatientId());
            map.put("status", h.getStatus());
            map.put("reason", h.getChangeReason());
            map.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{doctorId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String doctorId) {
        Doctor doc = doctorRepository.findById(doctorId);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Doctor not found"));
        }
        Department dept = departmentRepository.findById(doc.getDepartmentId());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", doc.getDoctorId());
        res.put("name", doc.getName());
        res.put("doctorCode", doc.getDoctorCode());
        res.put("department", dept != null ? dept.getDepartmentName() : "General");
        res.put("departmentId", doc.getDepartmentId());
        res.put("specialization", doc.getSpecialization() != null ? doc.getSpecialization() : "General Physician");
        res.put("phone", doc.getPhone() != null ? doc.getPhone() : "09 12345678");
        res.put("email", doc.getEmail() != null ? doc.getEmail() : "doctor@hospital.com");
        res.put("available", doc.isAvailable());
        res.put("averageConsultationMinutes", doc.getAverageConsultationMinutes());
        res.put("maxQueueSize", doc.getMaxQueueSize());
        res.put("queueOpenTime", doc.getQueueOpenTime() != null ? doc.getQueueOpenTime().toString() : "09:00");
        res.put("queueCloseTime", doc.getQueueCloseTime() != null ? doc.getQueueCloseTime().toString() : "17:00");
        return ResponseEntity.ok(res);
    }

    /**
     * Shared queue -> DTO mapping. All lookups (patient/doctor/department) are
     * passed in already-resolved rather than fetched here, so callers dealing
     * with a whole list (all for the same doctor) fetch the doctor/department
     * once and the patients in one batch instead of per queue row.
     */
    private Map<String, Object> mapQueueCore(Queue q, Patient pt, Doctor doc, Department dept) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getQueueId());
        map.put("queueNumber", q.getQueueNumber());
        map.put("patientId", q.getPatientId());
        map.put("patientName", pt != null ? pt.getName() : "Patient");
        map.put("doctorId", q.getDoctorId());
        map.put("doctorName", doc != null ? doc.getName() : "Doctor");
        map.put("departmentId", doc != null ? doc.getDepartmentId() : q.getDepartmentId());
        map.put("departmentName", dept != null ? dept.getDepartmentName() : "General Medicine");
        map.put("position", q.getPosition());
        map.put("status", q.getStatus());
        map.put("priority", q.getPriority());
        map.put("type", q.isEmergency() ? "EMERGENCY"
                : ("APPOINTMENT".equalsIgnoreCase(q.getPriority()) ? "APPOINTMENT" : "NORMAL"));
        map.put("emergency", q.isEmergency());
        map.put("estimatedWaitingMinutes", q.getEstimatedWaitingTime());
        map.put("waitingMinutes", q.getEstimatedWaitingTime());
        map.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().toString() : "");
        return map;
    }

    /**
     * Single-item convenience overload (e.g. mapping the one queue entry just
     * called).
     */
    private Map<String, Object> mapQueue(Queue q) {
        Patient pt = patientRepository.findById(q.getPatientId());
        Doctor doc = doctorRepository.findById(q.getDoctorId());
        Department dept = doc != null ? departmentRepository.findById(doc.getDepartmentId()) : null;
        return mapQueueCore(q, pt, doc, dept);
    }

    /**
     * Batched overload for lists of queue entries that all belong to the same
     * doctor (every doctor-facing endpoint here) — the doctor and department are
     * resolved once by the caller instead of once per queue row.
     */
    private Map<String, Object> mapQueueWithPatients(Queue q, Map<String, Patient> ptMap, Doctor doc, Department dept) {
        Patient pt = ptMap != null ? ptMap.get(q.getPatientId()) : null;
        return mapQueueCore(q, pt, doc, dept);
    }

    private Map<String, Object> mapAppointmentWithPatients(Appointment a, Map<String, Patient> ptMap, String deptName) {
        Patient pt = ptMap != null ? ptMap.get(a.getPatientId()) : patientRepository.findById(a.getPatientId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getAppointmentId());
        map.put("patientId", a.getPatientId());
        map.put("patientName", pt != null ? pt.getName() : "Patient");
        map.put("doctorId", a.getDoctorId());
        map.put("departmentName", deptName != null ? deptName : "General");
        map.put("appointmentDate", a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "");
        map.put("appointmentTime", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "");
        map.put("status", a.getStatus());
        map.put("notes", a.getNotes());
        return map;
    }
}
