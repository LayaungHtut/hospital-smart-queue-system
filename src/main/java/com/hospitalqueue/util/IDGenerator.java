package com.hospitalqueue.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates prefixed IDs (P patient, D doctor, S staff, A admin).
 */
public final class IDGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PATIENT_ID_PREFIX = "P";
    private static final String DOCTOR_ID_PREFIX = "D";
    private static final String STAFF_ID_PREFIX = "S";
    private static final String ADMIN_ID_PREFIX = "A";

    private IDGenerator() {
    }

    public static String generatePatientId(int lastSequence) {
        return PATIENT_ID_PREFIX + LocalDate.now().format(DATE_FORMATTER) + String.format("%04d", lastSequence + 1);
    }

    public static String generateDoctorId(int lastSequence) {
        return DOCTOR_ID_PREFIX + LocalDate.now().format(DATE_FORMATTER) + String.format("%04d", lastSequence + 1);
    }

    public static String generateStaffId(int lastSequence) {
        return STAFF_ID_PREFIX + LocalDate.now().format(DATE_FORMATTER) + String.format("%04d", lastSequence + 1);
    }

    public static String generateAdminId(int lastSequence) {
        return ADMIN_ID_PREFIX + LocalDate.now().format(DATE_FORMATTER) + String.format("%04d", lastSequence + 1);
    }
}
