package com.hospitalqueue.ml;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.QueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Predicts consultation duration using a statistical/heuristic model.
 * Factors: doctor's average speed, patient age, time of day, day of week,
 * symptom complexity, and historical completion times.
 */
@Service
public class ConsultationDurationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationDurationService.class);

    private final DoctorRepository doctorRepository;
    @SuppressWarnings("unused")
    private final AppointmentRepository appointmentRepository;
    private final QueueRepository queueRepository;

    public ConsultationDurationService(DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            QueueRepository queueRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueRepository = queueRepository;
    }

    /**
     * Predict consultation duration in minutes for a specific patient-doctor pair.
     */
    public double predictDuration(Doctor doctor, int patientAge, String symptoms,
            String appointmentType, boolean isNewPatient) {
        if (doctor == null) {
            return 15.0;
        }

        // 1. Start with doctor's historical average
        double baseMinutes = doctor.getAverageConsultationMinutes() > 0
                ? doctor.getAverageConsultationMinutes()
                : 15.0;

        // 2. Time-of-day adjustment (fatigue factor)
        double timeFactor = getTimeOfDayFactor();

        // 3. Day-of-week adjustment
        double dayFactor = getDayOfWeekFactor();

        // 4. Patient age adjustment (elderly + children take longer)
        double ageFactor = getAgeFactor(patientAge);

        // 5. Symptom complexity adjustment
        double complexityFactor = getSymptomComplexityFactor(symptoms);

        // 6. Appointment type adjustment
        double typeFactor = getTypeFactor(appointmentType);

        // 7. New patient takes longer
        double newPatientFactor = isNewPatient ? 1.25 : 1.0;

        // 8. Load-based adjustment (more patients waiting = faster consultations)
        int waitingCount = doctorRepository != null
                ? getDoctorWaitingCount(doctor.getDoctorId())
                : 0;
        double loadFactor = waitingCount > 5 ? 0.9 : 1.0;

        double predicted = baseMinutes * timeFactor * dayFactor * ageFactor
                * complexityFactor * typeFactor * newPatientFactor * loadFactor;

        // Clamp to reasonable range
        predicted = Math.max(5.0, Math.min(predicted, 60.0));

        log.debug(
                "Duration prediction for Dr. {}: base={} min, final={} min (factors: t={}, d={}, a={}, c={}, t={}, n={}, l={})",
                doctor.getDoctorId(), baseMinutes, predicted,
                timeFactor, dayFactor, ageFactor, complexityFactor, typeFactor, newPatientFactor, loadFactor);

        return Math.round(predicted * 10.0) / 10.0;
    }

    /**
     * Predict durations for all available doctors in a department.
     */
    public Map<String, Double> predictDurationsForDepartment(int departmentId, int patientAge,
            String symptoms, String appointmentType,
            boolean isNewPatient) {
        List<Doctor> doctors = doctorRepository.findByDepartment(departmentId);
        Map<String, Double> predictions = new HashMap<>();

        for (Doctor doc : doctors) {
            double duration = predictDuration(doc, patientAge, symptoms, appointmentType, isNewPatient);
            predictions.put(doc.getDoctorId(), duration);
        }
        return predictions;
    }

    /**
     * Get a summary report of consultation duration factors.
     */
    public DurationReport getDurationReport(Doctor doctor, int patientAge, String symptoms) {
        double predicted = predictDuration(doctor, patientAge, symptoms, "consultation",
                patientAge < 18);

        String speedCategory;
        long avg = doctor.getAverageConsultationMinutes();
        if (avg <= 10)
            speedCategory = "Fast";
        else if (avg <= 18)
            speedCategory = "Average";
        else
            speedCategory = "Thorough";

        String ageGroup;
        if (patientAge < 5)
            ageGroup = "Infant/Toddler";
        else if (patientAge < 18)
            ageGroup = "Pediatric";
        else if (patientAge < 65)
            ageGroup = "Adult";
        else
            ageGroup = "Elderly";

        return new DurationReport(predicted, speedCategory, ageGroup,
                doctor.getAverageConsultationMinutes(), doctor.getName());
    }

    private double getTimeOfDayFactor() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 9)
            return 1.15; // Early morning, slower start
        if (hour <= 11)
            return 1.0; // Morning peak, efficient
        if (hour == 12)
            return 1.1; // Lunch, slightly slower
        if (hour <= 14)
            return 1.05; // Post-lunch
        if (hour <= 16)
            return 0.95; // Afternoon, focused
        return 1.1; // Late afternoon, fatigue
    }

    private double getDayOfWeekFactor() {
        DayOfWeek day = LocalDateTime.now().getDayOfWeek();
        return switch (day) {
            case MONDAY -> 1.1; // Monday backlog
            case TUESDAY, WEDNESDAY -> 1.0;
            case THURSDAY -> 0.95; // Mid-week efficiency
            case FRIDAY -> 1.05; // Friday wrap-up rush
            case SATURDAY, SUNDAY -> 0.9; // Weekend = lighter load
        };
    }

    private double getAgeFactor(int age) {
        if (age < 2)
            return 1.4;
        if (age < 5)
            return 1.3;
        if (age < 12)
            return 1.15;
        if (age < 18)
            return 1.05;
        if (age < 40)
            return 1.0;
        if (age < 60)
            return 1.05;
        if (age < 75)
            return 1.15;
        return 1.3;
    }

    private double getSymptomComplexityFactor(String symptoms) {
        if (symptoms == null || symptoms.isBlank())
            return 1.0;

        String lower = symptoms.toLowerCase();
        int complexityScore = 0;

        // Simple symptoms
        if (lower.contains("cold") || lower.contains("cough") || lower.contains("headache"))
            complexityScore += 1;
        // Moderate symptoms
        if (lower.contains("fever") || lower.contains("pain") || lower.contains("rash"))
            complexityScore += 1;
        // Complex symptoms
        if (lower.contains("chest") || lower.contains("breathing") || lower.contains("abdomen"))
            complexityScore += 2;
        if (lower.contains("dizzy") || lower.contains("numbness") || lower.contains("vision"))
            complexityScore += 2;
        // Multiple symptoms indicator
        String[] commas = symptoms.split("[,;]");
        if (commas.length > 3)
            complexityScore += 1;

        if (complexityScore <= 1)
            return 0.85;
        if (complexityScore <= 3)
            return 1.0;
        return 1.3;
    }

    private double getTypeFactor(String appointmentType) {
        if (appointmentType == null)
            return 1.0;
        return switch (appointmentType.toLowerCase()) {
            case "checkup", "followup" -> 0.8;
            case "procedure" -> 2.0;
            case "consultation" -> 1.0;
            default -> 1.0;
        };
    }

    private int getDoctorWaitingCount(String doctorId) {
        try {
            return queueRepository.countWaitingByDoctor(doctorId);
        } catch (Exception e) {
            return 0;
        }
    }

    public record DurationReport(
            double predictedMinutes,
            String speedCategory,
            String ageGroup,
            long doctorAverageMinutes,
            String doctorName) {
    }
}
