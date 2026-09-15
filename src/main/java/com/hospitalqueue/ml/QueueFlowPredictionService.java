package com.hospitalqueue.ml;

import com.hospitalqueue.model.Department;
import com.hospitalqueue.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Queue Flow Prediction: predicts patient arrival volume for the next 1-2 hours
 * based on historical time-series patterns (hour-of-day, day-of-week).
 *
 * Uses a lightweight statistical approach:
 * - Queries historical queue arrivals grouped by hour and day-of-week
 * - Applies time-of-day seasonality factors
 * - Provides per-department and overall predictions
 */
@Service
public class QueueFlowPredictionService {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(QueueFlowPredictionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DepartmentRepository departmentRepository;

    private static class CacheEntry<T> {
        final T data;
        final long expiresAt;

        CacheEntry(T data, long ttlMs) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isValid() {
            return System.currentTimeMillis() < expiresAt;
        }
    }

    private final Map<Integer, CacheEntry<FlowPrediction>> flowCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<DepartmentFlowPrediction>> deptFlowCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<DayOfWeek, CacheEntry<PeakHoursReport>> peakHoursCache = new java.util.concurrent.ConcurrentHashMap<>();

    public QueueFlowPredictionService(JdbcTemplate jdbcTemplate,
            DepartmentRepository departmentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Predict arrival count for the next N hours, overall.
     */
    public FlowPrediction predictNextHours(int hoursAhead) {
        CacheEntry<FlowPrediction> cached = flowCache.get(hoursAhead);
        if (cached != null && cached.isValid()) {
            return cached.data;
        }
        LocalDateTime now = LocalDateTime.now();
        List<HourlyPrediction> predictions = new ArrayList<>();

        Map<String, Double> historicalMap = new HashMap<>();
        try {
            String sql = """
                    SELECT CAST(EXTRACT(DOW FROM created_at) AS INTEGER) AS dow,
                           CAST(EXTRACT(HOUR FROM created_at) AS INTEGER) AS hr,
                           COALESCE(AVG(cnt), 0) AS avg_arrivals
                    FROM (
                        SELECT DATE(created_at) AS d,
                               EXTRACT(DOW FROM created_at) AS dow,
                               EXTRACT(HOUR FROM created_at) AS hr,
                               COUNT(*) AS cnt
                        FROM queue
                        WHERE created_at > NOW() - INTERVAL '90 days'
                        GROUP BY DATE(created_at), EXTRACT(DOW FROM created_at), EXTRACT(HOUR FROM created_at)
                    ) sub
                    GROUP BY dow, hr
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                int dow = ((Number) row.get("dow")).intValue();
                int hr = ((Number) row.get("hr")).intValue();
                double avg = ((Number) row.get("avg_arrivals")).doubleValue();
                historicalMap.put(dow + ":" + hr, avg);
            }
        } catch (Exception e) {
            // Fall back to heuristics if query fails
        }

        for (int h = 0; h < hoursAhead; h++) {
            LocalDateTime target = now.plusHours(h);
            int dow = toPostgresDow(target.getDayOfWeek());
            int hr = target.getHour();
            Double hist = historicalMap.get(dow + ":" + hr);
            double predicted = (hist != null && hist > 0) ? hist : getDefaultEstimate(hr);
            predictions.add(new HourlyPrediction(
                    target.getHour(),
                    target.getDayOfWeek().toString(),
                    Math.round(predicted * 10.0) / 10.0,
                    getConfidenceLevel(predicted)));
        }

        double total = predictions.stream().mapToDouble(HourlyPrediction::predictedArrivals).sum();
        String trend = analyzeTrend(predictions);

        FlowPrediction result = new FlowPrediction(predictions, Math.round(total * 10.0) / 10.0, trend, now);
        flowCache.put(hoursAhead, new CacheEntry<>(result, 60000));
        return result;
    }

    /**
     * Predict arrivals per department for the next N hours.
     * Uses a single aggregated batch query instead of N+1 sequential DB roundtrips.
     */
    public DepartmentFlowPrediction predictByDepartment(int hoursAhead) {
        CacheEntry<DepartmentFlowPrediction> cached = deptFlowCache.get(hoursAhead);
        if (cached != null && cached.isValid()) {
            return cached.data;
        }

        List<Department> departments = departmentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<HourlyPrediction>> deptPredictions = new LinkedHashMap<>();

        Map<String, Double> historicalMap = new HashMap<>();
        try {
            String sql = """
                    SELECT department_id,
                           CAST(EXTRACT(DOW FROM created_at) AS INTEGER) AS dow,
                           CAST(EXTRACT(HOUR FROM created_at) AS INTEGER) AS hr,
                           COALESCE(AVG(cnt), 0) AS avg_arrivals
                    FROM (
                        SELECT department_id,
                               DATE(created_at) AS d,
                               EXTRACT(DOW FROM created_at) AS dow,
                               EXTRACT(HOUR FROM created_at) AS hr,
                               COUNT(*) AS cnt
                        FROM queue
                        WHERE created_at > NOW() - INTERVAL '90 days'
                        GROUP BY department_id, DATE(created_at), EXTRACT(DOW FROM created_at), EXTRACT(HOUR FROM created_at)
                    ) sub
                    GROUP BY department_id, dow, hr
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                int deptId = ((Number) row.get("department_id")).intValue();
                int dow = ((Number) row.get("dow")).intValue();
                int hr = ((Number) row.get("hr")).intValue();
                double avg = ((Number) row.get("avg_arrivals")).doubleValue();
                historicalMap.put(deptId + ":" + dow + ":" + hr, avg);
            }
        } catch (Exception e) {
            // Fall back to heuristics if query fails
        }

        for (Department dept : departments) {
            List<HourlyPrediction> hourly = new ArrayList<>();
            for (int h = 0; h < hoursAhead; h++) {
                LocalDateTime target = now.plusHours(h);
                int dow = toPostgresDow(target.getDayOfWeek());
                int hr = target.getHour();
                Double hist = historicalMap.get(dept.getDepartmentId() + ":" + dow + ":" + hr);
                double predicted = (hist != null && hist > 0) ? hist : getDefaultDeptEstimate(hr);
                hourly.add(new HourlyPrediction(
                        target.getHour(),
                        target.getDayOfWeek().toString(),
                        Math.round(predicted * 10.0) / 10.0,
                        getConfidenceLevel(predicted)));
            }
            deptPredictions.put(dept.getDepartmentCode(), hourly);
        }

        DepartmentFlowPrediction result = new DepartmentFlowPrediction(deptPredictions, hoursAhead, now);
        deptFlowCache.put(hoursAhead, new CacheEntry<>(result, 60000));
        return result;
    }

    /**
     * Get peak hours analysis for a given day.
     */
    public PeakHoursReport getPeakHours(DayOfWeek dayOfWeek) {
        CacheEntry<PeakHoursReport> cached = peakHoursCache.get(dayOfWeek);
        if (cached != null && cached.isValid()) {
            return cached.data;
        }

        String sql = """
                SELECT EXTRACT(HOUR FROM created_at) AS hour_of_day,
                       COUNT(*) AS total_arrivals
                FROM queue
                WHERE EXTRACT(DOW FROM created_at) = ?
                GROUP BY hour_of_day
                ORDER BY hour_of_day
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, toPostgresDow(dayOfWeek));

        Map<Integer, Double> hourlyAverages = new LinkedHashMap<>();
        for (int h = 8; h <= 20; h++)
            hourlyAverages.put(h, 0.0);

        long totalWeeks = Math.max(1, getTotalWeeksOfData());

        for (Map<String, Object> row : rows) {
            int hour = ((Number) row.get("hour_of_day")).intValue();
            long count = ((Number) row.get("total_arrivals")).longValue();
            if (hour >= 8 && hour <= 20) {
                hourlyAverages.put(hour, (double) count / totalWeeks);
            }
        }

        // Find peak and off-peak
        Optional<Map.Entry<Integer, Double>> peak = hourlyAverages.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        Optional<Map.Entry<Integer, Double>> offPeak = hourlyAverages.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min(Map.Entry.comparingByValue());

        String peakHour = peak.map(e -> String.format("%02d:00", e.getKey())).orElse("N/A");
        String offPeakHour = offPeak.map(e -> String.format("%02d:00", e.getKey())).orElse("N/A");

        List<Integer> rushHours = hourlyAverages.entrySet().stream()
                .filter(e -> e.getValue() > 3.0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        PeakHoursReport result = new PeakHoursReport(dayOfWeek.toString(), hourlyAverages, peakHour, offPeakHour,
                rushHours);
        peakHoursCache.put(dayOfWeek, new CacheEntry<>(result, 120000));
        return result;
    }

    /**
     * Get staffing recommendation based on predicted flow.
     */
    public StaffingRecommendation getStaffingRecommendation(int hoursAhead) {
        FlowPrediction prediction = predictNextHours(hoursAhead);
        double avgPerHour = prediction.totalPredictedArrivals() / Math.max(1, hoursAhead);

        // Rough rule: 1 doctor per 4 patients/hour
        int recommendedDoctors = (int) Math.ceil(avgPerHour / 4.0);
        recommendedDoctors = Math.max(1, Math.min(recommendedDoctors, 12));

        // Time period classification
        LocalTime now = LocalTime.now();
        String period;
        if (now.isBefore(LocalTime.of(10, 0)))
            period = "Early Morning";
        else if (now.isBefore(LocalTime.of(12, 0)))
            period = "Morning Peak";
        else if (now.isBefore(LocalTime.of(14, 0)))
            period = "Lunch";
        else if (now.isBefore(LocalTime.of(17, 0)))
            period = "Afternoon";
        else
            period = "Late Afternoon";

        String urgency;
        if (avgPerHour > 6)
            urgency = "HIGH";
        else if (avgPerHour > 3)
            urgency = "MEDIUM";
        else
            urgency = "LOW";

        return new StaffingRecommendation(recommendedDoctors, avgPerHour, urgency, period);
    }

    private double predictArrivalsAt(LocalDateTime target) {
        DayOfWeek dow = target.getDayOfWeek();
        int hour = target.getHour();

        // Get historical average for this hour + day-of-week combination
        String sql = """
                SELECT COALESCE(AVG(cnt), 0) AS avg_arrivals
                FROM (
                    SELECT DATE(created_at) AS d, COUNT(*) AS cnt
                    FROM queue
                    WHERE EXTRACT(DOW FROM created_at) = ?
                      AND EXTRACT(HOUR FROM created_at) = ?
                      AND created_at > NOW() - INTERVAL '90 days'
                    GROUP BY DATE(created_at)
                ) sub
                """;

        try {
            Double avg = jdbcTemplate.queryForObject(sql, Double.class,
                    dow.getValue() - 1, hour);
            return avg != null ? avg : getDefaultEstimate(hour);
        } catch (Exception e) {
            return getDefaultEstimate(hour);
        }
    }

    private double predictDepartmentArrivalsAt(int departmentId, LocalDateTime target) {
        DayOfWeek dow = target.getDayOfWeek();
        int hour = target.getHour();

        String sql = """
                SELECT COALESCE(AVG(cnt), 0) AS avg_arrivals
                FROM (
                    SELECT DATE(created_at) AS d, COUNT(*) AS cnt
                    FROM queue
                    WHERE department_id = ?
                      AND EXTRACT(DOW FROM created_at) = ?
                      AND EXTRACT(HOUR FROM created_at) = ?
                      AND created_at > NOW() - INTERVAL '90 days'
                    GROUP BY DATE(created_at)
                ) sub
                """;

        try {
            Double avg = jdbcTemplate.queryForObject(sql, Double.class,
                    departmentId, dow.getValue() - 1, hour);
            return avg != null ? avg : getDefaultDeptEstimate(hour);
        } catch (Exception e) {
            return getDefaultDeptEstimate(hour);
        }
    }

    private int toPostgresDow(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue();
    }

    private double getDefaultEstimate(int hour) {
        // Heuristic defaults when no historical data
        if (hour < 9)
            return 1.5;
        if (hour <= 11)
            return 5.0;
        if (hour == 12)
            return 2.0;
        if (hour <= 14)
            return 4.0;
        if (hour <= 16)
            return 3.5;
        if (hour <= 18)
            return 2.5;
        return 1.0;
    }

    private double getDefaultDeptEstimate(int hour) {
        return getDefaultEstimate(hour) / 6.0; // per department
    }

    private long getTotalWeeksOfData() {
        try {
            Long days = jdbcTemplate.queryForObject(
                    "SELECT EXTRACT(DAY FROM (MAX(created_at) - MIN(created_at))) FROM queue",
                    Long.class);
            return days != null ? Math.max(1, days / 7) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private String getConfidenceLevel(double predicted) {
        if (predicted >= 4.0)
            return "HIGH";
        if (predicted >= 2.0)
            return "MEDIUM";
        return "LOW";
    }

    private String analyzeTrend(List<HourlyPrediction> predictions) {
        if (predictions.size() < 2)
            return "STABLE";
        double first = predictions.get(0).predictedArrivals();
        double last = predictions.get(predictions.size() - 1).predictedArrivals();
        double diff = last - first;
        if (diff > 1.5)
            return "INCREASING";
        if (diff < -1.5)
            return "DECREASING";
        return "STABLE";
    }

    // Records

    public record HourlyPrediction(
            int hour,
            String dayOfWeek,
            double predictedArrivals,
            String confidence) {
    }

    public record FlowPrediction(
            List<HourlyPrediction> hourly,
            double totalPredictedArrivals,
            String trend,
            LocalDateTime predictedAt) {
    }

    public record DepartmentFlowPrediction(
            Map<String, List<HourlyPrediction>> byDepartment,
            int hoursAhead,
            LocalDateTime predictedAt) {
    }

    public record PeakHoursReport(
            String dayOfWeek,
            Map<Integer, Double> hourlyAverages,
            String peakHour,
            String offPeakHour,
            List<Integer> rushHours) {
    }

    public record StaffingRecommendation(
            int recommendedDoctors,
            double predictedPatientsPerHour,
            String urgencyLevel,
            String timePeriod) {
    }
}
