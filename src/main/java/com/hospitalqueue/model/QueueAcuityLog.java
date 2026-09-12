package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class QueueAcuityLog {
    private Long logId;
    private Long queueId;
    private Long triageId;
    private int originalPriority;
    private int escalatedPriority;
    private int acuityScore;
    private String disposition;
    private String escalationReason;
    private String triggeredBy;
    private LocalDateTime createdAt;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getQueueId() { return queueId; }
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public Long getTriageId() { return triageId; }
    public void setTriageId(Long triageId) { this.triageId = triageId; }
    public int getOriginalPriority() { return originalPriority; }
    public void setOriginalPriority(int originalPriority) { this.originalPriority = originalPriority; }
    public int getEscalatedPriority() { return escalatedPriority; }
    public void setEscalatedPriority(int escalatedPriority) { this.escalatedPriority = escalatedPriority; }
    public int getAcuityScore() { return acuityScore; }
    public void setAcuityScore(int acuityScore) { this.acuityScore = acuityScore; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public String getEscalationReason() { return escalationReason; }
    public void setEscalationReason(String escalationReason) { this.escalationReason = escalationReason; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}