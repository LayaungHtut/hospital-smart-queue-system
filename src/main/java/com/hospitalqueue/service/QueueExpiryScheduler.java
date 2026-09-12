package com.hospitalqueue.service;

import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.rule.EmergencyRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Missed-queue detection & queue expiration (Member 2 - Smart Queue Engine).
 *
 * Runs automatically every minute and expires:
 *  - WAITING queues that were registered but never called (abandoned) after {@code waiting_expiry_minutes}
 *  - CALLED queues where the patient did not show up (no-show) after {@code queue_expiry_minutes}
 */
@Component
public class QueueExpiryScheduler {

    private static final long DEFAULT_QUEUE_EXPIRY_MINUTES = 15;
    private static final long DEFAULT_WAITING_EXPIRY_MINUTES = 120;

    private final QueueRepository queueRepository;
    private final QueueService queueService;
    private final EmergencyRule emergencyRule;
    private final JdbcTemplate jdbcTemplate;

    public QueueExpiryScheduler(QueueRepository queueRepository, QueueService queueService,
                                EmergencyRule emergencyRule, JdbcTemplate jdbcTemplate) {
        this.queueRepository = queueRepository;
        this.queueService = queueService;
        this.emergencyRule = emergencyRule;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireMissedQueues() {
        LocalDateTime now = LocalDateTime.now();

        long calledExpiry = readSettingMinutes("queue_expiry_minutes", DEFAULT_QUEUE_EXPIRY_MINUTES);
        long waitingExpiry = readSettingMinutes("waiting_expiry_minutes", DEFAULT_WAITING_EXPIRY_MINUTES);

        for (Queue queue : queueRepository.findWaitingQueuesExpiredBefore(now.minusMinutes(waitingExpiry))) {
            if (emergencyRule.isMissed(queue.getCreatedAt(), waitingExpiry)) {
                queueService.expireQueue(queue.getQueueId());
            }
        }

        for (Queue queue : queueRepository.findCalledQueuesExpiredBefore(now.minusMinutes(calledExpiry))) {
            if (emergencyRule.isMissed(queue.getCalledAt(), calledExpiry)) {
                queueService.expireQueue(queue.getQueueId());
            }
        }
    }

    private long readSettingMinutes(String key, long fallback) {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM system_setting WHERE setting_key = ?", String.class, key);
            return value == null || value.isBlank() ? fallback : Long.parseLong(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
