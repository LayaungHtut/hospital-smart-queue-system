package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class WaitTimePrediction {
    private Long predictionId;
    private Long queueId;
    private String patientId;
    private String doctorId;
    private Integer departmentId;
    private Long modelId;
    private String featuresJson;
    private Double predictedWaitMin;
    private Double actualWaitMin;
    private LocalDateTime predictedAt;
    private LocalDateTime resolvedAt;

    public Long getPredictionId() { return predictionId; }
    public void setPredictionId(Long predictionId) { this.predictionId = predictionId; }
    public Long getQueueId() { return queueId; }
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public Double getPredictedWaitMin() { return predictedWaitMin; }
    public void setPredictedWaitMin(Double predictedWaitMin) { this.predictedWaitMin = predictedWaitMin; }
    public Double getActualWaitMin() { return actualWaitMin; }
    public void setActualWaitMin(Double actualWaitMin) { this.actualWaitMin = actualWaitMin; }
    public LocalDateTime getPredictedAt() { return predictedAt; }
    public void setPredictedAt(LocalDateTime predictedAt) { this.predictedAt = predictedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}