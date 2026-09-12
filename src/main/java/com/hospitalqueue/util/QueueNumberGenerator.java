package com.hospitalqueue.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates queue numbers like QCAR202608080001.
 */
public final class QueueNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String QUEUE_NUMBER_PREFIX = "Q";

    private QueueNumberGenerator() {
    }

    public static String generateQueueNumber(String doctorCode, int lastSequence) {
        int nextSequence = lastSequence + 1;
        return QUEUE_NUMBER_PREFIX + doctorCode + getDatePrefix() + String.format("%04d", nextSequence);
    }

    public static int extractLastSequence(String queueNumber) {
        if (queueNumber == null || queueNumber.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(queueNumber.substring(queueNumber.length() - 4));
    }

    public static String buildPrefix(String doctorCode) {
        return QUEUE_NUMBER_PREFIX + doctorCode + getDatePrefix();
    }

    public static String getDatePrefix() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
}
