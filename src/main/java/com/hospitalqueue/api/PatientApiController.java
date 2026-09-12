package com.hospitalqueue.api;

import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Notification;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.NotificationService;
import com.hospitalqueue.service.QueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
public class PatientApiController {

    private final PatientRepository patientRepository;
    private final QueueRepository queueRepository;
    private final QueueService queueService;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public PatientApiController(PatientRepository patientRepository,
            QueueRepository queueRepository,
            QueueService queueService,
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            AppointmentRepository appointmentRepository,
            NotificationService notificationService) {
        this.patientRepository = patientRepository;
        this.queueRepository = queueRepository;
        this.queueService = queueService;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/{patientId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String patientId) {
        Patient patient = patientRepository.findById(patientId);
        if (patient == null) {
            patient = patientRepository.findByPhone(patientId);
        }
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Patient not found"));
        }
        return ResponseEntity.ok(mapPatientProfile(patient));
    }

    @PutMapping("/{patientId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable String patientId, @RequestBody Map<String, Object> req) {
        Patient patient = patientRepository.findById(patientId);
        if (patient == null) {
            patient = patientRepository.findByPhone(patientId);
        }
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Patient not found"));
        }

        String name = (String) req.get("name");
        String email = (String) req.get("email");
        String address = (String) req.get("address");
        String gender = (String) req.get("gender");
        String dateOfBirth = (String) req.get("dateOfBirth");

