package com.hospitalqueue.api;

import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.repository.StaffRepository;
import com.hospitalqueue.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
public class StaffApiController {

    private final QueueRepository queueRepository;
    private final QueueService queueService;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;

    public StaffApiController(QueueRepository queueRepository,
            QueueService queueService,
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            StaffRepository staffRepository) {
        this.queueRepository = queueRepository;
        this.queueService = queueService;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.staffRepository = staffRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Long> stats = queueRepository.getDashboardStatistics();
        long totalToday = stats.getOrDefault("totalToday", 0L);
        long emergencyPending = stats.getOrDefault("emergencyPending", 0L);
        long waiting = stats.getOrDefault("waiting", 0L);
        long completed = stats.getOrDefault("completed", 0L);
        long emergCount = stats.getOrDefault("emergToday", 0L);
        long apptCount = stats.getOrDefault("apptToday", 0L);
        long normCount = stats.getOrDefault("normToday", 0L);

        List<Doctor> doctors = doctorRepository.findAll();
        long activeDoctors = doctors.stream().filter(d -> d.isAvailable()).count();
        long onBreakDoctors = doctors.size() - activeDoctors;

        long sum = Math.max(1, emergCount + apptCount + normCount);

        Map<String, Object> emergItem = new LinkedHashMap<>();
        emergItem.put("label", "Emergency");
        emergItem.put("value", emergCount);
        emergItem.put("percent", (int) Math.round((emergCount * 100.0) / sum));
        emergItem.put("color", "var(--color-status-emergency)");
        emergItem.put("color", "#EF4444");

        Map<String, Object> apptItem = new LinkedHashMap<>();
        apptItem.put("label", "Appointment");
        apptItem.put("value", apptCount);
        apptItem.put("percent", (int) Math.round((apptCount * 100.0) / sum));
        apptItem.put("color", "var(--color-status-appointment)");
        apptItem.put("color", "#0EA5E9");

        Map<String, Object> normItem = new LinkedHashMap<>();
        normItem.put("label", "Normal");
        normItem.put("value", normCount);
        normItem.put("percent", (int) Math.round((normCount * 100.0) / sum));
        normItem.put("color", "var(--color-status-normal)");
        normItem.put("color", "#10B981");

        Map<String, Object> h1 = new LinkedHashMap<>();
        h1.put("label", "Queues Completed");
        h1.put("value", completed);

        Map<String, Object> h2 = new LinkedHashMap<>();
        h2.put("label", "Patients Waiting");
        h2.put("value", waiting);

        double avgWait = queueRepository.getAverageWaitingTimeToday();

        Map<String, Object> h3 = new LinkedHashMap<>();
        h3.put("label", "Average Waiting Time");
        h3.put("value", (int) Math.round(avgWait));

        Map<String, Object> h4 = new LinkedHashMap<>();
        h4.put("label", "Doctor On Break");
        h4.put("value", onBreakDoctors);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalQueuesToday", totalToday);
        data.put("emergencyPending", emergencyPending);
        data.put("activeDoctors", activeDoctors);
        data.put("checkedInAppointments", apptCount);
        data.put("breakdown", List.of(emergItem, apptItem, normItem));
        data.put("highlights", List.of(h1, h2, h3, h4));

        return data;
    }

