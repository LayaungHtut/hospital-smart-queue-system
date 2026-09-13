package com.hospitalqueue.model;

import java.time.LocalDateTime;

public class Queue {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_CALLED = "CALLED";
    public static final String STATUS_SERVING = "SERVING";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String PRIORITY_EMERGENCY = "EMERGENCY";
    public static final String PRIORITY_APPOINTMENT = "APPOINTMENT";
    public static final String PRIORITY_NORMAL = "NORMAL";

    public static final String SOURCE_ONLINE = "ONLINE";
    public static final String SOURCE_WALK_IN = "WALK_IN";

    private long queueId;
    private String queueNumber;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private int departmentId;
    private String departmentName;
    private String priority;
    private String status;
    private int position;
    private long estimatedWaitingTime;
    private String source;
    private boolean emergency;
    private boolean emergencyConfirmed;
    private boolean reminderSent;
    private LocalDateTime calledAt;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime checkedInAt;

    public Queue() {
    }

    public Queue(String queueNumber, String patientId, String doctorId, int departmentId, String priority, String status) {
        this.queueNumber = queueNumber;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.departmentId = departmentId;
        this.priority = priority;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public String getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(String queueNumber) {
        this.queueNumber = queueNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getEstimatedWaitingTime() {
        return estimatedWaitingTime;
    }

    public void setEstimatedWaitingTime(long estimatedWaitingTime) {
        this.estimatedWaitingTime = estimatedWaitingTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
    }

    public boolean isEmergencyConfirmed() {
        return emergencyConfirmed;
    }

    public void setEmergencyConfirmed(boolean emergencyConfirmed) {
        this.emergencyConfirmed = emergencyConfirmed;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
