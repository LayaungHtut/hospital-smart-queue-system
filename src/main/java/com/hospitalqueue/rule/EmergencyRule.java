package com.hospitalqueue.rule;

import com.hospitalqueue.model.Queue;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Emergency screening rules (Member 2 - Smart Queue Engine).
 */
@Component
public class EmergencyRule {

    /**
     * Flags a queue as an emergency (pending staff confirmation).
     */
    public boolean requiresStaffConfirmation(Queue queue) {
        return queue != null && queue.isEmergency() && !queue.isEmergencyConfirmed();
    }

    /**
     * An emergency is fully active once confirmed by staff.
     */
    public boolean isActiveEmergency(Queue queue) {
        return queue != null && queue.isEmergency() && queue.isEmergencyConfirmed();
    }

    /**
     * Reorders a waiting list so that confirmed emergencies are served first.
     * Order: confirmed emergency -> appointment -> normal, then by position.
     */
    public List<Queue> reorderByPriority(List<Queue> waitingQueues) {
        waitingQueues.sort((q1, q2) -> {
            int p1 = priorityRank(q1);
            int p2 = priorityRank(q2);
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
            return Integer.compare(q1.getPosition(), q2.getPosition());
        });
        return waitingQueues;
    }

    /**
     * Detects missed queues (expiry rule): a waiting queue older than the
     * configured expiry minutes is considered missed.
     */
    public boolean isMissed(LocalDateTime createdAt, long expiryMinutes) {
        if (createdAt == null) {
            return false;
        }
        return Duration.between(createdAt, LocalDateTime.now()).toMinutes() >= expiryMinutes;
    }

    private int priorityRank(Queue queue) {
        if (Queue.PRIORITY_EMERGENCY.equals(queue.getPriority())) {
            return 1;
        }
        if (Queue.PRIORITY_APPOINTMENT.equals(queue.getPriority())) {
            return 2;
        }
        return 3;
    }
}
