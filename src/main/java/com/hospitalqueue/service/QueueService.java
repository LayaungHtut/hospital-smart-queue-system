package com.hospitalqueue.service;

import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.ml.WaitTimePredictionService;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.repository.SymptomRepository;
import com.hospitalqueue.rule.DoctorAvailabilityRule;
import com.hospitalqueue.rule.EmergencyRule;
import com.hospitalqueue.rule.QueueRule;
import com.hospitalqueue.util.QueueNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Smart Queue Engine service - the core business logic of the system.
 */
@Service
public class QueueService {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    private final QueueRepository queueRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    @SuppressWarnings("unused")
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final WaitingTimeService waitingTimeService;
    private final NotificationService notificationService;
    private final QueueRule queueRule;
    private final EmergencyRule emergencyRule;
    private final DoctorAvailabilityRule doctorAvailabilityRule;
    @SuppressWarnings("unused")
    private final WaitTimePredictionService waitTimePredictionService;
    private final SymptomRepository symptomRepository;
    private final JdbcTemplate jdbcTemplate;

    public QueueService(QueueRepository queueRepository,
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            PatientRepository patientRepository,
            WaitingTimeService waitingTimeService,
            NotificationService notificationService,
            QueueRule queueRule,
            EmergencyRule emergencyRule,
            DoctorAvailabilityRule doctorAvailabilityRule,
            WaitTimePredictionService waitTimePredictionService,
            SymptomRepository symptomRepository,
            JdbcTemplate jdbcTemplate) {
        this.queueRepository = queueRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.patientRepository = patientRepository;
        this.waitingTimeService = waitingTimeService;
        this.notificationService = notificationService;
        this.queueRule = queueRule;
        this.emergencyRule = emergencyRule;
        this.doctorAvailabilityRule = doctorAvailabilityRule;
        this.waitTimePredictionService = waitTimePredictionService;
        this.symptomRepository = symptomRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reads a HH:mm time setting from {@code system_setting}, falling back when
     * the row is missing/blank or unparsable - used for the hospital-wide
     * registration window and break window (Admin > Queue Settings).
     */
    private LocalTime readSettingTime(String key, LocalTime fallback) {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM system_setting WHERE setting_key = ?", String.class, key);
            return value == null || value.isBlank() ? fallback : LocalTime.parse(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Doctor availability detection: true when the doctor can currently accept new
     * patients.
     */
    public boolean isDoctorAvailable(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return false;
        }
        int waitingCount = queueRepository.countWaitingByDoctor(doctorId);
        return doctorAvailabilityRule.isAvailable(doctor, waitingCount, LocalTime.now());
    }

    /**
     * Marks a list of doctors with their computed availability and sorts available
     * first.
     */
    public List<Doctor> enrichAvailability(List<Doctor> doctors) {
        Map<String, Integer> waitingCounts = queueRepository.countWaitingByAllDoctors();
        return doctorAvailabilityRule.markAndSortAvailable(doctors, waitingCounts, LocalTime.now());
    }

    public Queue createQueue(String patientId, String doctorId, int departmentId, String priority, boolean emergency,
            String source) {
        return createQueue(patientId, doctorId, departmentId, priority, emergency, source,
                java.util.Collections.emptyList());
    }

    /**
     * Creates a new queue for a patient applying Rule 1 (one active queue),
     * Rule 5 (department validation), Rule 8 (registration window) and
     * Rule 11 (capacity). Generates the queue number and waiting time.
     */
    public synchronized Queue createQueue(String patientId, String doctorId, int departmentId, String priority,
            boolean emergency,
            String source, List<Integer> symptomIds) {
        com.hospitalqueue.model.Patient patient = patientRepository.findById(patientId);
        if (patient == null) {
            throw new IllegalStateException("Patient not found. Please log in again.");
        }
        // Use the canonical patient_id (findById tolerates lookup by phone/email/etc.)
        patientId = patient.getPatientId();

        Queue activeQueue = queueRepository.findActiveQueueByPatientId(patientId);
        if (queueRule.hasActiveQueue(activeQueue)) {
            throw new IllegalStateException("Rule 1: you already have an active queue. Cancel it first.");
        }

        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            throw new IllegalStateException("Doctor not found. Please choose a doctor.");
        }
        if (!doctor.isAvailable()) {
            throw new IllegalStateException("This doctor is currently unavailable. Please choose another doctor.");
        }
        if (doctor.getDepartmentId() != departmentId) {
            throw new IllegalStateException("Rule 5: the selected doctor does not belong to the chosen department.");
        }

        LocalTime open = doctor.getQueueOpenTime() != null ? doctor.getQueueOpenTime() : LocalTime.of(9, 0);
        LocalTime close = doctor.getQueueCloseTime() != null ? doctor.getQueueCloseTime() : LocalTime.of(16, 30);
        LocalTime now = LocalTime.now();

        // Enforce hospital operating hours for non-emergencies: the hospital-wide
        // registration window (Admin > Queue Settings, default 09:00-16:30)
        // intersected with the doctor's own working hours, and never during the
        // configured break window (default 12:00-13:00).
        if (!emergency) {
            LocalTime registrationStart = readSettingTime("registration_start_time", LocalTime.of(9, 0));
            LocalTime registrationEnd = readSettingTime("registration_end_time", LocalTime.of(16, 30));
            LocalTime effectiveOpen = open.isAfter(registrationStart) ? open : registrationStart;
            LocalTime effectiveClose = close.isBefore(registrationEnd) ? close : registrationEnd;

            if (now.isBefore(effectiveOpen) || now.isAfter(effectiveClose)) {
                throw new IllegalStateException("Rule 8: hospital queues are accepted between " + effectiveOpen
                        + " and " + effectiveClose
                        + ". Please register during operating hours or select Emergency if urgent.");
            }

            LocalTime breakStart = readSettingTime("break_start_time", LocalTime.of(12, 0));
            LocalTime breakEnd = readSettingTime("break_end_time", LocalTime.of(13, 0));
            if (!now.isBefore(breakStart) && now.isBefore(breakEnd)) {
                throw new IllegalStateException("Rule 8: queue registration is paused during the break (" + breakStart
                        + " - " + breakEnd + "). Please try again after the break, or select Emergency if urgent.");
            }
        }

        // Calculate dynamic session capacity based on actual session duration and
        // average consultation time (using historical data when available)
        long avgConsultMinutes = waitingTimeService.getDoctorAverageConsultationMinutes(doctor);
        long sessionMinutes = java.time.Duration.between(open, close).toMinutes();
        if (sessionMinutes <= 0)
            sessionMinutes = 450; // 7.5 hours default

        // Subtract the 1-hour lunch break from effective session time
        long effectiveSessionMinutes = Math.max(60, sessionMinutes - 60);

        // Calculate max capacity: effective session time / average consultation time
        long calculatedMaxCapacity = Math.max(5, effectiveSessionMinutes / Math.max(5, avgConsultMinutes));

        // Use the smaller of the doctor's configured max and the calculated capacity
        long maxQueueSize = doctor.getMaxQueueSize() > 0
                ? Math.min(doctor.getMaxQueueSize(), calculatedMaxCapacity)
                : calculatedMaxCapacity;

        int waitingCount = queueRepository.countWaitingByDoctor(doctorId);
        if (waitingCount >= maxQueueSize) {
            throw new IllegalStateException("The queue for " + doctor.getName()
                    + " has reached today's session limit (" + maxQueueSize
                    + " patients based on " + avgConsultMinutes + "-min avg consultation). "
                    + "Please choose another doctor or try again later.");
        }

        String queueNumber = generateQueueNumber(doctorId);
        long waitingTime = waitingTimeService.getPredictedWaitTime(doctorId);

        Queue queue = new Queue(queueNumber, patientId, doctorId, departmentId, priority, Queue.STATUS_WAITING);
        queue.setPosition(waitingCount + 1);
        queue.setEstimatedWaitingTime(waitingTime);
        queue.setSource(source != null ? source : Queue.SOURCE_ONLINE);
        queue.setEmergency(emergency);
        queue.setEmergencyConfirmed(false);
        queue.setCreatedAt(LocalDateTime.now());
        queue.setCheckedInAt(LocalDateTime.now());

        queueRepository.insert(queue);

        // Link symptoms to queue (many-to-many)
        if (symptomIds != null && !symptomIds.isEmpty()) {
            symptomRepository.linkQueueSymptoms(queue.getQueueId(), symptomIds);
        }

        notificationService.notify(patientId,
                "Your queue number " + queueNumber + " has been created for " + doctor.getName()
                        + ". Estimated wait: " + waitingTime + " minutes.");

        if (emergency) {
            notificationService.notify(patientId,
                    "Your case has been flagged as an emergency and is awaiting staff confirmation for priority.");
        }
        return queue;
    }

    /**
     * Rule 9: checks in a patient who has an appointment today with appointment
     * priority.
     */
    public Queue checkInAppointment(String patientId) {
        Queue activeQueue = queueRepository.findActiveQueueByPatientId(patientId);
        if (queueRule.hasActiveQueue(activeQueue)) {
            throw new IllegalStateException("Rule 1: you already have an active queue. Cancel it first.");
        }

        Appointment appointment = appointmentRepository.findTodaysAppointment(patientId);
        if (!queueRule.hasTodaysAppointment(appointment)) {
            throw new IllegalStateException("Rule 9: no appointment found for today.");
        }

        Queue queue = createQueue(patientId, appointment.getDoctorId(), appointment.getDepartmentId(),
                Queue.PRIORITY_APPOINTMENT, false, Queue.SOURCE_ONLINE, List.of());
        appointmentRepository.updateStatus(appointment.getAppointmentId(), Appointment.STATUS_CHECKED_IN,
                queue.getQueueId());
        return queue;
    }

    public boolean cancelQueue(String patientId) {
        Queue activeQueue = queueRepository.findActiveQueueByPatientId(patientId);
        if (!queueRule.hasActiveQueue(activeQueue)) {
            throw new IllegalStateException("No active queue to cancel.");
        }
        queueRepository.cancel(activeQueue.getQueueId(), "Patient cancelled");
        notificationService.notify(patientId,
                "Your queue number " + activeQueue.getQueueNumber() + " has been cancelled.");
        return true;
    }

    public boolean expireQueue(long queueId) {
        Queue queue = queueRepository.findById(queueId);
        if (queue == null) {
            return false;
        }
        queueRepository.expire(queueId);
        notificationService.notify(queue.getPatientId(),
                "Your queue number " + queue.getQueueNumber()
                        + " has expired because you missed your turn. Please register again.");
        return true;
    }

    public Queue getActiveQueue(String patientId) {
        return queueRepository.findActiveQueueByPatientId(patientId);
    }

    /**
     * Doctor: call the next patient using priority ordering (Rule 2, 4).
     * The patient is marked CALLED until the doctor starts the consultation.
     * Also updates estimated waiting times for remaining patients.
     */
    public Queue callNext(String doctorId) {
        if (currentConsultation(doctorId) != null) {
            throw new IllegalStateException(
                    "Finish or start the current consultation before calling the next patient.");
        }
        List<Queue> waiting = queueRepository.findWaitingQueuesByDoctor(doctorId);
        if (waiting.isEmpty()) {
            return null;
        }
        Queue next = emergencyRule.reorderByPriority(waiting).get(0);
        queueRepository.callNext(next.getQueueId());
        notificationService.notify(next.getPatientId(),
                "It's your turn! Queue number " + next.getQueueNumber()
                        + " is now being served. Please proceed to the doctor.");

        // Update estimated waiting times for remaining patients
        updateRemainingWaitTimes(doctorId);

        return next;
    }

    /**
     * Updates estimated waiting times for all remaining WAITING patients
     * for a given doctor, based on current queue position and historical data.
     */
    private void updateRemainingWaitTimes(String doctorId) {
        try {
            Doctor doctor = doctorRepository.findById(doctorId);
            if (doctor == null)
                return;
            long avgConsult = waitingTimeService.getDoctorAverageConsultationMinutes(doctor);
            List<Queue> remaining = queueRepository.findWaitingQueuesByDoctor(doctorId);
            java.util.Map<Long, Long> waitByQueueId = new java.util.LinkedHashMap<>();
            for (int i = 0; i < remaining.size(); i++) {
                Queue q = remaining.get(i);
                int patientsAhead = i; // patients ahead of this one (0-indexed)
                long estimatedWait = patientsAhead * avgConsult;
                // Add break offset if needed
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalTime completion = now.plusMinutes(estimatedWait);
                if (now.isBefore(java.time.LocalTime.of(13, 0))
                        && completion.isAfter(java.time.LocalTime.of(12, 0))) {
                    estimatedWait += 60; // lunch break
                }
                waitByQueueId.put(q.getQueueId(), Math.max(0, estimatedWait));
            }
            // One batched round trip instead of one UPDATE per waiting patient -
            // this runs on every call-next/complete action, so it's a hot path.
            queueRepository.updateEstimatedWaitingTimes(waitByQueueId);
        } catch (Exception e) {
            // Non-critical: log but don't fail the call-next operation
            org.slf4j.LoggerFactory.getLogger(QueueService.class)
                    .warn("Failed to update remaining wait times for doctor {}", doctorId, e);
        }
    }

    /**
     * Doctor: start the consultation for the currently called patient.
     */
    public boolean startConsultation(String doctorId) {
        Queue called = called(doctorId);
        if (called == null) {
            return false;
        }
        queueRepository.start(called.getQueueId());
        notificationService.notify(called.getPatientId(),
                "Consultation has started for queue number " + called.getQueueNumber() + ".");
        return true;
    }

    /**
     * The patient currently called (waiting to start) for a doctor, if any.
     */
    public Queue called(String doctorId) {
        List<Queue> list = queueRepository.findCalledByDoctor(doctorId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * The active consultation for a doctor: a called patient not yet started,
     * or the patient currently being served.
     */
    public Queue currentConsultation(String doctorId) {
        Queue called = called(doctorId);
        if (called != null) {
            return called;
        }
        return serving(doctorId);
    }

    public boolean completeCurrent(String doctorId) {
        Queue serving = serving(doctorId);
        if (serving == null) {
            return false;
        }
        queueRepository.complete(serving.getQueueId());
        notificationService.notify(serving.getPatientId(),
                "Your consultation for queue number " + serving.getQueueNumber() + " has been completed. Thank you!");
        // Update estimated waiting times for remaining patients after completion
        updateRemainingWaitTimes(doctorId);
        return true;
    }

    public boolean pauseCurrent(String doctorId) {
        Queue serving = serving(doctorId);
        if (serving == null) {
            return false;
        }
        queueRepository.pause(serving.getQueueId());
        return true;
    }

    public boolean resumeCurrent(String doctorId) {
        Queue serving = serving(doctorId);
        if (serving == null) {
            return false;
        }
        queueRepository.resume(serving.getQueueId());
        return true;
    }

    public Queue serving(String doctorId) {
        List<Queue> list = queueRepository.findServingByDoctor(doctorId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Queue> waitingForDoctor(String doctorId) {
        return emergencyRule.reorderByPriority(queueRepository.findWaitingQueuesByDoctor(doctorId));
    }

    public void confirmEmergency(long queueId) {
        queueRepository.confirmEmergency(queueId);
        Queue q = queueRepository.findById(queueId);
        if (q != null) {
            notificationService.notify(q.getPatientId(),
                    "Your emergency has been confirmed. You have been moved to the front of the queue.");
        }
    }

    public void reassignDoctor(long queueId, String doctorId) {
        queueRepository.reassignDoctor(queueId, doctorId);
    }

    private String generateQueueNumber(String doctorId) {
        String doctorCode = doctorRepository.findById(doctorId).getDoctorCode();
        String prefix = QueueNumberGenerator.buildPrefix(doctorCode);
        String latest = queueRepository.findLatestQueueNumber(prefix);
        int lastSequence = QueueNumberGenerator.extractLastSequence(latest);
        return QueueNumberGenerator.generateQueueNumber(doctorCode, lastSequence);
    }
}
