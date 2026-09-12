package com.hospitalqueue.service;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.PatientRegistrationRequest;
import com.hospitalqueue.model.Staff;
import com.hospitalqueue.repository.AdminUserRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRegistrationRequestRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.StaffRepository;
import com.hospitalqueue.util.IDGenerator;
import com.hospitalqueue.util.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final PatientRegistrationRequestRepository registrationRequestRepository;
    private final DoctorRepository doctorRepository;
    private final StaffRepository staffRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AuthService(PatientRepository patientRepository,
            PatientRegistrationRequestRepository registrationRequestRepository,
            DoctorRepository doctorRepository,
            StaffRepository staffRepository,
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService) {
        this.patientRepository = patientRepository;
        this.registrationRequestRepository = registrationRequestRepository;
        this.doctorRepository = doctorRepository;
        this.staffRepository = staffRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    public Patient loginPatient(String phone, String password) {
        if (phone == null || phone.isBlank() || password == null) {
            return null;
        }
        List<Patient> patients = patientRepository.findAllByPhone(phone.trim());
        for (Patient p : patients) {
            if (passwordEncoder.matches(password, p.getPasswordHash())) {
                return p;
            }
        }
        // Fallback for ID-based login
        Patient byId = patientRepository.findById(phone.trim());
        if (byId != null && passwordEncoder.matches(password, byId.getPasswordHash())) {
            return byId;
        }
        return null;
    }

    public String checkPatientRegistrationStatus(String phone) {
        if (phone == null || phone.isBlank())
            return null;
        PatientRegistrationRequest req = registrationRequestRepository.findLatestByPhone(phone.trim());
        if (req != null) {
            return req.getStatus();
        }
        return null;
    }

    public Doctor loginDoctor(String doctorId, String password) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null || doctor.getPasswordHash() == null
                || !passwordEncoder.matches(password, doctor.getPasswordHash())) {
            return null;
        }
        return doctor;
    }

    public Staff loginStaff(String identifier, String password) {
        if (identifier == null || identifier.isBlank())
            return null;
        Staff staff = staffRepository.findById(identifier.trim());
        if (staff == null || !passwordEncoder.matches(password, staff.getPasswordHash())) {
            return null;
        }
        return staff;
    }

    public AdminUser loginAdmin(String username, String password) {
        AdminUser admin = adminUserRepository.findByUsername(username == null ? null : username.trim());
        if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            return null;
        }
        return admin;
    }

    /**
     * Submits a patient registration request awaiting Admin approval.
     * Allows up to 3 household members under the same phone number.
     */
    public PatientRegistrationRequest requestPatientRegistration(String name, String phone, String password,
            String email,
            String address, String dateOfBirth, String gender) {
        if (!Validator.isNotEmpty(name)) {
            throw new IllegalArgumentException("Full name is required.");
        }
        String phoneError = Validator.getPhoneValidationError(phone);
        if (phoneError != null) {
            throw new IllegalArgumentException(phoneError);
        }
        String normalizedPhone = phone.trim().replaceAll("[\\s\\-()]", "");
        String passwordError = Validator.getPasswordValidationError(password);
        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }
        if (!Validator.isNotEmpty(dateOfBirth)) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (!Validator.isNotEmpty(gender)) {
            throw new IllegalArgumentException("Gender is required.");
        }

        if (address != null && !address.isBlank() && !Validator.isValidMyanmarAddress(address)) {
            throw new IllegalArgumentException("If provided, address must be within Myanmar and include region, town/city, and street.");
        }

        // Limit up to 3 people registered under the same phone number
        int existingCount = patientRepository.countByPhone(normalizedPhone);
        if (existingCount >= 3) {
            throw new IllegalStateException("Maximum limit of 3 registered accounts per phone number reached for this household.");
        }

        // Check if exact same name already registered under this phone
        List<Patient> existingPatients = patientRepository.findAllByPhone(normalizedPhone);
        for (Patient p : existingPatients) {
            if (p.getName().trim().equalsIgnoreCase(name.trim())) {
                throw new IllegalStateException("A family member named '" + name.trim() + "' is already registered under this phone number. Please log in.");
            }
        }

        PatientRegistrationRequest req = new PatientRegistrationRequest();
        req.setName(name.trim());
        req.setPhone(normalizedPhone);
        req.setPasswordHash(passwordEncoder.encode(password));
        req.setEmail(
                email != null && !email.isBlank() ? email.trim() : (normalizedPhone.replaceAll("[^0-9]", "") + "@hospital.com"));
        req.setAddress(address != null && !address.isBlank() ? address.trim() : "");
        req.setGender(gender.trim());
        req.setStatus(PatientRegistrationRequest.STATUS_PENDING);
        try {
            req.setDateOfBirth(java.time.LocalDate.parse(dateOfBirth));
        } catch (Exception ignored) {
        }

        registrationRequestRepository.insert(req);
        return req;
    }

    /**
     * Direct registration method (legacy / admin direct creation).
     */
    public synchronized Patient registerPatient(String name, String phone, String password, String email,
            String address, String dateOfBirth, String gender) {
        if (!Validator.isNotEmpty(name)) {
            throw new IllegalArgumentException("Full name is required.");
        }
        String phoneError = Validator.getPhoneValidationError(phone);
        if (phoneError != null) {
            throw new IllegalArgumentException(phoneError);
        }
        String normalizedPhone = phone.trim().replaceAll("[\\s\\-()]", "");
        String passwordError = Validator.getPasswordValidationError(password);
        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }

        if (address != null && !address.isBlank() && !Validator.isValidMyanmarAddress(address)) {
            throw new IllegalArgumentException("If provided, address must be within Myanmar and include region, town/city, and street.");
        }

        int existingCount = patientRepository.countByPhone(normalizedPhone);
        if (existingCount >= 3) {
            throw new IllegalStateException("Maximum limit of 3 registered accounts per phone number reached.");
        }

        int lastSequence = patientRepository.count();
        Patient patient = new Patient();
        patient.setPatientId(IDGenerator.generatePatientId(lastSequence));
        patient.setName(name.trim());
        patient.setPhone(normalizedPhone);
        patient.setPasswordHash(passwordEncoder.encode(password));
        patient.setEmail(email != null && !email.isBlank() ? email.trim() : (normalizedPhone.replaceAll("[^0-9]", "") + "@hospital.com"));
        patient.setAddress(address != null && !address.isBlank() ? address.trim() : "");
        patient.setGender(gender != null && !gender.isBlank() ? gender.trim() : "Other");
        patient.setNewPatient(true);
        patient.setRegisteredAt(LocalDateTime.now());
        if (Validator.isNotEmpty(dateOfBirth)) {
            try {
                patient.setDateOfBirth(java.time.LocalDate.parse(dateOfBirth));
            } catch (Exception ignored) {
            }
        }

        patientRepository.insert(patient);
        notificationService.notify(patient.getPatientId(),
                "Welcome to the Hospital Smart Queue System, " + patient.getName()
                        + "! Your account has been created.");
        return patient;
    }

    /**
     * Admin approves a patient registration request -> generates Patient ID and
     * activates account.
     */
    public synchronized Patient approvePatientRegistration(long requestId, String adminUsername) {
        PatientRegistrationRequest req = registrationRequestRepository.findById(requestId);
        if (req == null) {
            throw new IllegalStateException("Registration request not found.");
        }
        if (!PatientRegistrationRequest.STATUS_PENDING.equals(req.getStatus())) {
            throw new IllegalStateException("Registration request is already " + req.getStatus().toLowerCase() + ".");
        }

        List<Patient> existingWithPhone = patientRepository.findAllByPhone(req.getPhone());
        for (Patient p : existingWithPhone) {
            if (p.getName().trim().equalsIgnoreCase(req.getName().trim())) {
                registrationRequestRepository.updateStatus(requestId, PatientRegistrationRequest.STATUS_APPROVED,
                        adminUsername, "Already registered");
                return p;
            }
        }
        if (existingWithPhone.size() >= 3) {
            throw new IllegalStateException("Maximum limit of 3 registered accounts per phone number reached.");
        }

        int lastSequence = patientRepository.count();
        Patient patient = new Patient();
        patient.setPatientId(IDGenerator.generatePatientId(lastSequence));
        patient.setName(req.getName());
        patient.setPhone(req.getPhone());
        patient.setPasswordHash(req.getPasswordHash());
        patient.setEmail(req.getEmail());
        patient.setAddress(req.getAddress());
        patient.setGender(req.getGender());
        patient.setDateOfBirth(req.getDateOfBirth());
        patient.setNewPatient(true);
        patient.setRegisteredAt(LocalDateTime.now());

        patientRepository.insert(patient);
        registrationRequestRepository.updateStatus(requestId, PatientRegistrationRequest.STATUS_APPROVED, adminUsername,
                null);

        notificationService.notify(patient.getPatientId(),
                "Your registration has been approved by the Administrator! Welcome to Hospital Smart Queue System, "
                        + patient.getName() + ".");

        return patient;
    }

    /**
     * Admin rejects a patient registration request.
     */
    public void rejectPatientRegistration(long requestId, String adminUsername, String reason) {
        PatientRegistrationRequest req = registrationRequestRepository.findById(requestId);
        if (req == null) {
            throw new IllegalStateException("Registration request not found.");
        }
        registrationRequestRepository.updateStatus(requestId, PatientRegistrationRequest.STATUS_REJECTED, adminUsername,
                reason);
    }

    public List<PatientRegistrationRequest> getAllRegistrationRequests() {
        return registrationRequestRepository.findAll();
    }

    public List<PatientRegistrationRequest> getPendingRegistrationRequests() {
        return registrationRequestRepository.findPending();
    }
}
