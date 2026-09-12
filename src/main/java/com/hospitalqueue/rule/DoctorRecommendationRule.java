package com.hospitalqueue.rule;

import com.hospitalqueue.model.Doctor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rule 7: recommends doctors ordered by shortest waiting time.
 */
@Component
public class DoctorRecommendationRule {

    public List<Doctor> sortByShortestWaitingTime(List<Doctor> doctors, Map<String, Long> waitingTimeByDoctorId) {
        doctors.sort(new Comparator<Doctor>() {
            @Override
            public int compare(Doctor d1, Doctor d2) {
                long w1 = waitingTime(d1, waitingTimeByDoctorId);
                long w2 = waitingTime(d2, waitingTimeByDoctorId);
                return Long.compare(w1, w2);
            }
        });
        return doctors;
    }

    private long waitingTime(Doctor doctor, Map<String, Long> waitingTimeByDoctorId) {
        Long waitingTime = waitingTimeByDoctorId.get(doctor.getDoctorId());
        return waitingTime != null ? waitingTime : Long.MAX_VALUE;
    }
}
