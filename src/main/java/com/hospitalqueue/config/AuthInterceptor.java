package com.hospitalqueue.config;

import com.hospitalqueue.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protects the patient / doctor / staff / admin areas.
 * Requires the matching session attribute for each area.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String route = path.substring(contextPath.length());
        HttpSession session = request.getSession(false);

        // The auth, lookup and ai endpoints are intentionally left open here:
        // /api/auth hosts the login/registration calls themselves, and
        // /api/lookup only exposes public reference data (departments, doctors, etc).
        if (route.startsWith("/api/auth") || route.startsWith("/api/lookup")) {
            return true;
        }

        if (route.startsWith("/api/patient/")) {
            if (session == null || !SessionUtil.isPatientLoggedIn(session)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            return true;
        }

        if (route.startsWith("/api/doctor/")) {
            if (session == null || !SessionUtil.isDoctorLoggedIn(session)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            return true;
        }

        if (route.startsWith("/api/staff/")) {
            if (session == null || !SessionUtil.isStaffLoggedIn(session)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            return true;
        }

        if (route.startsWith("/api/admin/")) {
            if (session == null || !SessionUtil.isAdminLoggedIn(session)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            return true;
        }

        if (route.startsWith("/api/")) {
            return true;
        }

        if (route.startsWith("/patient/") || route.startsWith("/patient")) {
            if (route.endsWith("/login") || route.endsWith("/register")) {
                return true;
            }
            if (session == null || !SessionUtil.isPatientLoggedIn(session)) {
                response.sendRedirect(contextPath + "/patient/login");
                return false;
            }
            return true;
        }

        if (route.startsWith("/doctor/") || route.startsWith("/doctor")) {
            if (route.endsWith("/login")) {
                return true;
            }
            if (session == null || !SessionUtil.isDoctorLoggedIn(session)) {
                response.sendRedirect(contextPath + "/doctor/login");
                return false;
            }
            return true;
        }

        if (route.startsWith("/staff/") || route.startsWith("/staff")) {
            if (route.endsWith("/login")) {
                return true;
            }
            if (session == null || !SessionUtil.isStaffLoggedIn(session)) {
                response.sendRedirect(contextPath + "/staff/login");
                return false;
            }
            return true;
        }

        if (route.startsWith("/admin/") || route.startsWith("/admin")) {
            if (route.endsWith("/login")) {
                return true;
            }
            if (session == null || !SessionUtil.isAdminLoggedIn(session)) {
                response.sendRedirect(contextPath + "/admin/login");
                return false;
            }
            return true;
        }

        return true;
    }
}