    @GetMapping("/reports")
    public Map<String, Object> getReports(@RequestParam(required = false) String type,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = null;
        if (date != null && !date.isBlank()) {
            try {
                targetDate = LocalDate.parse(date);
            } catch (Exception ignored) {
            }
        }
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        List<Map<String, Object>> rows = queueRepository.getDepartmentReports(targetDate);

        long totalQueues = rows.stream().mapToLong(r -> ((Number) r.get("totalQueues")).longValue()).sum();
        long completed = rows.stream().mapToLong(r -> ((Number) r.get("completed")).longValue()).sum();
        long cancelled = rows.stream().mapToLong(r -> ((Number) r.get("cancelled")).longValue()).sum();
        long missed = rows.stream().mapToLong(r -> ((Number) r.get("missed")).longValue()).sum();

        return Map.of(
                "date", targetDate.toString(),
                "totalQueues", totalQueues,
                "completed", completed,
                "cancelled", cancelled,
                "missed", missed,
                "rows", rows);
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestParam(required = false) String staffId) {
        Staff staff = null;
        if (staffId != null) {
            staff = staffRepository.findById(staffId);
        }
        if (staff == null) {
            List<Staff> list = staffRepository.findAll();
            if (!list.isEmpty())
                staff = list.get(0);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", staff != null ? staff.getStaffId() : "STF001");
        map.put("staffCode", staff != null ? staff.getStaffCode() : "STF001");
        map.put("name", staff != null ? staff.getName() : "Staff Member");
        map.put("role", staff != null ? staff.getRole() : "Receptionist");
        map.put("department", "Reception / Triage");
        map.put("phone", staff != null ? staff.getPhone() : "09 987654321");
        map.put("email", staff != null && staff.getEmail() != null ? staff.getEmail() : "staff@hospital.com");
        map.put("workingShift", "8:00 AM - 4:00 PM");
        map.put("joiningDate", "12 Mar 2023");
        map.put("avatarUrl", "");
        return map;
    }

    @GetMapping("/emergencies")
    public List<Map<String, Object>> getEmergencies() {
        List<Queue> emergencies = queueRepository.findPendingEmergencies();
        if (emergencies.isEmpty())
            return Collections.emptyList();

        Map<String, Patient> ptMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getPatientId(), p -> p, (a, b) -> a));

