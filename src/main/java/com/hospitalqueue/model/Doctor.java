package com.hospitalqueue.model;

import java.time.LocalTime;

public class Doctor {

    private String doctorId;
    private String doctorCode;
    private String name;
    private int departmentId;
    private String specialization;
    private String phone;
    private String email;
    private String passwordHash;
    private long maxQueueSize;
    private LocalTime queueOpenTime;
    private LocalTime queueCloseTime;
    private long averageConsultationMinutes;
    private String qualification;
    private int yearsOfExperience = 5;
    private boolean available;
    private boolean active;
    private boolean computedAvailable;

    public Doctor() {
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public long getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(long maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public LocalTime getQueueOpenTime() {
        return queueOpenTime;
    }

    public void setQueueOpenTime(LocalTime queueOpenTime) {
        this.queueOpenTime = queueOpenTime;
    }

    public LocalTime getQueueCloseTime() {
        return queueCloseTime;
    }

    public void setQueueCloseTime(LocalTime queueCloseTime) {
        this.queueCloseTime = queueCloseTime;
    }

    public long getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }

    public void setAverageConsultationMinutes(long averageConsultationMinutes) {
        this.averageConsultationMinutes = averageConsultationMinutes;
    }

    public String getQualification() {
        return qualification != null ? qualification : "MBBS, M.Med.Sc";
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience > 0 ? yearsOfExperience : 5;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Computed at request time by the smart queue engine (not persisted).
     * True when the doctor is currently available to take new patients.
     */
    public boolean isComputedAvailable() {
        return computedAvailable;
    }

    public void setComputedAvailable(boolean computedAvailable) {
        this.computedAvailable = computedAvailable;
    }
}
