package com.hospitalqueue.ml;

import com.hospitalqueue.ai.OpenRouterClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates personalized post-visit follow-up instructions for patients
 * using the OpenRouter LLM API.
 *
 * Input: diagnosis, medications, patient age/gender, department
 * Output: structured follow-up instructions (diet, medications, warning signs, follow-up date)
 */
@Service
public class FollowUpInstructionService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpInstructionService.class);

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FollowUpInstructionService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    /**
     * Generate follow-up instructions using AI.
     */
    public FollowUpResult generateInstructions(String diagnosis, String medications,
                                                String patientAge, String patientGender,
                                                String department, String additionalNotes) {
        FollowUpResult result = null;

        if (openRouterClient.isConfigured()) {
            result = tryAiInstructions(diagnosis, medications, patientAge, patientGender,
                    department, additionalNotes);
        }

        if (result == null) {
            result = generateFallbackInstructions(diagnosis, medications, department);
        }

        return result;
    }

    private FollowUpResult tryAiInstructions(String diagnosis, String medications,
                                              String patientAge, String patientGender,
                                              String department, String additionalNotes) {
        String systemPrompt = "You are a helpful Hospital Patient Care AI. "
                + "Generate clear, patient-friendly follow-up instructions after a medical consultation. "
                + "Respond ONLY with a valid JSON object (no markdown wrapping) matching this schema:\n"
                + "{\n"
                + "  \"dietInstructions\": \"Specific dietary guidelines for the patient's condition\",\n"
                + "  \"medicationReminders\": [\"Take medication X after meals\", \"Avoid alcohol while on Y\"],\n"
                + "  \"activityRestrictions\": \"Physical activity guidelines\",\n"
                + "  \"warningSigns\": [\"Sign 1 that requires immediate attention\", \"Sign 2\"],\n"
                + "  \"followUpDate\": \"Recommended follow-up timeframe\",\n"
                + "  \"selfCareTips\": [\"Tip 1 for home care\", \"Tip 2\"],\n"
                + "  \"summary\": \"Brief 2-3 sentence summary of care instructions\"\n"
                + "}";

        String userPrompt = String.format(
                "Patient Consultation Follow-Up Instructions:\n"
                + "Diagnosis: %s\n"
                + "Medications Prescribed: %s\n"
                + "Patient Age: %s\n"
                + "Patient Gender: %s\n"
                + "Department: %s\n"
                + "Additional Notes: %s\n\n"
                + "Generate clear, easy-to-understand follow-up instructions suitable for a patient with no medical background.",
                diagnosis != null ? diagnosis : "General consultation",
                medications != null ? medications : "As prescribed",
                patientAge != null ? patientAge : "Adult",
                patientGender != null ? patientGender : "Unspecified",
                department != null ? department : "General Medicine",
                additionalNotes != null && !additionalNotes.isBlank() ? additionalNotes : "None"
        );

        try {
            String raw = openRouterClient.chat(systemPrompt, userPrompt);
            if (raw == null || raw.isBlank()) return null;

            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);

            FollowUpResult res = new FollowUpResult();
            res.setDietInstructions(root.path("dietInstructions").asText(
                    "Maintain a balanced diet. Stay hydrated."));
            res.setActivityRestrictions(root.path("activityRestrictions").asText(
                    "No strenuous activity for 48 hours."));

            List<String> medReminders = new ArrayList<>();
            JsonNode medsNode = root.path("medicationReminders");
            if (medsNode.isArray()) {
                medsNode.forEach(n -> medReminders.add(n.asText()));
            }
            res.setMedicationReminders(medReminders.isEmpty()
                    ? List.of("Take medications as prescribed") : medReminders);

            List<String> warningSigns = new ArrayList<>();
            JsonNode warnNode = root.path("warningSigns");
            if (warnNode.isArray()) {
                warnNode.forEach(n -> warningSigns.add(n.asText()));
            }
            res.setWarningSigns(warningSigns.isEmpty()
                    ? List.of("High fever (>38.5°C)", "Severe pain", "Difficulty breathing") : warningSigns);

            res.setFollowUpDate(root.path("followUpDate").asText("Follow up in 7 days"));

            List<String> selfCareTips = new ArrayList<>();
            JsonNode tipsNode = root.path("selfCareTips");
            if (tipsNode.isArray()) {
                tipsNode.forEach(n -> selfCareTips.add(n.asText()));
            }
            res.setSelfCareTips(selfCareTips.isEmpty()
                    ? List.of("Rest well", "Stay hydrated") : selfCareTips);

            res.setSummary(root.path("summary").asText(
                    "Follow the instructions above and contact us if symptoms worsen."));
            res.setAiUsed(true);

            return res;
        } catch (Exception e) {
            log.warn("Failed to parse AI follow-up instructions: {}", e.getMessage());
            return null;
        }
    }

    private FollowUpResult generateFallbackInstructions(String diagnosis, String medications,
                                                         String department) {
        FollowUpResult res = new FollowUpResult();
        String diag = diagnosis != null ? diagnosis.toLowerCase() : "";
        String dept = department != null ? department : "General Medicine";

        if (diag.contains("fever") || diag.contains("cold") || diag.contains("flu")) {
            res.setDietInstructions("Drink plenty of warm fluids. Eat light, easily digestible meals. "
                    + "Avoid cold and oily foods. Include soups, broths, and fruits rich in Vitamin C.");
            res.setMedicationReminders(List.of(
                    "Take prescribed fever-reducing medication as directed",
                    "Complete the full course of antibiotics if prescribed",
                    "Take medications after meals to avoid stomach upset"
            ));
            res.setActivityRestrictions("Rest for 2-3 days. Avoid strenuous physical activity until fever subsides completely.");
            res.setWarningSigns(List.of(
                    "Fever persists beyond 3 days or exceeds 39°C",
                    "Difficulty breathing or chest pain",
                    "Severe headache with stiff neck",
                    "Symptoms worsen after initial improvement"
            ));
            res.setFollowUpDate("Follow up in 5-7 days if symptoms don't improve, or sooner if warning signs appear.");
            res.setSelfCareTips(List.of(
                    "Use a humidifier or take steam inhalation for congestion",
                    "Gargle warm salt water for sore throat",
                    "Get adequate sleep (8-10 hours)",
                    "Wash hands frequently to prevent spreading"
            ));
        } else if (diag.contains("hypertension") || diag.contains("blood pressure") || "Cardiology".equalsIgnoreCase(dept)) {
            res.setDietInstructions("Follow a low-sodium diet (less than 2g salt per day). "
                    + "Eat plenty of fruits, vegetables, and whole grains. "
                    + "Limit caffeine and avoid processed foods.");
            res.setMedicationReminders(List.of(
                    "Take blood pressure medication at the same time daily",
                    "Do not skip doses even if you feel well",
                    "Monitor blood pressure at home daily"
            ));
            res.setActivityRestrictions("Light to moderate exercise (walking 30 min/day) is encouraged. "
                    + "Avoid heavy lifting and high-intensity workouts for 1 week.");
            res.setWarningSigns(List.of(
                    "Sudden severe headache",
                    "Chest pain or tightness",
                    "Blurred vision or vision changes",
                    "Blood pressure reading above 180/120 mmHg"
            ));
            res.setFollowUpDate("Follow up in 2 weeks with blood pressure log. "
                    + "Bring home BP readings to your appointment.");
            res.setSelfCareTips(List.of(
                    "Check blood pressure at the same time each morning",
                    "Reduce stress through deep breathing exercises",
                    "Maintain a healthy weight",
                    "Limit alcohol consumption"
            ));
        } else {
            res.setDietInstructions("Maintain a balanced diet with regular meals. "
                    + "Stay well hydrated (8-10 glasses of water daily). "
                    + "Eat fresh fruits, vegetables, and lean proteins.");
            res.setMedicationReminders(medications != null && !medications.isBlank()
                    ? List.of("Take " + medications + " as prescribed", "Complete the full course")
                    : List.of("Take any prescribed medications as directed", "Do not self-medicate"));
            res.setActivityRestrictions("Moderate activity is fine. "
                    + "Avoid strenuous exercise for 48 hours. "
                    + "Listen to your body and rest when needed.");
            res.setWarningSigns(List.of(
                    "Symptoms worsen or new symptoms develop",
                    "High fever (>38.5°C) that doesn't respond to medication",
                    "Severe pain at the affected area",
                    "Any unusual or concerning changes"
            ));
            res.setFollowUpDate("Follow up in 7-14 days or sooner if symptoms persist or worsen.");
            res.setSelfCareTips(List.of(
                    "Get adequate rest and sleep",
                    "Stay hydrated throughout the day",
                    "Avoid alcohol and tobacco",
                    "Keep your follow-up appointment"
            ));
        }

        res.setSummary("Please follow the above instructions carefully. "
                + "Contact the hospital immediately if any warning signs appear. "
                + "We wish you a speedy recovery.");
        res.setAiUsed(false);

        return res;
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.trim();
    }

    // Result class

    public static class FollowUpResult {
        private String dietInstructions;
        private List<String> medicationReminders;
        private String activityRestrictions;
        private List<String> warningSigns;
        private String followUpDate;
        private List<String> selfCareTips;
        private String summary;
        private boolean aiUsed;

        public String getDietInstructions() { return dietInstructions; }
        public void setDietInstructions(String v) { this.dietInstructions = v; }
        public List<String> getMedicationReminders() { return medicationReminders; }
        public void setMedicationReminders(List<String> v) { this.medicationReminders = v; }
        public String getActivityRestrictions() { return activityRestrictions; }
        public void setActivityRestrictions(String v) { this.activityRestrictions = v; }
        public List<String> getWarningSigns() { return warningSigns; }
        public void setWarningSigns(List<String> v) { this.warningSigns = v; }
        public String getFollowUpDate() { return followUpDate; }
        public void setFollowUpDate(String v) { this.followUpDate = v; }
        public List<String> getSelfCareTips() { return selfCareTips; }
        public void setSelfCareTips(List<String> v) { this.selfCareTips = v; }
        public String getSummary() { return summary; }
        public void setSummary(String v) { this.summary = v; }
        public boolean isAiUsed() { return aiUsed; }
        public void setAiUsed(boolean v) { this.aiUsed = v; }
    }
}