        if (name != null && !name.isBlank())
            patient.setName(name.trim());
        if (email != null)
            patient.setEmail(email.trim());
        if (address != null && !address.isBlank()) {
            if (!com.hospitalqueue.util.Validator.isValidMyanmarAddress(address)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "If provided, address must be within Myanmar and include region, town/city, and street."));
            }
            patient.setAddress(address.trim());
        } else if (address != null && address.isBlank()) {
            patient.setAddress("");
        }
        if (gender != null)
            patient.setGender(gender.trim());
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            try {
                patient.setDateOfBirth(LocalDate.parse(dateOfBirth));
            } catch (Exception ignored) {
            }
        }

        patientRepository.update(patient);
        return ResponseEntity.ok(mapPatientProfile(patient));
    }

    @GetMapping("/{patientId}/queue")
    public List<Map<String, Object>> getQueues(@PathVariable String patientId) {
        List<Queue> queues = queueRepository.findQueuesByPatientId(patientId);
        if (queues.isEmpty())
            return Collections.emptyList();

        Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDoctorId(), d -> d, (a, b) -> a));
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));
        Patient pt = patientRepository.findById(patientId);

        return queues.stream().map(q -> {
            Doctor doc = docMap.get(q.getDoctorId());
            Department dept = deptMap.get(q.getDepartmentId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", q.getQueueId());
            map.put("queueNumber", q.getQueueNumber());
            map.put("patientId", q.getPatientId());
            map.put("patientName", pt != null ? pt.getName() : "Patient");
            map.put("departmentId", q.getDepartmentId());
            map.put("departmentName", dept != null ? dept.getDepartmentName() : "General");
            map.put("doctorId", q.getDoctorId());
            map.put("doctorName", doc != null ? doc.getName() : "Dr. Specialist");
            map.put("position", q.getPosition());
            map.put("estimatedWaitingMinutes", q.getEstimatedWaitingTime());
            map.put("waitingMinutes", q.getEstimatedWaitingTime());
            map.put("status", q.getStatus());
            map.put("type", q.isEmergency() ? "EMERGENCY"
                    : ("APPOINTMENT".equalsIgnoreCase(q.getPriority()) ? "APPOINTMENT" : "NORMAL"));
            map.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().toString() : "");
            return map;
        }).collect(Collectors.toList());
    }

    public static class CreateQueueDto {
        public String patientId;
        public List<Integer> symptomIds;
        public int departmentId;
        public String doctorId;
        public boolean emergency;
    }

    @PostMapping("/queue")
    public ResponseEntity<?> createQueue(@RequestBody CreateQueueDto req) {
        if (req == null || req.patientId == null || req.doctorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Patient ID and Doctor ID are required."));
        }

        String priority = req.emergency ? Queue.PRIORITY_EMERGENCY : Queue.PRIORITY_NORMAL;
        try {
            Queue queue = queueService.createQueue(
                    req.patientId,
                    req.doctorId,
                    req.departmentId,
                    priority,
                    req.emergency,
                    Queue.SOURCE_ONLINE,
                    req.symptomIds);
            return ResponseEntity.ok(mapQueueEntry(queue));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/queue/{queueId}/cancel")
    public ResponseEntity<?> cancelQueue(@PathVariable long queueId, @RequestParam(required = false) String patientId) {
        Queue queue = queueRepository.findById(queueId);
        if (queue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Queue not found"));
        }
        try {
            queueRepository.cancel(queueId, "Patient cancelled");
            notificationService.notify(queue.getPatientId(),
                    "Your queue number " + queue.getQueueNumber() + " has been cancelled.");
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{patientId}/appointments")
    public List<Map<String, Object>> getAppointments(@PathVariable String patientId) {
        List<Appointment> list = appointmentRepository.findByPatientId(patientId);
        if (list.isEmpty())
            return Collections.emptyList();

        Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDoctorId(), d -> d, (a, b) -> a));
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));

        return list.stream().map(a -> {
            Doctor doc = docMap.get(a.getDoctorId());
            Department dept = deptMap.get(a.getDepartmentId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getAppointmentId());
            map.put("patientId", a.getPatientId());
            map.put("doctorId", a.getDoctorId());
            map.put("doctorName", doc != null ? doc.getName() : "Dr. Specialist");
            map.put("departmentName", dept != null ? dept.getDepartmentName() : "General");
            map.put("appointmentDate", a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "");
            map.put("appointmentTime", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "");
            map.put("status", a.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{patientId}/notifications")
    public List<Map<String, Object>> getNotifications(@PathVariable String patientId) {
        List<Notification> list = notificationService.getNotifications(patientId);
        return list.stream().map(n -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getNotificationId());
            map.put("title", n.getMessage().length() > 25 ? n.getMessage().substring(0, 25) + "..." : n.getMessage());
            map.put("message", n.getMessage());
            map.put("time", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "Just now");
            map.put("read", n.isRead());
            map.put("important", n.getMessage().toLowerCase().contains("emergency")
                    || n.getMessage().toLowerCase().contains("turn"));
            map.put("category", n.getMessage().toLowerCase().contains("emergency") ? "EMERGENCY" : "QUEUE");
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/{patientId}/notifications/read-all")
    public ResponseEntity<?> markNotificationsRead(@PathVariable String patientId) {
        notificationService.markAllRead(patientId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Map<String, Object> mapPatientProfile(Patient p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getPatientId());
        map.put("name", p.getName());
        map.put("phone", p.getPhone());
        map.put("dateOfBirth", p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");
        map.put("gender", p.getGender() != null ? p.getGender() : "");
        map.put("address", p.getAddress() != null ? p.getAddress() : "");
        return map;
    }

    private Map<String, Object> mapQueueEntry(Queue q) {
        Doctor doc = doctorRepository.findById(q.getDoctorId());
        Department dept = departmentRepository.findById(q.getDepartmentId());
        Patient pt = patientRepository.findById(q.getPatientId());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getQueueId());
        map.put("queueNumber", q.getQueueNumber());
        map.put("patientId", q.getPatientId());
        map.put("patientName", pt != null ? pt.getName() : "Patient");
        map.put("departmentId", q.getDepartmentId());
        map.put("departmentName", dept != null ? dept.getDepartmentName() : "General");
        map.put("doctorId", q.getDoctorId());
        map.put("doctorName", doc != null ? doc.getName() : "Dr. Specialist");
        map.put("position", q.getPosition());
        map.put("estimatedWaitingMinutes", q.getEstimatedWaitingTime());
        map.put("waitingMinutes", q.getEstimatedWaitingTime());
        map.put("status", q.getStatus());
        map.put("type", q.isEmergency() ? "EMERGENCY"
                : ("APPOINTMENT".equalsIgnoreCase(q.getPriority()) ? "APPOINTMENT" : "NORMAL"));
        map.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().toString() : "");
        return map;
    }
}
