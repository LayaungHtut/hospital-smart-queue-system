package com.hospitalqueue.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.rule.DepartmentRule;
import com.hospitalqueue.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Uses the OpenRouter free LLM to recommend a department from free-text
 * symptoms, with the local keyword-based DepartmentRule as a fallback.
 * Now includes triage: acuity score (1-5) and recommended tests/labs.
 */
@Service
public class AIRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AIRecommendationService.class);

    private final OpenRouterClient openRouterClient;
    private final DepartmentRule departmentRule;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIRecommendationService(OpenRouterClient openRouterClient,
                                   DepartmentRule departmentRule,
                                   DepartmentRepository departmentRepository) {
        this.openRouterClient = openRouterClient;
        this.departmentRule = departmentRule;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Result of an AI / local recommendation with triage.
     */
    public static class Recommendation {
        private Department department;
        private boolean emergency;
        private String reason;
        private boolean aiUsed;
        private int acuityScore; // 1-5 (1=non-urgent, 5=resuscitation)
        private List<String> recommendedTests;
        private List<String> recommendedLabs;
        private String disposition; // "routine", "urgent", "emergency"

        public Department getDepartment() { return department; }
        public void setDepartment(Department department) { this.department = department; }
        public boolean isEmergency() { return emergency; }
        public void setEmergency(boolean emergency) { this.emergency = emergency; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public boolean isAiUsed() { return aiUsed; }
        public void setAiUsed(boolean aiUsed) { this.aiUsed = aiUsed; }
        public int getAcuityScore() { return acuityScore; }
        public void setAcuityScore(int acuityScore) { this.acuityScore = acuityScore; }
        public List<String> getRecommendedTests() { return recommendedTests; }
        public void setRecommendedTests(List<String> recommendedTests) { this.recommendedTests = recommendedTests; }
        public List<String> getRecommendedLabs() { return recommendedLabs; }
        public void setRecommendedLabs(List<String> recommendedLabs) { this.recommendedLabs = recommendedLabs; }
        public String getDisposition() { return disposition; }
        public void setDisposition(String disposition) { this.disposition = disposition; }
    }

    /**
     * Recommends a department for the given symptoms with full triage.
     * Tries the AI first, then falls back to keyword rules.
     */
    public Recommendation recommend(String symptoms) {
        Recommendation result = new Recommendation();
        if (symptoms == null || symptoms.trim().isEmpty()) {
            return result;
        }

        Recommendation ai = tryAI(symptoms);
        if (ai != null && ai.getDepartment() != null) {
            return ai;
        }

        // Local fallback with basic triage
        int departmentId = departmentRule.mapSymptomToDepartment(symptoms);
        if (departmentId > 0) {
            result.setDepartment(departmentRepository.findById(departmentId));
        }
        boolean isEmergency = departmentRule.containsEmergencySymptom(symptoms);
        result.setEmergency(isEmergency);
        result.setAcuityScore(isEmergency ? 5 : 3);
        result.setDisposition(isEmergency ? "emergency" : "routine");
        result.setReason("Recommended locally by symptom keyword matching.");
        result.setAiUsed(false);
        result.setRecommendedTests(getDefaultTestsForDepartment(result.getDepartment()));
        result.setRecommendedLabs(getDefaultLabsForDepartment(result.getDepartment()));
        return result;
    }

    /**
     * Enhanced triage-only endpoint for existing department.
     */
    public Recommendation triageOnly(String symptoms, Department department) {
        Recommendation result = new Recommendation();
        result.setDepartment(department);
        
        Recommendation ai = tryAITriage(symptoms, department);
        if (ai != null) {
            return ai;
        }

        // Local fallback triage
        boolean isEmergency = departmentRule.containsEmergencySymptom(symptoms);
        result.setEmergency(isEmergency);
        result.setAcuityScore(calculateLocalAcuity(symptoms, isEmergency));
        result.setDisposition(getDispositionFromAcuity(result.getAcuityScore()));
        result.setReason("Triage assessment by local keyword rules.");
        result.setAiUsed(false);
        result.setRecommendedTests(getDefaultTestsForDepartment(department));
        result.setRecommendedLabs(getDefaultLabsForDepartment(department));
        return result;
    }

    private Recommendation tryAI(String symptoms) {
        String systemPrompt = buildSystemPrompt();
        String raw = openRouterClient.chat(systemPrompt, "Symptoms: " + symptoms);
        if (raw == null) return null;

        return parseAIResponse(raw);
    }

    private Recommendation tryAITriage(String symptoms, Department department) {
        String systemPrompt = buildTriagePrompt(department);
        String raw = openRouterClient.chat(systemPrompt, "Symptoms: " + symptoms);
        if (raw == null) return null;

        return parseAIResponse(raw);
    }

    private String buildSystemPrompt() {
        return "You are a medical triage assistant for a hospital queue system. "
                + "Analyze the patient's symptoms and respond with a JSON object containing:\n"
                + "1. \"department\": One of CAR, NEU, ORT, GEN, PED, DER\n"
                + "2. \"emergency\": true/false\n"
                + "3. \"reason\": Brief one-sentence explanation of why this department is recommended\n\n"
                + "Focus on: which department should the patient visit, and is it an emergency.\n"
                + "ESSENTIAL: Respond ONLY with valid JSON. No extra text.";
    }

    private String buildTriagePrompt(Department department) {
        return "You are a medical triage nurse. The patient is already assigned to " + department.getDepartmentName()
                + " (" + department.getDepartmentCode() + "). "
                + "Assess urgency level. Respond with JSON:\n"
                + "{\"emergency\": true/false, \"reason\": \"Brief explanation\"}\n"
                + "ESSENTIAL: Respond ONLY with valid JSON.";
    }

    private Recommendation parseAIResponse(String raw) {
        try {
            String cleaned = raw;
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = raw.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(cleaned);
            
            String code = node.path("department").asText("").trim().toUpperCase();
            Department department = departmentRepository.findByCode(code);
            if (department == null && node.has("department")) {
                // Try triage-only response (no department field)
                department = null;
            }

            Recommendation result = new Recommendation();
            result.setDepartment(department);
            result.setEmergency(node.path("emergency").asBoolean(false));
            result.setAcuityScore(node.path("acuity_score").asInt(3));
            result.setDisposition(node.path("disposition").asText("routine"));
            result.setReason(node.path("reason").asText("AI triage assessment."));
            result.setAiUsed(true);
            
            // Parse arrays
            JsonNode testsNode = node.path("recommended_tests");
            if (testsNode.isArray()) {
                result.setRecommendedTests(objectMapper.convertValue(testsNode, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
            JsonNode labsNode = node.path("recommended_labs");
            if (labsNode.isArray()) {
                result.setRecommendedLabs(objectMapper.convertValue(labsNode,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
            
            // Validate acuity
            result.setAcuityScore(Math.max(1, Math.min(5, result.getAcuityScore())));
            if (result.getDisposition() == null || result.getDisposition().isBlank()) {
                result.setDisposition(getDispositionFromAcuity(result.getAcuityScore()));
            }
            
            return result;
        } catch (Exception e) {
            log.warn("Could not parse AI response: {}", raw);
            return null;
        }
    }

    private int calculateLocalAcuity(String symptoms, boolean isEmergency) {
        if (isEmergency) return 5;
        
        String lower = symptoms.toLowerCase();
        int score = 2; // baseline
        
        // High acuity keywords
        if (lower.matches(".*\\b(chest pain|shortness of breath|difficulty breathing|severe|acute|sudden|crushing|radiating)\\b.*")) score = 4;
        if (lower.matches(".*\\b(unconscious|unresponsive|seizure|stroke|heart attack|anaphylaxis)\\b.*")) return 5;
        
        // Moderate acuity
        if (lower.matches(".*\\b(fever|vomiting|diarrhea|dehydration|infection|swelling|fracture|sprain)\\b.*")) score = Math.max(score, 3);
        
        // Low acuity
        if (lower.matches(".*\\b(rash|itch|mild|cold|flu|checkup|followup|refill|prescription)\\b.*")) score = Math.min(score, 2);
        
        return score;
    }

    private String getDispositionFromAcuity(int acuity) {
        return switch (acuity) {
            case 5 -> "emergency";
            case 4 -> "urgent";
            case 3 -> "urgent";
            case 2 -> "routine";
            default -> "routine";
        };
    }

    private List<String> getDefaultTestsForDepartment(Department dept) {
        if (dept == null) return List.of();
        return switch (dept.getDepartmentCode()) {
            case "CAR" -> List.of("ECG", "Chest X-ray", "Echocardiogram");
            case "NEU" -> List.of("CT Head", "MRI Brain", "EEG");
            case "ORT" -> List.of("X-ray", "CT Scan", "MRI");
            case "PED" -> List.of("Chest X-ray", "Ultrasound");
            case "DER" -> List.of("Dermoscopy", "Skin Biopsy");
            default -> List.of("Chest X-ray", "Ultrasound");
        };
    }

    private List<String> getDefaultLabsForDepartment(Department dept) {
        if (dept == null) return List.of();
        return switch (dept.getDepartmentCode()) {
            case "CAR" -> List.of("Troponin", "CBC", "BMP", "Lipid Panel", "D-dimer", "BNP");
            case "NEU" -> List.of("CBC", "BMP", "ESR", "CRP", "Vitamin B12", "Thyroid Panel");
            case "ORT" -> List.of("CBC", "BMP", "CRP", "ESR", "Vitamin D", "Calcium");
            case "PED" -> List.of("CBC", "BMP", "CRP", "Strep Test", "Flu/COVID Panel");
            case "DER" -> List.of("CBC", "ESR", "CRP", "ANA", "IgE");
            default -> List.of("CBC", "BMP", "CRP", "ESR");
        };
    }
}
