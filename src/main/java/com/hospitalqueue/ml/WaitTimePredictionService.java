package com.hospitalqueue.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.hospitalqueue.config.EnvConfig;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.WaitingTimeService;
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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ONNX Runtime inference service for predictive wait time.
 * Loads trained XGBoost/LightGBM model exported to ONNX format.
 */
@Service
public class WaitTimePredictionService {

    private static final Logger log = LoggerFactory.getLogger(WaitTimePredictionService.class);

    @SuppressWarnings("unused")
    private final EnvConfig env;
    private final ResourceLoader resourceLoader;
    private final QueueRepository queueRepository;
    private final WaitingTimeService waitingTimeService;

    private OrtEnvironment ortEnvironment;
    private OrtSession session;
    private Map<String, Integer> departmentEncoding;
    private Map<String, Integer> doctorEncoding;
    private List<String> featureColumns;
    private double[] scalerMean;
    private double[] scalerScale;
    private boolean modelLoaded = false;

    @Value("${ml.wait-time.model-path:models/wait_time_xgboost.onnx}")
    private String modelPath;

    @Value("${ml.wait-time.artifacts-path:models/wait_time_xgboost_artifacts.json}")
    private String artifactsPath;

    public WaitTimePredictionService(EnvConfig env, ResourceLoader resourceLoader,
            QueueRepository queueRepository, WaitingTimeService waitingTimeService) {
        this.env = env;
        this.resourceLoader = resourceLoader;
        this.queueRepository = queueRepository;
        this.waitingTimeService = waitingTimeService;
    }

    @PostConstruct
    public void init() {
        if (!isMlEnabled()) {
            log.info("ML wait time prediction disabled (no model configured)");
            return;
        }
        try {
            loadModel();
            loadArtifacts();
            modelLoaded = true;
            log.info("Wait time prediction model loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load wait time prediction model: {}", e.getMessage());
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
        Path tempFile = Files.createTempFile("wait_time_model_", ".onnx");
        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        ortEnvironment = OrtEnvironment.getEnvironment();
        session = ortEnvironment.createSession(tempFile.toString(), new OrtSession.SessionOptions());

        log.info("Model input names: {}", Arrays.toString(session.getInputInfo().keySet().toArray()));
        log.info("Model output names: {}", Arrays.toString(session.getOutputInfo().keySet().toArray()));

        tempFile.toFile().deleteOnExit();
    }

    private void loadArtifacts() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + artifactsPath);
        if (!resource.exists()) {
            log.warn("Artifacts file not found at {}, using defaults", artifactsPath);
            setDefaultArtifacts();
            return;
        }

