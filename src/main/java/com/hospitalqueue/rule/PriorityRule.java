package com.hospitalqueue.rule;

import com.hospitalqueue.model.Queue;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Priority queue ordering for the Smart Queue Engine.
 * EMERGENCY (confirmed) > APPOINTMENT > NORMAL, then FIFO position.
 */
@Component
public class PriorityRule {

    private static final Comparator<String> PRIORITY_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String p1, String p2) {
            return Integer.compare(rank(p1), rank(p2));
        }
    };

    private static int rank(String priority) {
        if (Queue.PRIORITY_EMERGENCY.equals(priority)) {
            return 1;
        }
        if (Queue.PRIORITY_APPOINTMENT.equals(priority)) {
            return 2;
        }
        return 3;
    }

    public Comparator<String> priorityComparator() {
        return PRIORITY_COMPARATOR;
    }

    public int rankOf(String priority) {
        return rank(priority);
    }
}
