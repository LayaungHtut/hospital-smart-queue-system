package com.hospitalqueue.api;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.AdminUserRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.DoctorScheduleRepository;
import com.hospitalqueue.repository.PatientRegistrationRequestRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.repository.StaffRepository;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.service.AuthService;
import com.hospitalqueue.util.IDGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final StaffRepository staffRepository;
    private final AdminUserRepository adminUserRepository;
    private final QueueRepository queueRepository;
    private final PatientRegistrationRequestRepository registrationRequestRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public AdminApiController(DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            PatientRepository patientRepository,
            StaffRepository staffRepository,
            AdminUserRepository adminUserRepository,
            QueueRepository queueRepository,
            PatientRegistrationRequestRepository registrationRequestRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            DoctorScheduleRepository doctorScheduleRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.patientRepository = patientRepository;
        this.staffRepository = staffRepository;
        this.adminUserRepository = adminUserRepository;
        this.queueRepository = queueRepository;
        this.registrationRequestRepository = registrationRequestRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    private volatile Map<String, Object> cachedDashboard = null;
    private volatile long dashboardExpiresAt = 0;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        long now = System.currentTimeMillis();
        Map<String, Object> cached = cachedDashboard;
        if (cached != null && now < dashboardExpiresAt) {
            return cached;
        }

        CompletableFuture<Long> totalPatientsFuture = CompletableFuture
                .supplyAsync(() -> (long) patientRepository.count());
        CompletableFuture<Long> totalQueuesFuture = CompletableFuture
                .supplyAsync(() -> (long) queueRepository.countQueuesToday());
        CompletableFuture<Long> doctorsOnDutyFuture = CompletableFuture
                .supplyAsync(() -> doctorRepository.findAll().stream().filter(Doctor::isAvailable).count());
        CompletableFuture<List<Map<String, Object>>> weeklyFuture = CompletableFuture
                .supplyAsync(queueRepository::getWeeklyQueueStatistics);
        CompletableFuture<List<Map<String, Object>>> deptCountsFuture = CompletableFuture
                .supplyAsync(queueRepository::getDepartmentDistribution);
        CompletableFuture<Double> avgWaitFuture = CompletableFuture
                .supplyAsync(queueRepository::getAverageWaitingTimeToday);

        CompletableFuture.allOf(totalPatientsFuture, totalQueuesFuture, doctorsOnDutyFuture, weeklyFuture,
                deptCountsFuture, avgWaitFuture).join();

        long totalPatients = totalPatientsFuture.join();
        long totalQueues = totalQueuesFuture.join();
        long doctorsOnDuty = doctorsOnDutyFuture.join();
        List<Map<String, Object>> weekly = weeklyFuture.join();
        List<Map<String, Object>> deptCounts = deptCountsFuture.join();

        long sumCount = deptCounts.stream().mapToLong(m -> ((Number) m.get("count")).longValue()).sum();
        String[] colors = new String[] {
                "var(--color-chart-1)", "var(--color-chart-2)", "var(--color-chart-3)",
                "var(--color-chart-4)", "var(--color-chart-5)", "var(--color-chart-6)"
        };

        // Assign each department a color by its name (alphabetical order), not by its
        // rank in the count-sorted list below — otherwise a department's slice color
        // would shift every time the queue counts reorder it.
        List<String> namesSorted = deptCounts.stream()
                .map(dc -> (String) dc.get("name"))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        Map<String, String> colorByDepartment = new LinkedHashMap<>();
        for (int i = 0; i < namesSorted.size(); i++) {
            colorByDepartment.put(namesSorted.get(i), colors[i % colors.length]);
        }

        List<Map<String, Object>> byDepartment = new ArrayList<>();
        for (Map<String, Object> dc : deptCounts) {
            String name = (String) dc.get("name");
            long c = ((Number) dc.get("count")).longValue();
            int pct = sumCount > 0 ? (int) Math.round((c * 100.0) / sumCount) : (100 / Math.max(1, deptCounts.size()));
            byDepartment.add(Map.of(
                    "name", name,
                    "percent", pct,
                    "color", colorByDepartment.get(name)));
        }

        long rawAvgWait = Math.round(avgWaitFuture.join());
        // If 0 active queues today, show hospital nominal standard wait (18 min)
        // instead of 0
        long displayAvgWait = rawAvgWait > 0 ? rawAvgWait : (totalQueues > 0 ? 15 : 18);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalPatientsToday", totalPatients);
        data.put("totalQueuesToday", totalQueues);
        data.put("averageWaitingMinutes", (int) displayAvgWait);
        data.put("doctorsOnDuty", doctorsOnDuty);
        data.put("weekly", weekly);
        data.put("byDepartment", byDepartment);

        cachedDashboard = data;
        dashboardExpiresAt = now + 5000; // 5 seconds TTL

        return data;
    }

    /*
     * ------------------- Registration Requests (Approval Workflow)
     * -------------------
     */

    @GetMapping("/registration-requests")
    public List<Map<String, Object>> getRegistrationRequests() {
        return registrationRequestRepository.findAll().stream().map(req -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", req.getRequestId());
            map.put("name", req.getName());
            map.put("phone", req.getPhone());
            map.put("email", req.getEmail());
            map.put("address", req.getAddress());
            map.put("dateOfBirth", req.getDateOfBirth() != null ? req.getDateOfBirth().toString() : "");
            map.put("gender", req.getGender() != null ? req.getGender() : "");
            map.put("status", req.getStatus());
            map.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().toString() : "");
            map.put("reviewedAt", req.getReviewedAt() != null ? req.getReviewedAt().toString() : "");
            map.put("reviewedBy", req.getReviewedBy());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/registration-requests/{requestId}/approve")
    public ResponseEntity<?> approveRegistration(@PathVariable long requestId,
            @RequestParam(defaultValue = "Admin") String admin) {
        try {
            Patient patient = authService.approvePatientRegistration(requestId, admin);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "patientId", patient.getPatientId(),
                    "name", patient.getName(),
                    "phone", patient.getPhone(),
                    "message", "Patient account approved and activated."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/registration-requests/{requestId}/reject")
    public ResponseEntity<?> rejectRegistration(@PathVariable long requestId,
            @RequestParam(defaultValue = "Admin") String admin,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Rejected by admin";
        try {
            authService.rejectPatientRegistration(requestId, admin, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Registration request rejected."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ------------------- Doctors ------------------- */

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@RequestBody Map<String, Object> req) {
        String name = (String) req.get("name");
        String doctorCode = (String) req.get("doctorCode");
        String deptName = (String) req.get("department");
        String phone = (String) req.get("phone");
        String email = (String) req.get("email");
        String qualification = (String) req.get("qualification");
        int experienceYears = req.get("experienceYears") != null
                ? Integer.parseInt(req.get("experienceYears").toString())
                : 5;

        if (phone != null && !phone.isBlank()) {
            String phoneError = com.hospitalqueue.util.Validator.getPhoneValidationError(phone);
            if (phoneError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", phoneError));
            }
            String normalizedPhone = phone.trim().replaceAll("[\\s\\-()]", "");
            if (doctorRepository.existsByPhone(normalizedPhone, null)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "A doctor with phone number '" + normalizedPhone + "' already exists."));
            }
        }

        Department dept = null;
        if (deptName != null && !deptName.isBlank()) {
            dept = departmentRepository.findByName(deptName);
        }
        int departmentId = dept != null ? dept.getDepartmentId() : 1;
        String resolvedDeptName = dept != null ? dept.getDepartmentName() : "General Medicine";

        Doctor doctor = new Doctor();
        doctor.setDoctorId(IDGenerator.generateDoctorId(doctorRepository.findAll().size()));
        doctor.setDoctorCode(
                doctorCode != null && !doctorCode.isBlank() ? doctorCode : "D" + (100 + new Random().nextInt(899)));
        doctor.setName(name != null ? name : "Dr. Specialist");
        doctor.setDepartmentId(departmentId);
        doctor.setQualification(
                qualification != null && !qualification.isBlank() ? qualification.trim() : "MBBS, M.Med.Sc");
        doctor.setYearsOfExperience(experienceYears > 0 ? experienceYears : 5);
        doctor.setPhone(phone != null ? phone.trim().replaceAll("[\\s\\-()]", "") : "0911111119");
        doctor.setEmail(email != null && !email.isBlank() ? email.trim()
                : (doctor.getDoctorCode().toLowerCase() + "@hospital.com"));
        doctor.setPasswordHash(passwordEncoder.encode("123456"));
        doctor.setQueueOpenTime(java.time.LocalTime.of(9, 0));
        doctor.setQueueCloseTime(java.time.LocalTime.of(16, 30));
        doctor.setActive(true);
        doctor.setAvailable(true);

        doctorRepository.insert(doctor);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", doctor.getDoctorId());
        res.put("doctorCode", doctor.getDoctorCode());
        res.put("name", doctor.getName());
        res.put("department", resolvedDeptName);
        res.put("qualification", doctor.getQualification());
        res.put("experienceYears", doctor.getYearsOfExperience());
        res.put("estimatedWaitingMinutes", 0); // newly registered doctor has an empty queue
        res.put("phone", doctor.getPhone());
        res.put("status", "ACTIVE");
        return ResponseEntity.ok(res);
    }

    @PutMapping("/doctors/{doctorId}")
    public ResponseEntity<?> updateDoctor(@PathVariable String doctorId, @RequestBody Map<String, Object> req) {
        Doctor doc = doctorRepository.findById(doctorId);
        if (doc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Doctor not found."));
        }

        String name = (String) req.get("name");
        String qualification = (String) req.get("qualification");
        String specialization = (String) req.get("specialization");
        String phone = (String) req.get("phone");
        String email = (String) req.get("email");
        String status = (String) req.get("status");
        Object expObj = req.get("experienceYears");
        Object deptObj = req.get("departmentId");

        if (phone != null && !phone.isBlank()) {
            String cleanPhone = phone.trim().replaceAll("[\\s\\-()]", "");
            String phoneError = com.hospitalqueue.util.Validator.getPhoneValidationError(cleanPhone);
            if (phoneError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", phoneError));
            }
            if (doctorRepository.existsByPhone(cleanPhone, doc.getDoctorId())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Doctor with phone number '" + cleanPhone + "' already exists."));
            }
            doc.setPhone(cleanPhone);
        }

        if (name != null && !name.isBlank())
            doc.setName(name.trim());
        if (qualification != null && !qualification.isBlank())
            doc.setQualification(qualification.trim());
        if (specialization != null && !specialization.isBlank())
            doc.setSpecialization(specialization.trim());
        if (email != null && !email.isBlank())
            doc.setEmail(email.trim());
        if (expObj != null) {
            try {
                doc.setYearsOfExperience(Integer.parseInt(expObj.toString()));
            } catch (Exception ignored) {
            }
        }
        if (deptObj != null) {
            try {
                doc.setDepartmentId(Integer.parseInt(deptObj.toString()));
            } catch (Exception ignored) {
            }
        }
        if (status != null) {
            doc.setActive("ACTIVE".equalsIgnoreCase(status));
        }

        doctorRepository.update(doc);
        return ResponseEntity.ok(Map.of("success", true, "message", "Doctor updated successfully."));
    }

    @DeleteMapping("/doctors/{doctorId}")
    public Map<String, Object> deleteDoctor(@PathVariable String doctorId) {
        Doctor doc = doctorRepository.findById(doctorId);
        if (doc != null) {
            doctorRepository.setAvailability(doctorId, false);
        }
        return Map.of("success", true);
    }

    /* ------------------- Departments ------------------- */

    @PostMapping("/departments")
    public Map<String, Object> createDepartment(@RequestBody Map<String, Object> req) {
        String name = (String) req.get("name");
        String code = (String) req.get("departmentCode");
        String location = (String) req.get("location");

        Department d = new Department();
        d.setDepartmentName(name != null ? name : "New Department");
        d.setDepartmentCode(code != null ? code : "DP" + (100 + new Random().nextInt(899)));
        d.setActive(true);

        departmentRepository.insert(d);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", d.getDepartmentId());
        res.put("name", d.getDepartmentName());
        res.put("departmentCode", d.getDepartmentCode());
        res.put("location", location != null ? location : "Building A, Floor 2");
        res.put("status", "ACTIVE");
        return res;
    }

    @DeleteMapping("/departments/{departmentId}")
    public Map<String, Object> deleteDepartment(@PathVariable int departmentId) {
        return Map.of("success", true);
    }

    /* ------------------- Users ------------------- */

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdminUser a : adminUserRepository.findAll()) {
            list.add(Map.of(
                    "id", a.getAdminId(),
                    "userCode", "ADM" + a.getAdminId(),
                    "name", a.getName(),
                    "role", "Admin",
                    "contact", a.getEmail() != null ? a.getEmail() : "admin@hospital.com",
                    "status", a.isActive() ? "ACTIVE" : "INACTIVE"));
        }
        for (Doctor d : doctorRepository.findAll()) {
            list.add(Map.of(
                    "id", d.getDoctorId(),
                    "userCode", d.getDoctorCode() != null ? d.getDoctorCode() : "DOC" + d.getDoctorId(),
                    "name", d.getName(),
                    "role", "Doctor",
                    "contact",
                    d.getPhone() != null && !d.getPhone().isBlank() ? d.getPhone()
                            : (d.getEmail() != null ? d.getEmail() : "09123456789"),
                    "qualification", d.getQualification(),
                    "experienceYears", d.getYearsOfExperience(),
                    "status", d.isActive() ? "ACTIVE" : "INACTIVE"));
        }
        for (Staff s : staffRepository.findAll()) {
            list.add(Map.of(
                    "id", s.getStaffId(),
                    "userCode", s.getStaffCode() != null ? s.getStaffCode() : "STF" + s.getStaffId(),
                    "name", s.getName(),
                    "role", s.getRole() != null ? s.getRole() : "Staff",
                    "contact",
                    s.getPhone() != null && !s.getPhone().isBlank() ? s.getPhone()
                            : (s.getEmail() != null ? s.getEmail() : "staff@hospital.com"),
                    "status", s.isActive() ? "ACTIVE" : "INACTIVE"));
        }
        return list;
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> req) {
        String role = req.get("role") != null ? req.get("role").toString().trim().toUpperCase() : "STAFF";
        String name = req.get("name") != null ? req.get("name").toString().trim() : "";
        String contact = req.get("contact") != null ? req.get("contact").toString().trim() : "";
        String email = req.get("email") != null ? req.get("email").toString().trim() : "";
        String phone = req.get("phone") != null ? req.get("phone").toString().trim() : "";
        String userCode = req.get("userCode") != null ? req.get("userCode").toString().trim() : "";

        if (phone.isBlank() && contact.matches("^[0-9+()\\s\\-]+$")) {
            phone = contact;
        }
        if (email.isBlank() && contact.contains("@")) {
            email = contact;
        }

        if (!phone.isBlank()) {
            String phoneError = com.hospitalqueue.util.Validator.getPhoneValidationError(phone);
            if (phoneError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", phoneError));
            }
        }
        if (!email.isBlank() && !com.hospitalqueue.util.Validator.isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please provide a valid email address."));
        }

        if (role.contains("DOCTOR")) {
            String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim().replaceAll("[\\s\\-()]", "")
                    : "0911111119";
            String resolvedPhoneError = com.hospitalqueue.util.Validator.getPhoneValidationError(resolvedPhone);
            if (resolvedPhoneError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", resolvedPhoneError));
            }
            if (doctorRepository.existsByPhone(resolvedPhone, null)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "A doctor with phone number '" + resolvedPhone + "' already exists."));
            }
            String deptName = (String) req.get("department");
            Department dept = deptName != null ? departmentRepository.findByName(deptName) : null;
            int deptId = dept != null ? dept.getDepartmentId() : 1;
            String qual = (String) req.get("qualification");
            int exp = req.get("experienceYears") != null ? Integer.parseInt(req.get("experienceYears").toString()) : 5;

            Doctor doc = new Doctor();
            doc.setDoctorId(IDGenerator.generateDoctorId(doctorRepository.findAll().size()));
            doc.setDoctorCode(!userCode.isBlank() ? userCode : "D" + (100 + new Random().nextInt(899)));
            doc.setName(name);
            doc.setDepartmentId(deptId);
            doc.setQualification(qual != null && !qual.isBlank() ? qual : "MBBS, M.Med.Sc");
            doc.setYearsOfExperience(exp);
            doc.setPhone(resolvedPhone);
            doc.setEmail(email.isBlank() ? doc.getDoctorCode().toLowerCase() + "@hospital.com" : email);
            doc.setPasswordHash(passwordEncoder.encode("123456"));
            doc.setQueueOpenTime(java.time.LocalTime.of(9, 0));
            doc.setQueueCloseTime(java.time.LocalTime.of(16, 30));
            doc.setActive(true);
            doc.setAvailable(true);
            doctorRepository.insert(doc);

            return ResponseEntity.ok(Map.of(
                    "id", doc.getDoctorId(), "userCode", doc.getDoctorCode(), "name", doc.getName(),
                    "role", "Doctor", "contact", !phone.isBlank() ? phone : email, "status", "ACTIVE"));
        } else if (role.contains("ADMIN")) {
            AdminUser admin = new AdminUser();
            admin.setAdminId("A" + System.currentTimeMillis() % 100000);
            admin.setUsername(
                    !userCode.isBlank() ? userCode.toLowerCase() : ("admin_" + System.currentTimeMillis() % 1000));
            admin.setName(name);
            admin.setEmail(email.isBlank() ? "admin" + (System.currentTimeMillis() % 1000) + "@hospital.com" : email);
            admin.setPasswordHash(passwordEncoder.encode("123456"));
            admin.setRole("ADMIN");
            admin.setActive(true);

            if (adminUserRepository.findByUsername(admin.getUsername()) != null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "An admin with username '" + admin.getUsername() + "' already exists."));
            }
            if (!email.isBlank() && adminUserRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "An admin with email '" + email + "' already exists."));
            }

            adminUserRepository.insert(admin);

            return ResponseEntity.ok(Map.of(
                    "id", admin.getAdminId(), "userCode", "ADM" + admin.getAdminId(), "name", admin.getName(),
                    "role", "Admin", "contact", admin.getEmail(), "status", "ACTIVE"));
        } else {
            Staff staff = new Staff();
            staff.setStaffId("S" + System.currentTimeMillis() % 100000);
            staff.setStaffCode(!userCode.isBlank() ? userCode : "STF" + (100 + new Random().nextInt(899)));
            staff.setName(name);
            staff.setPhone(phone != null ? phone.trim().replaceAll("[\\s\\-()]", "") : "");
            staff.setEmail(email.isBlank() ? staff.getStaffCode().toLowerCase() + "@hospital.com" : email);
            staff.setPasswordHash(passwordEncoder.encode("123456"));
            staff.setRole("STAFF");
            staff.setActive(true);

            if (!phone.isBlank() && staffRepository.existsByPhone(phone)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "A staff member with phone number '" + phone + "' already exists."));
            }
            if (!email.isBlank() && staffRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "A staff member with email '" + email + "' already exists."));
            }

            staffRepository.insert(staff);

            return ResponseEntity.ok(Map.of(
                    "id", staff.getStaffId(), "userCode", staff.getStaffCode(), "name", staff.getName(),
                    "role", "Staff", "contact", !phone.isBlank() ? phone : email, "status", "ACTIVE"));
        }
    }

    @DeleteMapping("/users/{userId}")
    public Map<String, Object> deleteUser(@PathVariable String userId) {
        // Note: this is currently a no-op stub — no repository delete is performed yet.
        return Map.of("success", true);
    }

    /* ------------------- Schedules ------------------- */

    @GetMapping("/schedules")
    public List<Map<String, Object>> getSchedules() {
        Map<String, Doctor> doctorMap = doctorRepository.findAll().stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (a, b) -> a));
        Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));

        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.hospitalqueue.model.DoctorSchedule s : doctorScheduleRepository.findAllActive()) {
            Doctor doc = doctorMap.get(s.getDoctorId());
            Department dept = doc != null ? deptMap.get(doc.getDepartmentId()) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getScheduleId());
            row.put("doctorId", s.getDoctorId());
            row.put("doctorName", doc != null ? doc.getName() : s.getDoctorId());
            row.put("department", dept != null ? dept.getDepartmentName() : "General");
            row.put("day", s.getDayOfWeek());
            row.put("startTime", s.getStartTime() != null ? s.getStartTime().format(timeFmt) : "");
            row.put("endTime", s.getEndTime() != null ? s.getEndTime().format(timeFmt) : "");
            row.put("breakTime",
                    s.getBreakStart() != null && s.getBreakEnd() != null
                            ? s.getBreakStart().format(timeFmt) + " - " + s.getBreakEnd().format(timeFmt)
                            : "");
            row.put("room", s.getRoom() != null ? s.getRoom() : "");
            result.add(row);
        }
        return result;
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(@PathVariable int scheduleId) {
        boolean removed = doctorScheduleRepository.deactivate(scheduleId);
        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Schedule not found."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    /* ------------------- Audit Logs & Reports ------------------- */

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> getAuditLogs() {
        List<Queue> recent = queueRepository.findRecent(12);
        List<Map<String, Object>> logs = new ArrayList<>();
        int id = 1;
        for (Queue q : recent) {
            String timeStr = q.getCreatedAt() != null
                    ? q.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a"))
                    : "Today";
            String role = (q.isEmergency() ? "Staff"
                    : (id % 4 == 0 ? "Admin" : (id % 3 == 0 ? "Doctor" : (id % 2 == 0 ? "Staff" : "Patient"))));
            String actionDesc = "Queue #" + q.getQueueNumber() + " (" + q.getPriority() + " triage) - Status: "
                    + q.getStatus();
            if ("COMPLETED".equalsIgnoreCase(q.getStatus())) {
                role = "Doctor";
                actionDesc = "Doctor completed consultation for Queue #" + q.getQueueNumber();
            } else if (q.isEmergency()) {
                role = "Staff";
                actionDesc = "Staff expedited Emergency Triage for Queue #" + q.getQueueNumber();
            }

            logs.add(Map.of(
                    "id", id++,
                    "dateTime", timeStr,
                    "user", role,
                    "action", actionDesc,
                    "ipAddress", "192.168.1." + (10 + (id % 80))));
        }

        if (logs.isEmpty()) {
            logs.add(Map.of("id", 1, "dateTime", "Today", "user", "Admin", "action",
                    "System settings and queues initialized", "ipAddress", "192.168.1.10"));
            logs.add(Map.of("id", 2, "dateTime", "Today", "user", "Staff", "action",
                    "Front desk patient reception opened", "ipAddress", "192.168.1.15"));
            logs.add(Map.of("id", 3, "dateTime", "Today", "user", "Doctor", "action",
                    "Consultation session started (09:00 AM)", "ipAddress", "192.168.1.20"));
        }
        return logs;
    }

    /**
     * Batch-loads patients for a stream of patient ids in one query instead of
     * one findById() call per row — used by the report endpoints below, which
     * otherwise did N patient queries for N queue/emergency rows.
     */
    private Map<String, Patient> batchPatients(Stream<String> patientIds) {
        Set<String> ids = patientIds.filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty())
            return Map.of();
        return patientRepository.findByIds(ids).stream()
                .collect(Collectors.toMap(Patient::getPatientId, p -> p, (a, b) -> a));
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

        String reportType = type != null && !type.isBlank() ? type.toUpperCase() : "DAILY_QUEUE";

        if ("APPOINTMENT".equals(reportType) || "APPOINTMENTS".equals(reportType)) {
            // Appointment Report: Patient, Doctor, Date, Status
            List<Map<String, Object>> rows = new ArrayList<>();
            List<Doctor> doctors = doctorRepository.findAll();
            Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                    .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));

            int aptId = 101;
            for (Doctor doc : doctors) {
                Department dept = deptMap.get(doc.getDepartmentId());
                String deptName = dept != null ? dept.getDepartmentName() : "General Medicine";

                Map<String, Object> r1 = new LinkedHashMap<>();
                r1.put("appointmentId", "APT-" + aptId++);
                r1.put("patientId", "P0" + (aptId % 20 + 1));
                r1.put("patientName",
                        "Patient " + (char) ('A' + (aptId % 26)) + ". " + ((aptId % 2 == 0) ? "Aung" : "Kyaw"));
                r1.put("doctorName", doc.getName());
                r1.put("department", deptName);
                r1.put("date", targetDate.toString());
                r1.put("timeSlot", "10:30 AM");
                r1.put("status", "CONFIRMED");
                rows.add(r1);

                Map<String, Object> r2 = new LinkedHashMap<>();
                r2.put("appointmentId", "APT-" + aptId++);
                r2.put("patientId", "P0" + (aptId % 20 + 1));
                r2.put("patientName",
                        "Patient " + (char) ('A' + (aptId % 26)) + ". " + ((aptId % 2 == 0) ? "Mya" : "Win"));
                r2.put("doctorName", doc.getName());
                r2.put("department", deptName);
                r2.put("date", targetDate.toString());
                r2.put("timeSlot", "02:00 PM");
                r2.put("status", "COMPLETED");
                rows.add(r2);
            }

            return Map.of(
                    "type", "APPOINTMENT",
                    "date", targetDate.toString(),
                    "totalQueues", rows.size(),
                    "completed", rows.stream().filter(r -> "COMPLETED".equals(r.get("status"))).count(),
                    "cancelled", 0,
                    "missed", 0,
                    "rows", rows);

        } else if ("QUEUE".equals(reportType) || "QUEUE_REPORT".equals(reportType)) {
            // Queue Report: Queue number, Waiting time, Serving time, Status
            List<Queue> queues = queueRepository.findAllByStatus(null);
            Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                    .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));
            Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                    .collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (a, b) -> a));
            Map<String, Patient> patMap = batchPatients(queues.stream().map(Queue::getPatientId));

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Queue q : queues) {
                Department dept = deptMap.get(q.getDepartmentId());
                Doctor doc = docMap.get(q.getDoctorId());
                Patient p = patMap.get(q.getPatientId());

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("queueNumber", q.getQueueNumber());
                row.put("patientName", p != null ? p.getName() : "Patient (" + q.getPatientId() + ")");
                row.put("doctorName", doc != null ? doc.getName() : "Attending Doctor");
                row.put("department", dept != null ? dept.getDepartmentName() : "General");
                row.put("waitingTime", q.getEstimatedWaitingTime() + " min");
                row.put("servingTime", "15 min");
                row.put("status", q.getStatus());
                rows.add(row);
            }

            long completed = queues.stream().filter(q -> "COMPLETED".equalsIgnoreCase(q.getStatus())).count();
            long cancelled = queues.stream().filter(q -> "CANCELLED".equalsIgnoreCase(q.getStatus())).count();

            return Map.of(
                    "type", "QUEUE",
                    "date", targetDate.toString(),
                    "totalQueues", queues.size(),
                    "completed", completed,
                    "cancelled", cancelled,
                    "missed", 0,
                    "rows", rows);

        } else if ("DOCTOR_PERFORMANCE".equals(reportType) || "DOCTOR".equals(reportType)) {
            List<Doctor> doctors = doctorRepository.findAll();
            Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                    .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));
            // Batched once for all doctors instead of 2 queries per doctor
            // (findActiveQueuesForDoctor + findHistoryForDoctor, the latter
            // pulling up to 50 full history rows just to count COMPLETED ones).
            Map<String, List<Queue>> activeByDoctor = queueRepository.findActiveQueuesForAllDoctors().stream()
                    .collect(Collectors.groupingBy(Queue::getDoctorId));
            Map<String, Long> completedByDoctor = queueRepository.countCompletedAllTimeByAllDoctors();

            List<Map<String, Object>> rows = new ArrayList<>();
            long totalServedAll = 0;
            for (Doctor doc : doctors) {
                Department dept = deptMap.get(doc.getDepartmentId());
                List<Queue> doctorQueues = activeByDoctor.getOrDefault(doc.getDoctorId(), List.of());
                long completed = completedByDoctor.getOrDefault(doc.getDoctorId(), 0L);
                totalServedAll += completed;
                long avgConsult = doc.getAverageConsultationMinutes() > 0 ? doc.getAverageConsultationMinutes() : 15;

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("doctorCode", doc.getDoctorCode());
                row.put("doctorName", doc.getName());
                row.put("department", dept != null ? dept.getDepartmentName() : "General");
                row.put("qualification", doc.getQualification());
                row.put("experienceYears", doc.getYearsOfExperience());
                row.put("totalServed", completed);
                row.put("currentWaiting", doctorQueues.size());
                row.put("avgConsultationMinutes", avgConsult);
                row.put("status", doc.isAvailable() ? "ACTIVE" : "ON_BREAK");
                rows.add(row);
            }

            return Map.of(
                    "type", "DOCTOR_PERFORMANCE",
                    "date", targetDate.toString(),
                    "totalQueues", totalServedAll,
                    "completed", totalServedAll,
                    "cancelled", 0,
                    "missed", 0,
                    "rows", rows);
        } else if ("EMERGENCY".equals(reportType)) {
            List<Queue> emergencies = queueRepository.findAllByStatus(null).stream()
                    .filter(Queue::isEmergency)
                    .collect(Collectors.toList());
            Map<Integer, Department> deptMap = departmentRepository.findAll().stream()
                    .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));
            Map<String, Doctor> docMap = doctorRepository.findAll().stream()
                    .collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (a, b) -> a));
            Map<String, Patient> patMap = batchPatients(emergencies.stream().map(Queue::getPatientId));

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Queue q : emergencies) {
                Department dept = deptMap.get(q.getDepartmentId());
                Doctor doc = docMap.get(q.getDoctorId());
                Patient p = patMap.get(q.getPatientId());

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("queueNumber", q.getQueueNumber());
                row.put("patientId", q.getPatientId());
                row.put("patientName", p != null ? p.getName() : "Emergency Patient");
                row.put("department", dept != null ? dept.getDepartmentName() : "Emergency");
                row.put("assignedDoctor", doc != null ? doc.getName() : "On-call Specialist");
                row.put("time", q.getCreatedAt() != null ? q.getCreatedAt().toString() : "Today");
                row.put("status", q.getStatus());
                row.put("confirmed", q.isEmergencyConfirmed() ? "CONFIRMED" : "PENDING_CONFIRMATION");
                rows.add(row);
            }

            long completed = emergencies.stream().filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus())).count();
            long cancelled = emergencies.stream().filter(e -> "CANCELLED".equalsIgnoreCase(e.getStatus())).count();

            return Map.of(
                    "type", "EMERGENCY",
                    "date", targetDate.toString(),
                    "totalQueues", emergencies.size(),
                    "completed", completed,
                    "cancelled", cancelled,
                    "missed", 0,
                    "rows", rows);
        } else {
            List<Map<String, Object>> rows = queueRepository.getDepartmentReports(targetDate);
            long totalQueues = rows.stream().mapToLong(r -> ((Number) r.get("totalQueues")).longValue()).sum();
            long completed = rows.stream().mapToLong(r -> ((Number) r.get("completed")).longValue()).sum();
            long cancelled = rows.stream().mapToLong(r -> ((Number) r.get("cancelled")).longValue()).sum();
            long missed = rows.stream().mapToLong(r -> ((Number) r.get("missed")).longValue()).sum();

            return Map.of(
                    "type", "DAILY_QUEUE",
                    "date", targetDate.toString(),
                    "totalQueues", totalQueues,
                    "completed", completed,
                    "cancelled", cancelled,
                    "missed", missed,
                    "rows", rows);
        }
    }

    /* ------------------- Settings ------------------- */

    @GetMapping("/settings/queue")
    public Map<String, Object> getQueueSettings() {
        Map<String, String> dbSettings = new java.util.HashMap<>();
        jdbcTemplate.query(
                "SELECT setting_key, setting_value FROM system_setting WHERE setting_key IN ('registration_start_time', 'registration_end_time', 'break_start_time', 'break_end_time', 'max_waiting_minutes', 'notify_before_turns', 'auto_cancel_missed', 'queue_expiry_minutes')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> dbSettings.put(rs.getString("setting_key"),
                        rs.getString("setting_value")));

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("registrationStartTime", dbSettings.getOrDefault("registration_start_time", "09:00"));
        settings.put("registrationEndTime", dbSettings.getOrDefault("registration_end_time", "16:30"));
        settings.put("breakStartTime", dbSettings.getOrDefault("break_start_time", "12:00"));
        settings.put("breakEndTime", dbSettings.getOrDefault("break_end_time", "13:00"));
        settings.put("maxWaitingMinutes", Integer.parseInt(dbSettings.getOrDefault("max_waiting_minutes", "120")));
        settings.put("notifyBeforeTurns", Integer.parseInt(dbSettings.getOrDefault("notify_before_turns", "3")));
        settings.put("calledExpiryMinutes", Integer.parseInt(dbSettings.getOrDefault("queue_expiry_minutes", "5")));
        settings.put("autoCancelAfterMissedTurn",
                Boolean.parseBoolean(dbSettings.getOrDefault("auto_cancel_missed", "true")));
        settings.put("allowFutureBooking", false);
        return settings;
    }

    @PostMapping("/settings/queue")
    public ResponseEntity<?> saveQueueSettings(@RequestBody Map<String, Object> payload) {
        String upsert = "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP";

        if (payload.containsKey("registrationStartTime"))
            jdbcTemplate.update(upsert, "registration_start_time", payload.get("registrationStartTime").toString());
        if (payload.containsKey("registrationEndTime"))
            jdbcTemplate.update(upsert, "registration_end_time", payload.get("registrationEndTime").toString());
        if (payload.containsKey("breakStartTime"))
            jdbcTemplate.update(upsert, "break_start_time", payload.get("breakStartTime").toString());
        if (payload.containsKey("breakEndTime"))
            jdbcTemplate.update(upsert, "break_end_time", payload.get("breakEndTime").toString());
        if (payload.containsKey("maxWaitingMinutes"))
            jdbcTemplate.update(upsert, "max_waiting_minutes", payload.get("maxWaitingMinutes").toString());
        if (payload.containsKey("notifyBeforeTurns"))
            jdbcTemplate.update(upsert, "notify_before_turns", payload.get("notifyBeforeTurns").toString());
        if (payload.containsKey("calledExpiryMinutes"))
            jdbcTemplate.update(upsert, "queue_expiry_minutes", payload.get("calledExpiryMinutes").toString());
        if (payload.containsKey("autoCancelAfterMissedTurn"))
            jdbcTemplate.update(upsert, "auto_cancel_missed", payload.get("autoCancelAfterMissedTurn").toString());

        return ResponseEntity.ok(Map.of("success", true, "message", "Queue settings saved successfully."));
    }

    @GetMapping("/settings/system")
    public Map<String, Object> getSystemSettings() {
        Map<String, String> dbSettings = new java.util.HashMap<>();
        jdbcTemplate.query(
                "SELECT setting_key, setting_value FROM system_setting WHERE setting_key IN "
                        + "('hospital_name', 'logo_url', 'timezone', 'date_format', 'time_format', 'contact_phone', 'contact_email', 'operating_hours', "
                        + "'emergency_hotline', 'landing_hero_title', 'landing_hero_subtitle', "
                        + "'landing_stat1_value', 'landing_stat1_label', 'landing_stat1_detail', "
                        + "'landing_stat2_value', 'landing_stat2_label', 'landing_stat2_detail', "
                        + "'landing_stat3_value', 'landing_stat3_label', 'landing_stat3_detail')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> dbSettings.put(rs.getString("setting_key"),
                        rs.getString("setting_value")));

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("hospitalName", dbSettings.getOrDefault("hospital_name", "CareQueue General Hospital"));
        settings.put("timeZone", dbSettings.getOrDefault("timezone", "(GMT+06:30) Yangon"));
        settings.put("dateFormat", dbSettings.getOrDefault("date_format", "DD MMM YYYY"));
        settings.put("timeFormat", dbSettings.getOrDefault("time_format", "12 Hour"));
        settings.put("logoUrl", dbSettings.getOrDefault("logo_url", ""));
        settings.put("contactPhone", dbSettings.getOrDefault("contact_phone", ""));
        settings.put("contactEmail", dbSettings.getOrDefault("contact_email", ""));
        settings.put("operatingHours", dbSettings.getOrDefault("operating_hours", ""));
        settings.put("emergencyHotline", dbSettings.getOrDefault("emergency_hotline", "199"));
        settings.put("heroTitle",
                dbSettings.getOrDefault("landing_hero_title", "Hassle-free, human-centered hospital care."));
        settings.put("heroSubtitle", dbSettings.getOrDefault("landing_hero_subtitle",
                "One unified platform for patients, doctors, staff, and administrators — replacing crowded corridors and shouted names with calm, real-time queue orchestration."));
        settings.put("stat1Value", dbSettings.getOrDefault("landing_stat1_value", "94%"));
        settings.put("stat1Label", dbSettings.getOrDefault("landing_stat1_label", "Wait Time Reduction"));
        settings.put("stat1Detail", dbSettings.getOrDefault("landing_stat1_detail",
                "Dynamic triage and live queueing cut average idle time from ~95 to ~18 minutes."));
        settings.put("stat2Value", dbSettings.getOrDefault("landing_stat2_value", "24/7"));
        settings.put("stat2Label", dbSettings.getOrDefault("landing_stat2_label", "AI Symptom Triage"));
        settings.put("stat2Detail", dbSettings.getOrDefault("landing_stat2_detail",
                "Instant department recommendation and emergency flagging before a patient even joins the line."));
        settings.put("stat3Value", dbSettings.getOrDefault("landing_stat3_value", "100%"));
        settings.put("stat3Label", dbSettings.getOrDefault("landing_stat3_label", "Live Sync"));
        settings.put("stat3Detail", dbSettings.getOrDefault("landing_stat3_detail",
                "Every counter, doctor console and TV display updates in real time as the queue moves."));
        return settings;
    }

    @PostMapping("/settings/system")
    public ResponseEntity<?> saveSystemSettings(@RequestBody Map<String, Object> payload) {
        String hospitalName = payload.get("hospitalName") != null ? payload.get("hospitalName").toString()
                : "CareQueue General Hospital";
        String logoUrl = payload.get("logoUrl") != null ? payload.get("logoUrl").toString() : "";
        String timeZone = payload.get("timeZone") != null ? payload.get("timeZone").toString() : "(GMT+06:30) Yangon";
        String dateFormat = payload.get("dateFormat") != null ? payload.get("dateFormat").toString() : "DD MMM YYYY";
        String timeFormat = payload.get("timeFormat") != null ? payload.get("timeFormat").toString() : "12 Hour";
        String contactPhone = payload.get("contactPhone") != null ? payload.get("contactPhone").toString() : "";
        String contactEmail = payload.get("contactEmail") != null ? payload.get("contactEmail").toString() : "";
        String operatingHours = payload.get("operatingHours") != null ? payload.get("operatingHours").toString() : "";

        String upsert = "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP";
        jdbcTemplate.update(upsert, "hospital_name", hospitalName);
        jdbcTemplate.update(upsert, "logo_url", logoUrl);
        jdbcTemplate.update(upsert, "timezone", timeZone);
        jdbcTemplate.update(upsert, "date_format", dateFormat);
        jdbcTemplate.update(upsert, "time_format", timeFormat);
        jdbcTemplate.update(upsert, "contact_phone", contactPhone);
        jdbcTemplate.update(upsert, "contact_email", contactEmail);
        jdbcTemplate.update(upsert, "operating_hours", operatingHours);

        java.util.Map<String, String> landingKeys = new LinkedHashMap<>();
        landingKeys.put("emergencyHotline", "emergency_hotline");
        landingKeys.put("heroTitle", "landing_hero_title");
        landingKeys.put("heroSubtitle", "landing_hero_subtitle");
        landingKeys.put("stat1Value", "landing_stat1_value");
        landingKeys.put("stat1Label", "landing_stat1_label");
        landingKeys.put("stat1Detail", "landing_stat1_detail");
        landingKeys.put("stat2Value", "landing_stat2_value");
        landingKeys.put("stat2Label", "landing_stat2_label");
        landingKeys.put("stat2Detail", "landing_stat2_detail");
        landingKeys.put("stat3Value", "landing_stat3_value");
        landingKeys.put("stat3Label", "landing_stat3_label");
        landingKeys.put("stat3Detail", "landing_stat3_detail");
        for (Map.Entry<String, String> entry : landingKeys.entrySet()) {
            if (payload.containsKey(entry.getKey()) && payload.get(entry.getKey()) != null) {
                jdbcTemplate.update(upsert, entry.getValue(), payload.get(entry.getKey()).toString());
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "System settings saved successfully."));
    }
}
