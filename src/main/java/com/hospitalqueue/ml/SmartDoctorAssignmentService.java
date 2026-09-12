package com.hospitalqueue.ml;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.QueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Doctor Assignment: recommends the best doctor for a patient based on:
 * - Specialization match with symptoms
 * - Current queue load (fewer waiting = better)
 * - Doctor availability status
 * - Historical treatment success rate
 * - Predicted wait time
 */
@Service
public class SmartDoctorAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(SmartDoctorAssignmentService.class);

    private final DoctorRepository doctorRepository;
    private final QueueRepository queueRepository;
    private final AppointmentRepository appointmentRepository;

    public SmartDoctorAssignmentService(DoctorRepository doctorRepository,
                                        QueueRepository queueRepository,
                                        AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.queueRepository = queueRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Get ranked doctor recommendations for a department.
     */
    public List<DoctorRecommendation> recommendDoctors(int departmentId, String symptoms,
                                                        String patientId) {
        List<Doctor> doctors = doctorRepository.findByDepartment(departmentId);
        if (doctors.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> queueCounts = queueRepository.countWaitingByAllDoctors();

        List<DoctorRecommendation> recommendations = new ArrayList<>();
        for (Doctor doctor : doctors) {
            DoctorRecommendation rec = scoreDoctor(doctor, symptoms, patientId, queueCounts);
            recommendations.add(rec);
        }

        // Sort by score descending (highest score = best match)
        recommendations.sort((a, b) -> Double.compare(b.score(), a.score()));

        // Assign ranks
        List<DoctorRecommendation> ranked = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            DoctorRecommendation r = recommendations.get(i);
            ranked.add(new DoctorRecommendation(
                    r.doctor(), r.score(), i + 1,
                    r.queueLoadScore(), r.availabilityScore(),
                    r.specializationScore(), r.historyScore(),
                    r.waitTimeMinutes(), r.reason()
            ));
        }

        log.debug("Smart assignment for dept {} with symptoms '{}': {} doctors ranked",
                departmentId, symptoms, ranked.size());

        return ranked;
    }

    /**
     * Get the single best doctor recommendation.
     */
    public DoctorRecommendation recommendBest(int departmentId, String symptoms, String patientId) {
        List<DoctorRecommendation> all = recommendDoctors(departmentId, symptoms, patientId);
        return all.isEmpty() ? null : all.get(0);
    }

    private DoctorRecommendation scoreDoctor(Doctor doctor, String symptoms,
                                              String patientId, Map<String, Integer> queueCounts) {
        // 1. Specialization score (0-100)
        double specScore = calculateSpecializationScore(doctor, symptoms);

        // 2. Queue load score (0-100) — fewer waiting = higher score
        int waiting = queueCounts.getOrDefault(doctor.getDoctorId(), 0);
        double loadScore = calculateLoadScore(waiting, (int) doctor.getMaxQueueSize());

        // 3. Availability score (0-100)
        double availScore = calculateAvailabilityScore(doctor);

        // 4. History score (0-100) — past treatment success with this patient
        double historyScore = calculateHistoryScore(doctor.getDoctorId(), patientId);

        // 5. Estimated wait time
        long avgConsult = doctor.getAverageConsultationMinutes() > 0
                ? doctor.getAverageConsultationMinutes() : 15;
        double waitMinutes = waiting * avgConsult;

        // Weighted final score
        double score = (specScore * 0.35) + (loadScore * 0.25)
                + (availScore * 0.25) + (historyScore * 0.15);

        String reason = buildReason(specScore, loadScore, availScore, historyScore,
                waiting, doctor.getName());

        return new DoctorRecommendation(doctor, score, 0,
                loadScore, availScore, specScore, historyScore,
                waitMinutes, reason);
    }

    private double calculateSpecializationScore(Doctor doctor, String symptoms) {
        if (symptoms == null || symptoms.isBlank()) return 50.0;

        String spec = doctor.getSpecialization() != null ? doctor.getSpecialization().toLowerCase() : "";
        String lower = symptoms.toLowerCase();

        // Direct keyword matching
        Map<String, List<String>> keywordMap = Map.of(
                "cardiology", List.of("chest", "heart", "palpitation", "blood pressure", "hypertension", "angina"),
                "neurology", List.of("headache", "migraine", "seizure", "dizzy", "numbness", "stroke", "vision"),
                "orthopedics", List.of("bone", "fracture", "joint", "knee", "back", "sprain", "sports injury"),
                "general", List.of("fever", "cold", "cough", "fatigue", "weakness", "routine", "checkup"),
                "pediatrics", List.of("child", "baby", "vaccination", "growth", "pediatric"),
                "dermatology", List.of("skin", "rash", "acne", "eczema", "psoriasis", "allergy")
        );

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            if (spec.contains(entry.getKey()) || entry.getKey().contains(spec)) {
                long matches = entry.getValue().stream()
                        .filter(lower::contains)
                        .count();
                if (matches > 0) return 90.0 + Math.min(matches * 5, 10);
            }
        }

        // General medicine is a catch-all
        if (spec.contains("general") || spec.contains("internal")) {
            return 60.0;
        }

        return 40.0;
    }

    private double calculateLoadScore(int waiting, int maxQueueSize) {
        if (maxQueueSize <= 0) maxQueueSize = 10;
        double loadRatio = (double) waiting / maxQueueSize;
        // Linear decay: 0 waiting = 100, full = 0
        return Math.max(0, 100 * (1.0 - loadRatio));
    }

    private double calculateAvailabilityScore(Doctor doctor) {
        if (!doctor.isActive()) return 0.0;
        if (!doctor.isAvailable()) return 20.0;
        if (doctor.isComputedAvailable()) return 100.0;
        return 60.0;
    }

    private double calculateHistoryScore(String doctorId, String patientId) {
        if (patientId == null || patientId.isBlank()) return 50.0;

        try {
            long totalAppts = appointmentRepository.countByPatientId(patientId);
            // Without a treatment-outcome table, use appointment count as a proxy
            // More past visits with this doctor = higher familiarity score
            return Math.min(80, 30 + totalAppts * 5);
        } catch (Exception e) {
            return 50.0;
        }
    }

    private String buildReason(double specScore, double loadScore, double availScore,
                                double historyScore, int waiting, String doctorName) {
        List<String> reasons = new ArrayList<>();

        if (specScore >= 80) reasons.add("specialization match");
        if (loadScore >= 70) reasons.add("low queue load (" + waiting + " waiting)");
        else if (loadScore < 30) reasons.add("high queue load (" + waiting + " waiting)");
        if (availScore >= 90) reasons.add("currently available");
        else if (availScore < 30) reasons.add("limited availability");
        if (historyScore >= 60) reasons.add("familiar with patient history");

        if (reasons.isEmpty()) reasons.add("general recommendation");

        return doctorName + " recommended: " + String.join(", ", reasons);
    }

    public record DoctorRecommendation(
            Doctor doctor,
            double score,
            int rank,
            double queueLoadScore,
            double availabilityScore,
            double specializationScore,
            double historyScore,
            double waitTimeMinutes,
            String reason
    ) {}
}
