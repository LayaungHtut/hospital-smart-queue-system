package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class TriageAssessment {
    private Long assessmentId;
    private String patientId;
    private Long queueId;
    private Integer departmentId;
    private String symptomsText;
    private String recommendedDeptCode;
    private Boolean emergencyFlag;
    private Integer acuityScore;
    private String disposition;
    private String recommendedTests;
    private String recommendedLabs;
    private String reason;
    private Boolean aiUsed;
    private String modelVersion;
    private Integer staffAcuityScore;
    private String staffDisposition;
    private String staffNotes;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public Long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public Long getQueueId() { return queueId; }
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public String getSymptomsText() { return symptomsText; }
    public void setSymptomsText(String symptomsText) { this.symptomsText = symptomsText; }
    public String getRecommendedDeptCode() { return recommendedDeptCode; }
    public void setRecommendedDeptCode(String recommendedDeptCode) { this.recommendedDeptCode = recommendedDeptCode; }
    public Boolean isEmergencyFlag() { return emergencyFlag; }
    public void setEmergencyFlag(Boolean emergencyFlag) { this.emergencyFlag = emergencyFlag; }
    public Integer getAcuityScore() { return acuityScore; }
    public void setAcuityScore(Integer acuityScore) { this.acuityScore = acuityScore; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public String getRecommendedTests() { return recommendedTests; }
    public void setRecommendedTests(String recommendedTests) { this.recommendedTests = recommendedTests; }
    public String getRecommendedLabs() { return recommendedLabs; }
    public void setRecommendedLabs(String recommendedLabs) { this.recommendedLabs = recommendedLabs; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean isAiUsed() { return aiUsed; }
    public void setAiUsed(Boolean aiUsed) { this.aiUsed = aiUsed; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Integer getStaffAcuityScore() { return staffAcuityScore; }
    public void setStaffAcuityScore(Integer staffAcuityScore) { this.staffAcuityScore = staffAcuityScore; }
    public String getStaffDisposition() { return staffDisposition; }
    public void setStaffDisposition(String staffDisposition) { this.staffDisposition = staffDisposition; }
    public String getStaffNotes() { return staffNotes; }
    public void setStaffNotes(String staffNotes) { this.staffNotes = staffNotes; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}