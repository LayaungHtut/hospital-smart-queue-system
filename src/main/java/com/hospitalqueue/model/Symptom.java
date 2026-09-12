package com.hospitalqueue.model;

public class Symptom {

    private int symptomId;
    private String symptomName;
    private String symptomCode;
    private String category;
    private boolean active;

    public Symptom() {
    }

    public Symptom(int symptomId, String symptomName, String symptomCode, String category) {
        this.symptomId = symptomId;
        this.symptomName = symptomName;
        this.symptomCode = symptomCode;
        this.category = category;
        this.active = true;
    }

    public int getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(int symptomId) {
        this.symptomId = symptomId;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    public String getSymptomCode() {
        return symptomCode;
    }

    public void setSymptomCode(String symptomCode) {
        this.symptomCode = symptomCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
