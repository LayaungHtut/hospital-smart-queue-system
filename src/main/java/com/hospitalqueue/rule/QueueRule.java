package com.hospitalqueue.rule;

import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Queue;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Smart Queue Engine rules (Member 2).
 */
@Component
public class QueueRule {

    private static final long DEFAULT_MAX_QUEUE_SIZE = 20;

    /**
     * Rule 1: a patient may only have one active queue.
     */
    public boolean hasActiveQueue(Queue activeQueue) {
        return activeQueue != null;
    }

    /**
     * Rule 11: the queue has reached its configured capacity.
     */
    public boolean isQueueFull(int waitingCount, long maxQueueSize) {
        return waitingCount >= maxQueueSize;
    }

    public long getDefaultMaxQueueSize() {
        return DEFAULT_MAX_QUEUE_SIZE;
    }

    /**
     * Rule 8: is the current time inside the doctor's registration window.
     */
    public boolean isWithinRegistrationWindow(LocalTime open, LocalTime close, LocalTime now) {
        if (open == null || close == null) {
            return true;
        }
        return !now.isBefore(open) && !now.isAfter(close);
    }

    /**
     * Rule 9: does the patient have an appointment today.
     */
    public boolean hasTodaysAppointment(Appointment appointment) {
        return appointment != null;
    }

    /**
     * Rule 6: is the given priority emergency.
     */
    public boolean isEmergency(String priority) {
        return Queue.PRIORITY_EMERGENCY.equals(priority);
    }
}
