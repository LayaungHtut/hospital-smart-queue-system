package com.hospitalqueue.api;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.PatientRegistrationRequest;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    public static class LoginRequest {
        public String username;
        public String password;
        public String role;
    }

    public static class RegisterRequest {
        public String name;
        public String phone;
        public String password;
        public String dateOfBirth;
        public String gender;
        public String email;
        public String address;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.role == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid login request"));
        }

        String role = req.role.toUpperCase();
        String identifier = req.username != null ? req.username.trim() : "";
        String password = req.password != null ? req.password : "";

        if ("PATIENT".equals(role)) {
            Patient patient = authService.loginPatient(identifier, password);
            if (patient == null) {
                String regStatus = authService.checkPatientRegistrationStatus(identifier);
                if (PatientRegistrationRequest.STATUS_PENDING.equals(regStatus)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error",
                            "Your account registration is currently pending administrator approval. Please wait for an admin to activate your account."));
                } else if (PatientRegistrationRequest.STATUS_REJECTED.equals(regStatus)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error",
                            "Your registration request was rejected by the administrator. Please contact hospital reception."));
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid phone number or password"));
            }
            return ResponseEntity.ok(Map.of(
                    "token", "pt-" + UUID.randomUUID(),
                    "userId", patient.getPatientId(),
                    "name", patient.getName(),
                    "role", "PATIENT"));
        } else if ("DOCTOR".equals(role)) {
            Doctor doctor = authService.loginDoctor(identifier, password);
            if (doctor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid doctor ID or password"));
            }
            return ResponseEntity.ok(Map.of(
                    "token", "doc-" + UUID.randomUUID(),
                    "userId", doctor.getDoctorId(),
                    "name", doctor.getName(),
                    "role", "DOCTOR"));
        } else if ("STAFF".equals(role)) {
            Staff staff = authService.loginStaff(identifier, password);
            if (staff == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid phone number or password"));
            }
            return ResponseEntity.ok(Map.of(
                    "token", "stf-" + UUID.randomUUID(),
                    "userId", staff.getStaffId(),
                    "name", staff.getName(),
                    "role", "STAFF"));
        } else if ("ADMIN".equals(role)) {
            AdminUser admin = authService.loginAdmin(identifier, password);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password"));
            }
            return ResponseEntity.ok(Map.of(
                    "token", "adm-" + UUID.randomUUID(),
                    "userId", admin.getAdminId(),
                    "name", admin.getName(),
                    "role", "ADMIN"));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Unknown role: " + role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req == null || req.phone == null || req.phone.isBlank() || req.name == null || req.name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and phone number are required."));
        }

        String password = req.password != null && !req.password.isBlank() ? req.password : "123456";
        String email = req.email != null && !req.email.isBlank() ? req.email
                : (req.phone.replaceAll("[^0-9]", "") + "@hospital.com");
        String address = req.address != null ? req.address : "Default Address";

        try {
            PatientRegistrationRequest request = authService.requestPatientRegistration(
                    req.name,
                    req.phone,
                    password,
                    email,
                    address,
                    req.dateOfBirth,
                    req.gender);

            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please check your registration details."));
            }

            return ResponseEntity.ok(Map.of(
                    "id", request.getRequestId(),
                    "name", request.getName(),
                    "phone", request.getPhone(),
                    "status", request.getStatus(),
                    "message",
                    "Registration request submitted. An administrator will review and activate your account."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
