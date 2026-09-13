package com.hospitalqueue.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.hospitalqueue.model.Appointment;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.repository.AppointmentRepository;
import com.hospitalqueue.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ONNX Runtime inference service for no-show prediction.
 */
@Service
public class NoShowPredictionService {

    private static final Logger log = LoggerFactory.getLogger(NoShowPredictionService.class);

    private final ResourceLoader resourceLoader;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    private OrtEnvironment ortEnvironment;
    private OrtSession session;
    private Map<String, Integer> departmentEncoding;
    private Map<String, Integer> doctorEncoding;
    private Map<String, Integer> genderEncoding;
    private Map<String, Integer> insuranceEncoding;
    private Map<String, Integer> reminderEncoding;
    private Map<String, Integer> apptTypeEncoding;
    private List<String> featureColumns;
    private double[] scalerMean;
    private double[] scalerScale;
    private double threshold = 0.5;
    private boolean modelLoaded = false;

    @Value("${ml.noshow.model-path:models/noshow_xgboost.onnx}")
    private String modelPath;

    @Value("${ml.noshow.artifacts-path:models/noshow_xgboost_artifacts.json}")
    private String artifactsPath;

    public NoShowPredictionService(ResourceLoader resourceLoader,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository) {
        this.resourceLoader = resourceLoader;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @PostConstruct
    public void init() {
        if (!isMlEnabled()) {
            log.info("ML no-show prediction disabled (no model configured)");
            return;
        }
        try {
            loadModel();
            loadArtifacts();
            modelLoaded = true;
            log.info("No-show prediction model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load no-show prediction model: {}", e.getMessage());
            modelLoaded = false;
        }
    }

    private boolean isMlEnabled() {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + modelPath);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private void loadModel() throws IOException, OrtException {
        Resource resource = resourceLoader.getResource("classpath:" + modelPath);
        Path tempFile = Files.createTempFile("noshow_model_", ".onnx");
        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        ortEnvironment = OrtEnvironment.getEnvironment();
        session = ortEnvironment.createSession(tempFile.toString(), new OrtSession.SessionOptions());

        tempFile.toFile().deleteOnExit();
    }

    private void loadArtifacts() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + artifactsPath);
        if (!resource.exists()) {
            log.warn("Artifacts file not found, using defaults");
            setDefaultArtifacts();
            return;
        }