        String json = new String(resource.getInputStream().readAllBytes());
        // Simple JSON parsing - in production use Jackson
        parseArtifacts(json);
    }

    private void parseArtifacts(String json) {
        // Simple parsing for demo - replace with Jackson ObjectMapper in production
        try {
            // Extract feature_columns
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

            // Extract dept_classes
            int dcStart = json.indexOf("\"dept_classes\"");
            if (dcStart > 0) {
                int arrStart = json.indexOf('[', dcStart);
                int arrEnd = json.indexOf(']', arrStart);
                String arrStr = json.substring(arrStart + 1, arrEnd).trim();
                String[] classes = arrStr.split(",");
                departmentEncoding = new HashMap<>();
                for (int i = 0; i < classes.length; i++) {
                    String cls = classes[i].replaceAll("[\"]", "").trim();
                    departmentEncoding.put(cls, i);
                }
            }

            // Extract doctor_classes
            int drStart = json.indexOf("\"doctor_classes\"");
            if (drStart > 0) {
                int arrStart = json.indexOf('[', drStart);
                int arrEnd = json.indexOf(']', arrStart);
                String arrStr = json.substring(arrStart + 1, arrEnd).trim();
                String[] classes = arrStr.split(",");
                doctorEncoding = new HashMap<>();
                for (int i = 0; i < classes.length; i++) {
                    String cls = classes[i].replaceAll("[\"]", "").trim();
                    doctorEncoding.put(cls, i);
                }
            }

            // Extract scaler params
            int smStart = json.indexOf("\"scaler_mean\"");
            if (smStart > 0) {
                int arrStart = json.indexOf('[', smStart);
                int arrEnd = json.indexOf(']', arrStart);
                String arrStr = json.substring(arrStart + 1, arrEnd).trim();
                if (!arrStr.isEmpty()) {
                    scalerMean = Arrays.stream(arrStr.split(","))
                            .map(String::trim)
                            .mapToDouble(Double::parseDouble)
                            .toArray();
                }
            }

            int ssStart = json.indexOf("\"scaler_scale\"");
            if (ssStart > 0) {
                int arrStart = json.indexOf('[', ssStart);
                int arrEnd = json.indexOf(']', arrStart);
                String arrStr = json.substring(arrStart + 1, arrEnd).trim();
                if (!arrStr.isEmpty()) {
                    scalerScale = Arrays.stream(arrStr.split(","))
                            .map(String::trim)
                            .mapToDouble(Double::parseDouble)
                            .toArray();
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse artifacts, using defaults: {}", e.getMessage());
            setDefaultArtifacts();
        }
    }

    private void setDefaultArtifacts() {
        featureColumns = List.of(
                "department_encoded", "doctor_encoded",
                "hour_sin", "hour_cos", "day_sin", "day_cos", "month_sin", "month_cos",
                "is_weekend", "is_peak_hour",
                "queue_length", "avg_consultation_minutes", "doctor_experience_years",
                "doctor_speed_factor", "patient_age", "patient_priority", "is_new_patient",
                "dept_avg_wait_last_hour", "doctor_avg_wait_last_hour", "dept_queue_trend",
                "queue_x_consultation", "queue_x_speed");

        departmentEncoding = Map.of(
                "CAR", 0, "NEU", 1, "ORT", 2, "GEN", 3, "PED", 4, "DER", 5);

        doctorEncoding = new ConcurrentHashMap<>();
        for (int i = 1; i <= 16; i++) {
            doctorEncoding.put(String.format("D%03d", i), i - 1);
        }

        scalerMean = new double[featureColumns.size()];
        scalerScale = new double[featureColumns.size()];
        Arrays.fill(scalerScale, 1.0);
    }

    /**
     * Predict wait time for a specific patient-doctor combination.
     */
    public Double predictWaitTime(Doctor doctor, Department department, int queueLength,
            int patientAge, int patientPriority, boolean isNewPatient) {
        if (!modelLoaded || session == null) {
            log.debug("Model not loaded, falling back to static calculation");
            return fallbackPrediction(doctor, queueLength);
        }

        try {
            float[] features = buildFeatures(doctor, department, queueLength, patientAge, patientPriority,
                    isNewPatient);
            float[] scaled = scaleFeatures(features);

            // Create input tensor
            long[] shape = new long[] { 1, scaled.length };
            FloatBuffer buffer = FloatBuffer.wrap(scaled);
            OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, buffer, shape);

            // Run inference
            Map<String, OnnxTensor> inputs = new HashMap<>();
            String inputName = session.getInputInfo().keySet().iterator().next();
            inputs.put(inputName, inputTensor);

            OrtSession.Result result = session.run(inputs);

            // Extract prediction
            float[][] output = (float[][]) result.get(0).getValue();
            double prediction = output[0][0];

            // Sanity check
            prediction = Math.max(0, Math.min(prediction, 480)); // Cap at 8 hours

            log.debug("Predicted wait time: {:.1f} min for Dr. {} (static: {:.1f})",
                    prediction, doctor.getDoctorId(), fallbackPrediction(doctor, queueLength));

            return prediction;

        } catch (Exception e) {
            log.warn("Prediction failed, using fallback: {}", e.getMessage());
            return fallbackPrediction(doctor, queueLength);
        }
    }

    /**
     * Predict wait times for all doctors in a department.
     */
    public Map<String, Double> predictWaitTimesForDepartment(Department department,
            List<Doctor> doctors,
            int patientAge, int patientPriority, boolean isNewPatient) {
        Map<String, Double> predictions = new HashMap<>();

        // Get current queue lengths for all doctors
        Map<String, Integer> queueCounts = queueRepository.countWaitingByAllDoctors();

        for (Doctor doctor : doctors) {
            int queueLength = queueCounts.getOrDefault(doctor.getDoctorId(), 0);
            Double waitTime = predictWaitTime(doctor, department, queueLength, patientAge, patientPriority,
                    isNewPatient);
            predictions.put(doctor.getDoctorId(), waitTime);
        }

        return predictions;
    }

    private float[] buildFeatures(Doctor doctor, Department department, int queueLength,
            int patientAge, int patientPriority, boolean isNewPatient) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int month = now.getMonthValue();

        int hour = time.getHour();
        int dayOfWeekNum = dayOfWeek.getValue() - 1; // 0=Monday
        boolean isWeekend = dayOfWeekNum >= 5;
        boolean isPeakHour = (hour >= 9 && hour <= 11) || (hour >= 14 && hour <= 16);

        double avgConsultation = doctor.getAverageConsultationMinutes() > 0 ? doctor.getAverageConsultationMinutes()
                : 15.0;
        double doctorExp = 10.0; // Default, could be stored in Doctor model
        double doctorSpeed = 1.0; // Could be learned per doctor

        // Get historical averages (simplified - in production query from DB)
        double deptAvgWait = waitingTimeService.getDepartmentAverageWait(department.getDepartmentId());
        double doctorAvgWait = waitingTimeService.getDoctorAverageWait(doctor.getDoctorId());
        double deptTrend = 0.0; // Could compute from recent history

        float[] features = new float[featureColumns.size()];
        int idx = 0;

        features[idx++] = departmentEncoding.getOrDefault(department.getDepartmentCode(), 0);
        features[idx++] = doctorEncoding.getOrDefault(doctor.getDoctorId(), 0);
        features[idx++] = (float) Math.sin(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.cos(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.sin(2 * Math.PI * dayOfWeekNum / 7);
        features[idx++] = (float) Math.cos(2 * Math.PI * dayOfWeekNum / 7);
        features[idx++] = (float) Math.sin(2 * Math.PI * month / 12);
        features[idx++] = (float) Math.cos(2 * Math.PI * month / 12);
        features[idx++] = isWeekend ? 1.0f : 0.0f;
        features[idx++] = isPeakHour ? 1.0f : 0.0f;
        features[idx++] = queueLength;
        features[idx++] = (float) avgConsultation;
        features[idx++] = (float) doctorExp;
        features[idx++] = (float) doctorSpeed;
        features[idx++] = patientAge;
        features[idx++] = patientPriority;
        features[idx++] = isNewPatient ? 1.0f : 0.0f;
        features[idx++] = (float) deptAvgWait;
        features[idx++] = (float) doctorAvgWait;
        features[idx++] = (float) deptTrend;
        features[idx++] = queueLength * (float) avgConsultation;
        features[idx++] = queueLength * (float) doctorSpeed;

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

    private double fallbackPrediction(Doctor doctor, int queueLength) {
        double avgConsult = doctor.getAverageConsultationMinutes() > 0 ? doctor.getAverageConsultationMinutes() : 15.0;
        return queueLength * avgConsult;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
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