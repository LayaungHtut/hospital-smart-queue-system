package com.hospitalqueue.rule;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Department recommendation by symptom keywords (Rule 5).
 * This is the local fallback used when the OpenRouter AI is unavailable.
 */
@Component
public class DepartmentRule {

    public static final int DEPARTMENT_CARDIOLOGY = 1;
    public static final int DEPARTMENT_NEUROLOGY = 2;
    public static final int DEPARTMENT_ORTHOPEDICS = 3;
    public static final int DEPARTMENT_GENERAL = 4;
    public static final int DEPARTMENT_PEDIATRICS = 5;
    public static final int DEPARTMENT_DERMATOLOGY = 6;

    private static final Map<Integer, List<String>> SYMPTOM_KEYWORDS = new HashMap<>();

    static {
        SYMPTOM_KEYWORDS.put(DEPARTMENT_CARDIOLOGY, Arrays.asList("chest", "heart", "palpitation", "blood pressure", "breath", "cardiac"));
        SYMPTOM_KEYWORDS.put(DEPARTMENT_NEUROLOGY, Arrays.asList("headache", "migraine", "dizzy", "seizure", "stroke", "numb", "neurolog"));
        SYMPTOM_KEYWORDS.put(DEPARTMENT_ORTHOPEDICS, Arrays.asList("bone", "joint", "knee", "back", "fracture", "muscle", "sprain", "leg", "arm", "ortho"));
        SYMPTOM_KEYWORDS.put(DEPARTMENT_PEDIATRICS, Arrays.asList("child", "baby", "infant", "son", "daughter", "pediatr"));
        SYMPTOM_KEYWORDS.put(DEPARTMENT_DERMATOLOGY, Arrays.asList("rash", "skin", "itch", "acne", "allergy", "hair", "pimple", "derm"));
    }

    private static final List<String> EMERGENCY_SYMPTOMS = Arrays.asList(
            "unconscious", "severe bleeding", "difficulty breathing", "stroke", "heart attack", "seizure",
            "chest pain", "cannot breathe", "heavy bleeding", "paralysis");

    /**
     * Maps symptoms to a department ID, or GENERAL when no keyword matches.
     */
    public int mapSymptomToDepartment(String symptoms) {
        if (symptoms == null || symptoms.trim().isEmpty()) {
            return 0;
        }
        String normalized = symptoms.toLowerCase();
        for (Map.Entry<Integer, List<String>> entry : SYMPTOM_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalized.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return DEPARTMENT_GENERAL;
    }

    /**
     * Rule 6: emergency screening - do the symptoms suggest an emergency.
     */
    public boolean containsEmergencySymptom(String symptoms) {
        if (symptoms == null) {
            return false;
        }
        String normalized = symptoms.toLowerCase();
        for (String symptom : EMERGENCY_SYMPTOMS) {
            if (normalized.contains(symptom)) {
                return true;
            }
        }
        return false;
    }
}