        String json = new String(resource.getInputStream().readAllBytes());
        parseArtifacts(json);
    }

    private void parseArtifacts(String json) {
        try {
            int fcStart = json.indexOf("\"feature_columns\"");
            if (fcStart > 0) {
                int arrStart = json.indexOf('[', fcStart);
                int arrEnd = json.indexOf(']', arrStart);
                String arrStr = json.substring(arrStart + 1, arrEnd).trim();
                featureColumns = Arrays.stream(arrStr.split(","))
                        .map(s -> s.replaceAll("[\"]", "").trim())
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            // Parse encoders
            parseEncoder(json, "dept_classes", departmentEncoding = new HashMap<>());
            parseEncoder(json, "doctor_classes", doctorEncoding = new HashMap<>());
            parseEncoder(json, "gender_classes", genderEncoding = new HashMap<>());
            parseEncoder(json, "insurance_classes", insuranceEncoding = new HashMap<>());
            parseEncoder(json, "reminder_classes", reminderEncoding = new HashMap<>());
            parseEncoder(json, "appt_type_classes", apptTypeEncoding = new HashMap<>());

            // Scaler
            parseScaler(json, "scaler_mean", scalerMean = new double[0]);
            parseScaler(json, "scaler_scale", scalerScale = new double[0]);

            // Threshold
            int thStart = json.indexOf("\"threshold\"");
            if (thStart > 0) {
                int colon = json.indexOf(':', thStart);
                int end = Math.min(
                        json.indexOf(',', colon),
                        json.indexOf('}', colon));
                if (end < 0)
                    end = json.length();
                threshold = Double.parseDouble(json.substring(colon + 1, end).trim());
            }

        } catch (Exception e) {
            log.warn("Failed to parse artifacts: {}", e.getMessage());
            setDefaultArtifacts();
        }
    }

    private void parseEncoder(String json, String key, Map<String, Integer> map) {
        int start = json.indexOf("\"" + key + "\"");
        if (start > 0) {
            int arrStart = json.indexOf('[', start);
            int arrEnd = json.indexOf(']', arrStart);
            String arrStr = json.substring(arrStart + 1, arrEnd).trim();
            String[] classes = arrStr.split(",");
            for (int i = 0; i < classes.length; i++) {
                String cls = classes[i].replaceAll("[\"]", "").trim();
                if (!cls.isEmpty())
                    map.put(cls, i);
            }
        }
    }

    private void parseScaler(String json, String key, double[] arr) {
        int start = json.indexOf("\"" + key + "\"");
        if (start > 0) {
            int arrStart = json.indexOf('[', start);
            int arrEnd = json.indexOf(']', arrStart);
            String arrStr = json.substring(arrStart + 1, arrEnd).trim();
            if (!arrStr.isEmpty()) {
                arr = Arrays.stream(arrStr.split(","))
                        .map(String::trim)
                        .mapToDouble(Double::parseDouble)
                        .toArray();
            }
        }
    }

    private void setDefaultArtifacts() {
        featureColumns = List.of(
                "department_encoded", "doctor_encoded",
                "days_ahead", "hour_sin", "hour_cos", "day_sin", "day_cos", "month_sin", "month_cos",
                "is_weekend",
                "patient_age", "gender_encoded", "distance_log", "insurance_encoded",
                "total_past_appointments", "past_noshows", "past_cancellations", "noshow_rate",
                "days_since_log", "lead_time_log",
                "is_followup", "priority", "appt_type_encoded",
                "temperature", "precipitation", "is_bad_weather",
                "received_reminder", "reminder_encoded", "confirmed_appointment",
                "noshow_rate_x_appointments", "reminder_x_confirm");

        departmentEncoding = Map.of("CAR", 0, "NEU", 1, "ORT", 2, "GEN", 3, "PED", 4, "DER", 5);
        doctorEncoding = new ConcurrentHashMap<>();
        for (int i = 1; i <= 16; i++)
            doctorEncoding.put(String.format("D%03d", i), i - 1);
        genderEncoding = Map.of("M", 0, "F", 1);
        insuranceEncoding = Map.of("public", 0, "private", 1, "self_pay", 2);
        reminderEncoding = Map.of("sms", 0, "email", 1, "call", 2, "none", 3);
        apptTypeEncoding = Map.of("consultation", 0, "procedure", 1, "checkup", 2);

        scalerMean = new double[featureColumns.size()];
        scalerScale = new double[featureColumns.size()];
        Arrays.fill(scalerScale, 1.0);
    }

    /**
     * Predict no-show probability for an appointment.
     * Returns probability [0,1] where higher = more likely to no-show.
     */
    public Double predictNoShowProbability(Appointment appointment) {
        if (!modelLoaded || session == null) {
            return fallbackPrediction(appointment);
        }

        try {
            float[] features = buildFeatures(appointment);
            float[] scaled = scaleFeatures(features);

            long[] shape = new long[] { 1, scaled.length };
            FloatBuffer buffer = FloatBuffer.wrap(scaled);
            OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, buffer, shape);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            String inputName = session.getInputInfo().keySet().iterator().next();
            inputs.put(inputName, inputTensor);

            OrtSession.Result result = session.run(inputs);
            float[][] output = (float[][]) result.get(0).getValue();

            // For binary classification, output is probability of class 1 (no-show)
            double probability = output[0].length > 1 ? output[0][1] : output[0][0];

            return Math.max(0, Math.min(1, probability));

        } catch (Exception e) {
            log.warn("No-show prediction failed: {}", e.getMessage());
            return fallbackPrediction(appointment);
        }
    }

    /**
     * Predict if patient will no-show (binary decision).
     */
    public boolean predictNoShow(Appointment appointment) {
        Double prob = predictNoShowProbability(appointment);
        return prob != null && prob >= threshold;
    }

    /**
     * Get risk level: LOW (<30%), MEDIUM (30-60%), HIGH (>60%)
     */
    public String getRiskLevel(Appointment appointment) {
        return getRiskLevel(predictNoShowProbability(appointment));
    }

    /**
     * Same as {@link #getRiskLevel(Appointment)} but reuses an already-computed
     * probability instead of re-running feature extraction + inference.
     */
    public String getRiskLevel(Double probability) {
        if (probability == null)
            return "UNKNOWN";
        if (probability < 0.3)
            return "LOW";
        if (probability < 0.6)
            return "MEDIUM";
        return "HIGH";
    }

    private float[] buildFeatures(Appointment appointment) {
        Patient patient = patientRepository.findById(appointment.getPatientId());
        String doctorId = appointment.getDoctorId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime apptDateTime = appointment.getAppointmentDate().atTime(appointment.getAppointmentTime());

        long daysAhead = ChronoUnit.DAYS.between(now.toLocalDate(), apptDateTime.toLocalDate());
        int hour = apptDateTime.getHour();
        int dayOfWeek = apptDateTime.getDayOfWeek().getValue() - 1;
        int month = apptDateTime.getMonthValue();
        boolean isWeekend = dayOfWeek >= 5;

        // Patient features
        int patientAge = patient != null ? calculateAge(patient.getDateOfBirth()) : 30;
        String gender = patient != null ? patient.getGender() : "M";
        double distance = (patient != null && patient.getDistanceKm() != null) ? patient.getDistanceKm() : 15.0;
        String insurance = patient != null ? patient.getInsuranceType() : "public";

        // History (single round trip instead of 5 separate queries)
        var history = appointmentRepository.getPatientHistoryStats(appointment.getPatientId());
        int totalAppts = history.totalAppointments();
        int pastNoshows = history.pastNoshows();
        int pastCancels = history.pastCancellations();
        double noshowRate = totalAppts > 0 ? (double) pastNoshows / totalAppts : 0;

        int daysSinceLast = history.daysSinceLastVisit() != null ? history.daysSinceLastVisit() : 365;
        int avgLeadTime = history.averageLeadTime() != null ? history.averageLeadTime() : 14;

        // Appointment features
        boolean isFollowup = appointment.isFollowup();
        int priority = 3;
        try {
            priority = appointment.getPriority() != null ? Integer.parseInt(appointment.getPriority()) : 3;
        } catch (NumberFormatException e) {
            priority = 3;
        }
        String apptType = appointment.getAppointmentType() != null ? appointment.getAppointmentType() : "consultation";

        // Weather (simplified - in production integrate weather API)
        double temperature = 22.0;
        double precipitation = 0.0;
        boolean isBadWeather = false;

        // Communication
        boolean receivedReminder = appointment.isReminderSent();
        String reminderChannel = appointment.getReminderChannel() != null ? appointment.getReminderChannel() : "sms";
        boolean confirmed = appointment.isConfirmed();

        float[] features = new float[featureColumns.size()];
        int idx = 0;

        features[idx++] = departmentEncoding.getOrDefault(appointment.getDepartmentCode(), 0);
        features[idx++] = doctorEncoding.getOrDefault(doctorId, 0);
        features[idx++] = (float) daysAhead;
        features[idx++] = (float) Math.sin(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.cos(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.sin(2 * Math.PI * dayOfWeek / 7);
        features[idx++] = (float) Math.cos(2 * Math.PI * dayOfWeek / 7);
        features[idx++] = (float) Math.sin(2 * Math.PI * month / 12);
        features[idx++] = (float) Math.cos(2 * Math.PI * month / 12);
        features[idx++] = isWeekend ? 1.0f : 0.0f;
        features[idx++] = patientAge;
        features[idx++] = genderEncoding.getOrDefault(gender, 0);
        features[idx++] = (float) Math.log1p(distance);
        features[idx++] = insuranceEncoding.getOrDefault(insurance, 0);
        features[idx++] = totalAppts;
        features[idx++] = pastNoshows;
        features[idx++] = pastCancels;
        features[idx++] = (float) noshowRate;
        features[idx++] = (float) Math.log1p(daysSinceLast);
        features[idx++] = (float) Math.log1p(avgLeadTime);
        features[idx++] = isFollowup ? 1.0f : 0.0f;
        features[idx++] = priority;
        features[idx++] = apptTypeEncoding.getOrDefault(apptType, 0);
        features[idx++] = (float) temperature;
        features[idx++] = (float) precipitation;
        features[idx++] = isBadWeather ? 1.0f : 0.0f;
        features[idx++] = receivedReminder ? 1.0f : 0.0f;
        features[idx++] = reminderEncoding.getOrDefault(reminderChannel, 0);
        features[idx++] = confirmed ? 1.0f : 0.0f;
        features[idx++] = (float) (noshowRate * Math.log1p(totalAppts));
        features[idx++] = (receivedReminder && confirmed) ? 1.0f : 0.0f;

        return features;
    }

    private float[] scaleFeatures(float[] features) {
        if (scalerMean == null || scalerScale == null || scalerMean.length != features.length) {
            return features;
        }
        float[] scaled = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            double scale = scalerScale[i] != 0 ? scalerScale[i] : 1.0;
            scaled[i] = (float) ((features[i] - scalerMean[i]) / scale);
        }
        return scaled;
    }

    private double fallbackPrediction(Appointment appointment) {
        // Simple heuristic fallback
        Patient patient = patientRepository.findById(appointment.getPatientId());
        if (patient == null)
            return 0.15;

        var history = appointmentRepository.getPatientHistoryStats(appointment.getPatientId());
        int totalAppts = history.totalAppointments();
        int pastNoshows = history.pastNoshows();

        if (totalAppts == 0)
            return 0.15; // Base rate for new patients
        return Math.min(0.8, (double) pastNoshows / totalAppts + 0.05);
    }

    private int calculateAge(java.time.LocalDate dob) {
        if (dob == null)
            return 30;
        return (int) ChronoUnit.YEARS.between(dob, java.time.LocalDate.now());
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public double getThreshold() {
        return threshold;
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null)
                session.close();
            if (ortEnvironment != null)
                ortEnvironment.close();
        } catch (Exception e) {
            log.warn("Error closing ONNX session: {}", e.getMessage());
        }
    }
}