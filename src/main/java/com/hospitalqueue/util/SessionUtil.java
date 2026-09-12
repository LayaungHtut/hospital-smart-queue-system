package com.hospitalqueue.util;

import com.hospitalqueue.model.AdminUser;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Staff;
import jakarta.servlet.http.HttpSession;

/**
 * Session helpers for the different actor types (patient, doctor, staff, admin).
 */
public final class SessionUtil {

    public static final String ATTR_PATIENT = "currentPatient";
    public static final String ATTR_DOCTOR = "currentDoctor";
    public static final String ATTR_STAFF = "currentStaff";
    public static final String ATTR_ADMIN = "currentAdmin";
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_INFO = "info";

    private SessionUtil() {
    }

    public static void setPatient(HttpSession session, Patient patient) {
        session.setAttribute(ATTR_PATIENT, patient);
    }

    public static Patient getPatient(HttpSession session) {
        return session == null ? null : (Patient) session.getAttribute(ATTR_PATIENT);
    }

    public static void setDoctor(HttpSession session, Doctor doctor) {
        session.setAttribute(ATTR_DOCTOR, doctor);
    }

    public static Doctor getDoctor(HttpSession session) {
        return session == null ? null : (Doctor) session.getAttribute(ATTR_DOCTOR);
    }

    public static void setStaff(HttpSession session, Staff staff) {
        session.setAttribute(ATTR_STAFF, staff);
    }

    public static Staff getStaff(HttpSession session) {
        return session == null ? null : (Staff) session.getAttribute(ATTR_STAFF);
    }

    public static void setAdmin(HttpSession session, AdminUser admin) {
        session.setAttribute(ATTR_ADMIN, admin);
    }

    public static AdminUser getAdmin(HttpSession session) {
        return session == null ? null : (AdminUser) session.getAttribute(ATTR_ADMIN);
    }

    public static boolean isPatientLoggedIn(HttpSession session) {
        return getPatient(session) != null;
    }

    public static boolean isDoctorLoggedIn(HttpSession session) {
        return getDoctor(session) != null;
    }

    public static boolean isStaffLoggedIn(HttpSession session) {
        return getStaff(session) != null;
    }

    public static boolean isAdminLoggedIn(HttpSession session) {
        return getAdmin(session) != null;
    }

    public static void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}