        return emergencies.stream().map(q -> {
            Patient pt = ptMap.get(q.getPatientId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", q.getQueueId());
            map.put("patientCode",
                    pt != null ? pt.getPatientId() : (q.getPatientId() != null ? q.getPatientId() : "P001"));
            map.put("patientId", q.getPatientId());
            map.put("patientName", pt != null ? pt.getName() : "Emergency Patient");
            map.put("symptoms", "Emergency Case - " + q.getQueueNumber());
            map.put("requestedTime", q.getCreatedAt() != null ? q.getCreatedAt().toLocalTime().toString() : "10:15 AM");
            map.put("priority", "HIGH");
            map.put("resolved", q.isEmergencyConfirmed());
            map.put("handledBy", q.isEmergencyConfirmed() ? "Staff on Duty" : null);
            map.put("confirmedTime", q.isEmergencyConfirmed() ? "Confirmed" : null);
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/emergencies/{queueId}/confirm")
    public ResponseEntity<?> confirmEmergency(@PathVariable long queueId) {
        queueService.confirmEmergency(queueId);
        return ResponseEntity.ok(Map.of(
                "id", queueId,
                "resolved", true,
                "handledBy", "Staff",
                "confirmedTime", "Just now"));
    }

    @PostMapping("/emergencies/{queueId}/reject")
    public ResponseEntity<?> rejectEmergency(@PathVariable long queueId) {
        queueRepository.cancel(queueId, "Emergency rejected by staff");
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/queue-monitor")
    public List<Map<String, Object>> getQueueMonitor(@RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String doctorName) {
        List<Doctor> doctors = queueService.enrichAvailability(doctorRepository.findAll());
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));
        Map<String, Queue> servingMap = queueRepository.findServingByAllDoctors();
        Map<String, Integer> waitingMap = queueRepository.countWaitingByAllDoctors();
        Map<String, Long> completedMap = queueRepository.countCompletedTodayByAllDoctors();

        return doctors.stream().map(doc -> {
            Department dept = deptMap.get(doc.getDepartmentId());
            String deptName = dept != null ? dept.getDepartmentName() : "General";
            Queue serving = servingMap.get(doc.getDoctorId());
            int waitingCount = waitingMap.getOrDefault(doc.getDoctorId(), 0);
            long completedCount = completedMap.getOrDefault(doc.getDoctorId(), 0L);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("departmentName", deptName);
            row.put("doctorName", doc.getName());
            row.put("status", doc.isAvailable() ? "CONSULTING" : "UNAVAILABLE");
            row.put("nowServing", serving != null ? serving.getQueueNumber() : null);
            row.put("waiting", waitingCount);
            row.put("completed", (int) completedCount);
            row.put("avgWaitingMinutes", (int) doc.getAverageConsultationMinutes());
            return row;
        }).filter(r -> (departmentName == null || "ALL".equalsIgnoreCase(departmentName)
                || departmentName.equalsIgnoreCase((String) r.get("departmentName")))
                && (doctorName == null || "ALL".equalsIgnoreCase(doctorName)
                        || doctorName.equalsIgnoreCase((String) r.get("doctorName"))))
                .collect(Collectors.toList());
    }

    @GetMapping("/queues/reassignable")
    public List<Map<String, Object>> getReassignableQueues(@RequestParam(required = false) String departmentName) {
        List<Queue> waiting = queueRepository.findAllWaiting();
        if (waiting.isEmpty())
            return Collections.emptyList();

        if (departmentName != null && !departmentName.isBlank() && !"ALL".equalsIgnoreCase(departmentName)) {
            Department targetDept = departmentRepository.findByName(departmentName);
            if (targetDept != null) {
                int targetDeptId = targetDept.getDepartmentId();
                waiting = waiting.stream().filter(q -> q.getDepartmentId() == targetDeptId)
                        .collect(Collectors.toList());
            }
        }

        Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDoctorId(), d -> d, (a, b) -> a));
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));
        Map<String, Patient> ptMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getPatientId(), p -> p, (a, b) -> a));

        return waiting.stream().map(q -> {
            Doctor doc = docMap.get(q.getDoctorId());
            Department dept = deptMap.get(q.getDepartmentId());
            Patient pt = ptMap.get(q.getPatientId());

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

    public static class ReassignDto {
        public String doctorId;
        public String reason;
    }

    @PostMapping("/queues/{queueId}/reassign")
    public ResponseEntity<?> reassignQueue(@PathVariable long queueId, @RequestBody ReassignDto req) {
        if (req == null || req.doctorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target doctor ID is required."));
        }
        queueService.reassignDoctor(queueId, req.doctorId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    public static class DoctorUnavailableDto {
        public String reason;
    }

    @PostMapping("/doctors/{doctorId}/unavailable")
    public ResponseEntity<?> setDoctorUnavailable(@PathVariable String doctorId,
            @RequestBody(required = false) DoctorUnavailableDto req) {
        Doctor doc = doctorRepository.findById(doctorId);
        if (doc != null) {
            doctorRepository.setAvailability(doctorId, false);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/appointments")
    public List<Map<String, Object>> getAllAppointments() {
        List<Appointment> list = appointmentRepository.findAll();
        if (list.isEmpty())
            return Collections.emptyList();

        Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDoctorId(), d -> d, (a, b) -> a));
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getDepartmentId(), d -> d, (a, b) -> a));
        Map<String, Patient> ptMap = patientRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getPatientId(), p -> p, (a, b) -> a));

        return list.stream().map(a -> {
            Doctor doc = docMap.get(a.getDoctorId());
            Department dept = deptMap.get(a.getDepartmentId());
            Patient pt = ptMap.get(a.getPatientId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getAppointmentId());
            map.put("patientId", a.getPatientId());
            map.put("patientName", pt != null ? pt.getName() : "Patient");
            map.put("doctorId", a.getDoctorId());
            map.put("doctorName", doc != null ? doc.getName() : "Dr. Specialist");
            map.put("departmentName", dept != null ? dept.getDepartmentName() : "General");
            map.put("appointmentDate", a.getAppointmentDate() != null ? a.getAppointmentDate().toString() : "");
            map.put("appointmentTime", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "");
            map.put("status", a.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> getNotifications() {
        return List.of(
                Map.of("id", 1, "title", "Emergency case added", "message",
                        "Emergency patient waiting for triage confirmation.", "time", "10:15 AM", "read", false,
                        "important", true, "category", "EMERGENCY"),
                Map.of("id", 2, "title", "Queue reassigned", "message", "Queue reassigned successfully.", "time",
                        "10:08 AM", "read", false, "important", false, "category", "QUEUE"),
                Map.of("id", 3, "title", "Doctor on break", "message", "Dr. Khin Khin (Pediatrics) is now on break.",
                        "time", "09:50 AM", "read", false, "important", false, "category", "DOCTOR"),
                Map.of("id", 4, "title", "Schedule updated", "message", "Doctor schedules have been updated.", "time",
                        "Yesterday", "read", true, "important", true, "category", "SCHEDULE"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsRead() {
        return ResponseEntity.ok(Map.of("success", true));
    }
}
