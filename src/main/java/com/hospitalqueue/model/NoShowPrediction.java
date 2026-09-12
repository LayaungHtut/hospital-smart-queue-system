package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class NoShowPrediction {
    private Long predictionId;
    private Long appointmentId;
    private String patientId;
    private String doctorId;
    private Long modelId;
    private String featuresJson;
    private Double predictedProbability;
    private String riskLevel;
    private Double thresholdUsed;
    private Boolean predictedNoShow;
    private Boolean actualNoShow;
    private LocalDateTime predictedAt;
    private LocalDateTime resolvedAt;

    public Long getPredictionId() { return predictionId; }
    public void setPredictionId(Long predictionId) { this.predictionId = predictionId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public Double getPredictedProbability() { return predictedProbability; }
    public void setPredictedProbability(Double predictedProbability) { this.predictedProbability = predictedProbability; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Double getThresholdUsed() { return thresholdUsed; }
    public void setThresholdUsed(Double thresholdUsed) { this.thresholdUsed = thresholdUsed; }
    public Boolean isPredictedNoShow() { return predictedNoShow; }
    public void setPredictedNoShow(Boolean predictedNoShow) { this.predictedNoShow = predictedNoShow; }
    public Boolean getActualNoShow() { return actualNoShow; }
    public void setActualNoShow(Boolean actualNoShow) { this.actualNoShow = actualNoShow; }
    public LocalDateTime getPredictedAt() { return predictedAt; }
    public void setPredictedAt(LocalDateTime predictedAt) { this.predictedAt = predictedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}