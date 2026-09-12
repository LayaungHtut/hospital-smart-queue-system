package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class MlModel {
    private Long modelId;
    private String modelName;
    private String modelType;
    private String framework;
    private String version;
    private String filePath;
    private String artifactsPath;
    private String metricsJson;
    private boolean active;
    private LocalDateTime trainedAt;
    private LocalDateTime deployedAt;
    private String createdBy;
    private String description;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getArtifactsPath() { return artifactsPath; }
    public void setArtifactsPath(String artifactsPath) { this.artifactsPath = artifactsPath; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getTrainedAt() { return trainedAt; }
    public void setTrainedAt(LocalDateTime trainedAt) { this.trainedAt = trainedAt; }
    public LocalDateTime getDeployedAt() { return deployedAt; }
    public void setDeployedAt(LocalDateTime deployedAt) { this.deployedAt = deployedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}