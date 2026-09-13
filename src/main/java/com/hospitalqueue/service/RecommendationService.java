package com.hospitalqueue.service;

import com.hospitalqueue.ai.AIRecommendationService;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.rule.DoctorAvailabilityRule;
import com.hospitalqueue.rule.DoctorRecommendationRule;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final DoctorRecommendationRule doctorRecommendationRule;
    private final DoctorAvailabilityRule doctorAvailabilityRule;
    private final DoctorRepository doctorRepository;
    private final QueueRepository queueRepository;
    private final WaitingTimeService waitingTimeService;
    private final AIRecommendationService aiRecommendationService;

    public RecommendationService(DoctorRecommendationRule doctorRecommendationRule,
                                 DoctorAvailabilityRule doctorAvailabilityRule,
                                 DoctorRepository doctorRepository,
                                 QueueRepository queueRepository,
                                 WaitingTimeService waitingTimeService,
                                 AIRecommendationService aiRecommendationService) {
        this.doctorRecommendationRule = doctorRecommendationRule;
        this.doctorAvailabilityRule = doctorAvailabilityRule;
        this.doctorRepository = doctorRepository;
        this.queueRepository = queueRepository;
        this.waitingTimeService = waitingTimeService;
        this.aiRecommendationService = aiRecommendationService;
    }

    /**
     * Uses AI (with local fallback) to recommend a department from symptoms.
     */
    public AIRecommendationService.Recommendation recommendDepartmentAI(String symptoms) {
        return aiRecommendationService.recommend(symptoms);
    }

    public Department recommendDepartment(String symptoms) {
        AIRecommendationService.Recommendation rec = aiRecommendationService.recommend(symptoms);
        return rec.getDepartment();
    }

    public boolean hasEmergencySymptoms(String symptoms) {
        AIRecommendationService.Recommendation rec = aiRecommendationService.recommend(symptoms);
        return rec != null && rec.isEmergency();
    }

    /**
     * Rule 7 + doctor availability: recommends the doctors of a department.
     * Available doctors come first, each group ordered by shortest waiting time.
     */
    public List<Doctor> recommendDoctors(int departmentId) {
        List<Doctor> doctors = doctorRepository.findByDepartment(departmentId);
        Map<String, Integer> waitingCounts = queueRepository.countWaitingByAllDoctors();
        // Fetched once and reused for both the wait-time map and the
        // availability check below, instead of querying per doctor twice over.
        Map<String, Double> historicalAvgByDoctorId = queueRepository.getPreviousDayAverageConsultationMinutesForAllDoctors();
        Map<String, Long> waitingTimeByDoctorId = buildWaitingTimeMapWithCounts(doctors, waitingCounts, historicalAvgByDoctorId);

        LocalTime now = LocalTime.now();
        for (Doctor doctor : doctors) {
            doctor.setComputedAvailable(
                    doctorAvailabilityRule.isAvailable(doctor, waitingCounts.getOrDefault(doctor.getDoctorId(), 0),
                            now, historicalAvgByDoctorId));
        }

        List<Doctor> available = doctors.stream().filter(d -> d.isComputedAvailable()).collect(Collectors.toList());
        List<Doctor> unavailable = doctors.stream().filter(d -> !d.isComputedAvailable()).collect(Collectors.toList());
        List<Doctor> result = new ArrayList<>();
        result.addAll(doctorRecommendationRule.sortByShortestWaitingTime(available, waitingTimeByDoctorId));
        result.addAll(doctorRecommendationRule.sortByShortestWaitingTime(unavailable, waitingTimeByDoctorId));
        return result;
    }

    public Map<String, Long> buildWaitingTimeMap(List<Doctor> doctors) {
        return buildWaitingTimeMapWithCounts(doctors, queueRepository.countWaitingByAllDoctors(),
                queueRepository.getPreviousDayAverageConsultationMinutesForAllDoctors());
    }

    private Map<String, Long> buildWaitingTimeMapWithCounts(List<Doctor> doctors, Map<String, Integer> waitingCounts,
            Map<String, Double> historicalAvgByDoctorId) {
        Map<String, Long> waitingTimeByDoctorId = new HashMap<>();
        for (Doctor doctor : doctors) {
            int count = waitingCounts.getOrDefault(doctor.getDoctorId(), 0);
            long avgConsult = waitingTimeService.getDoctorAverageConsultationMinutes(doctor, historicalAvgByDoctorId);
            long waitingTime = waitingTimeService.calculateWaitingTime(count, avgConsult);
            waitingTimeByDoctorId.put(doctor.getDoctorId(), waitingTime);
        }
        return waitingTimeByDoctorId;
    }
}
