package com.hospitalqueue.service;

import com.hospitalqueue.ml.WaitTimePredictionService;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.QueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Calculates dynamic waiting times for the smart queue engine.
 * Uses historical average consultation duration when available, falls back
 * to the doctor's configured average or 20 minutes as default.
 * Accounts for doctor break time (lunch hour 12:00-13:00) in wait estimation.
 */
@Service
public class WaitingTimeService {

    private static final Logger log = LoggerFactory.getLogger(WaitingTimeService.class);
    private static final long DEFAULT_CONSULTATION_MINUTES = 20L;
    private static final long MIN_CONSULTATION_MINUTES = 5L;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final long LUNCH_MINUTES = 60;

    private final QueueRepository queueRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final WaitTimePredictionService waitTimePredictionService;

    public WaitingTimeService(QueueRepository queueRepository,
                              DoctorRepository doctorRepository,
                              DepartmentRepository departmentRepository,
                              @Lazy WaitTimePredictionService waitTimePredictionService) {
        this.queueRepository = queueRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.waitTimePredictionService = waitTimePredictionService;
    }

    /**
     * Simple wait time: number of patients ahead * average consultation time.
     */
    public long calculateWaitingTime(int waitingCount, long averageConsultationMinutes) {
        if (waitingCount <= 0) {
            return 0;
        }
        return waitingCount * averageConsultationMinutes;
    }

    /**
     * Gets the doctor's average consultation minutes using the best available data:
     * 1. Recent historical average from completed consultations (previous day, then all-time).
     * 2. The doctor's configured averageConsultationMinutes field.
     * 3. Default of 20 minutes.
     */
    public long getDoctorAverageConsultationMinutes(Doctor doctor) {
        if (doctor == null) return DEFAULT_CONSULTATION_MINUTES;

        // Try historical data: previous day average first, then all-time average
        Double historicalAvg = queueRepository.getPreviousDayAverageConsultationMinutes(doctor.getDoctorId());
        if (historicalAvg != null && historicalAvg > 0) {
            long result = Math.max(MIN_CONSULTATION_MINUTES, Math.round(historicalAvg));
            log.debug("Using historical avg consultation for Dr. {}: {} min", doctor.getDoctorId(), result);
            return result;
        }

        // Fall back to doctor's configured average
        if (doctor.getAverageConsultationMinutes() > 0) {
            return doctor.getAverageConsultationMinutes();
        }

        // Final fallback: default 20 minutes
        log.debug("No historical data for Dr. {}, using default {} min", doctor.getDoctorId(), DEFAULT_CONSULTATION_MINUTES);
        return DEFAULT_CONSULTATION_MINUTES;
    }

    /**
     * Calculate waiting time for a specific doctor including break offset.
     */
    public long calculateWaitingTimeForDoctor(String doctorId, long averageConsultationMinutes) {
        Doctor doctor = doctorRepository.findById(doctorId);
        int waitingCount = queueRepository.countWaitingByDoctor(doctorId);
        long avgMinutes = doctor != null
                ? getDoctorAverageConsultationMinutes(doctor)
                : (averageConsultationMinutes > 0 ? averageConsultationMinutes : DEFAULT_CONSULTATION_MINUTES);
        long wait = waitingCount * avgMinutes;
        wait += calculateBreakOffset(doctor, wait);
        return wait;
    }

    /**
     * Get predicted wait time for a specific doctor using the best available method:
     * 1. ML model prediction (if loaded).
     * 2. Historical average * waiting count + break offset.
     */
    public long getPredictedWaitTime(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId);
        if (doctor == null) {
            return 0;
        }

        Department department = departmentRepository.findById(doctor.getDepartmentId());
        int waitingCount = queueRepository.countWaitingByDoctor(doctorId);
        long avgConsult = getDoctorAverageConsultationMinutes(doctor);

        // Try ML prediction first
        if (waitTimePredictionService.isModelLoaded()) {
            try {
                Double predicted = waitTimePredictionService.predictWaitTime(
                        doctor, department, waitingCount, 30, 3, true);
                if (predicted != null && predicted > 0) {
                    log.debug("ML wait prediction for Dr. {}: {} min", doctorId, predicted);
                    long baseWait = Math.max(0, predicted.longValue());
                    return baseWait + calculateBreakOffset(doctor, baseWait);
                }
            } catch (Exception e) {
                log.warn("ML prediction failed for Dr. {}, falling back to calculation", doctorId, e);
            }
        }

        // Fallback: historical average * waiting count + break offset
        long calculated = calculateWaitingTime(waitingCount, avgConsult);
        return Math.max(0, calculated + calculateBreakOffset(doctor, calculated));
    }

    /**
     * Calculates additional wait time if the estimated wait window overlaps
     * with the doctor's lunch break (12:00 PM - 1:00 PM).
     *
     * Logic:
     * - If the doctor is currently on break, add the remaining break duration.
     * - If the estimated completion of this patient's wait would cross into
     *   the break window (i.e., current time + wait extends past 12:00 and
     *   current time is before 13:00), add the full 60-minute lunch break.
     */
    private long calculateBreakOffset(Doctor doctor, long baseWaitMinutes) {
        if (doctor == null || baseWaitMinutes <= 0) return 0;

        LocalTime now = LocalTime.now();

        // Case 1: Doctor is currently on break — add remaining break time
        if (now.isAfter(LUNCH_START) && now.isBefore(LUNCH_END)) {
            long remainingBreak = Duration.between(now, LUNCH_END).toMinutes();
            if (remainingBreak > 0) {
                log.debug("Doctor {} is on break, adding {} min remaining", doctor.getDoctorId(), remainingBreak);
                return remainingBreak;
            }
        }

        // Case 2: The estimated wait would cross into the break window
        // If now is before break end AND (now + baseWait) crosses into or through lunch
        if (now.isBefore(LUNCH_END)) {
            LocalTime estimatedCompletion = now.plusMinutes(baseWaitMinutes);
            // If the wait would extend past the lunch start, the patient hits the break
            if (estimatedCompletion.isAfter(LUNCH_START) && now.isBefore(LUNCH_END)) {
                log.debug("Wait crosses lunch break for Dr. {}, adding {} min break",
                        doctor.getDoctorId(), LUNCH_MINUTES);
                return LUNCH_MINUTES;
            }
        }

        return 0;
    }

    /**
     * Get estimated wait for a department (fallback for department-level display).
     */
    public long getDepartmentAverageWait(int departmentId) {
        int waitingCount = queueRepository.countWaitingByDepartment(departmentId);
        return waitingCount * DEFAULT_CONSULTATION_MINUTES;
    }

    /**
     * Public getter for the default consultation minutes (used by other services).
     */
    public long getDefaultConsultationMinutes() {
        return DEFAULT_CONSULTATION_MINUTES;
    }

    public long getDoctorAverageWait(String doctorId) {
        return getPredictedWaitTime(doctorId);
    }
}
