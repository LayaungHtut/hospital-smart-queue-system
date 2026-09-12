package com.hospitalqueue.rule;

import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.service.WaitingTimeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Doctor availability detection (Member 2 - Smart Queue Engine).
 *
 * A doctor is available when:
 *  - the account is active and not manually marked unavailable,
 *  - the current time is inside the working / registration window,
 *  - the queue has not reached its calculated session capacity
 *    (based on session duration and average consultation time).
 */
@Component
public class DoctorAvailabilityRule {

    private final WaitingTimeService waitingTimeService;

    public DoctorAvailabilityRule(@Lazy WaitingTimeService waitingTimeService) {
        this.waitingTimeService = waitingTimeService;
    }

    public boolean isWithinWorkingHours(Doctor doctor, LocalTime now) {
        if (doctor.getQueueOpenTime() == null || doctor.getQueueCloseTime() == null) {
            return true;
        }
        return !now.isBefore(doctor.getQueueOpenTime()) && !now.isAfter(doctor.getQueueCloseTime());
    }

    public boolean isAvailable(Doctor doctor, int waitingCount, LocalTime now) {
        if (doctor == null || !doctor.isActive() || !doctor.isAvailable()) {
            return false;
        }
        if (!isWithinWorkingHours(doctor, now)) {
            return false;
        }
        long maxCapacity = calculateSessionCapacity(doctor);
        return waitingCount < maxCapacity;
    }

    /**
     * Calculates the doctor's effective session capacity based on
     * session duration and average consultation time.
     * Subtracts 60 minutes for the lunch break.
     */
    public long calculateSessionCapacity(Doctor doctor) {
        if (doctor.getQueueOpenTime() == null || doctor.getQueueCloseTime() == null) {
            return doctor.getMaxQueueSize() > 0 ? doctor.getMaxQueueSize() : 30;
        }
        long sessionMinutes = java.time.Duration.between(doctor.getQueueOpenTime(), doctor.getQueueCloseTime()).toMinutes();
        if (sessionMinutes <= 0) sessionMinutes = 450;
        long effectiveSession = Math.max(60, sessionMinutes - 60); // minus lunch break
        long avgConsult = waitingTimeService.getDoctorAverageConsultationMinutes(doctor);
        long calculated = Math.max(5, effectiveSession / Math.max(5, avgConsult));
        return doctor.getMaxQueueSize() > 0 ? Math.min(doctor.getMaxQueueSize(), calculated) : calculated;
    }

    /**
     * Marks each doctor with a computed availability flag and sorts available doctors first.
     */
    public List<Doctor> markAndSortAvailable(List<Doctor> doctors, Map<String, Integer> waitingCounts, LocalTime now) {
        doctors.sort((d1, d2) -> {
            boolean a1 = isAvailable(d1, waitingCounts.getOrDefault(d1.getDoctorId(), 0), now);
            boolean a2 = isAvailable(d2, waitingCounts.getOrDefault(d2.getDoctorId(), 0), now);
            if (a1 != a2) {
                return a1 ? -1 : 1;
            }
            return d1.getName().compareToIgnoreCase(d2.getName());
        });
        for (Doctor doctor : doctors) {
            doctor.setComputedAvailable(isAvailable(doctor, waitingCounts.getOrDefault(doctor.getDoctorId(), 0), now));
        }
        return doctors;
    }
}
