package com.hospitalqueue.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiSoapService {

    private static final Logger log = LoggerFactory.getLogger(AiSoapService.class);

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiSoapService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public static class SoapNoteResult {
        private String subjective;
        private String objective;
        private String assessment;
        private List<String> icd10Codes;
        private List<PrescriptionSuggestion> medications;
        private List<String> recommendedTests;
        private String lifestyleAdvice;
        private String followUp;
        private String summary;
        private boolean aiUsed;

        public String getSubjective() {
            return subjective;
        }

        public void setSubjective(String subjective) {
            this.subjective = subjective;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getAssessment() {
            return assessment;
        }

        public void setAssessment(String assessment) {
            this.assessment = assessment;
        }

        public List<String> getIcd10Codes() {
            return icd10Codes;
        }

        public void setIcd10Codes(List<String> icd10Codes) {
            this.icd10Codes = icd10Codes;
        }

        public List<PrescriptionSuggestion> getMedications() {
            return medications;
        }

        public void setMedications(List<PrescriptionSuggestion> medications) {
            this.medications = medications;
        }

        public List<String> getRecommendedTests() {
            return recommendedTests;
        }

        public void setRecommendedTests(List<String> recommendedTests) {
            this.recommendedTests = recommendedTests;
        }

        public String getLifestyleAdvice() {
            return lifestyleAdvice;
        }

        public void setLifestyleAdvice(String lifestyleAdvice) {
            this.lifestyleAdvice = lifestyleAdvice;
        }

        public String getFollowUp() {
            return followUp;
        }

        public void setFollowUp(String followUp) {
            this.followUp = followUp;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public boolean isAiUsed() {
            return aiUsed;
        }

        public void setAiUsed(boolean aiUsed) {
            this.aiUsed = aiUsed;
        }
    }

    public static class PrescriptionSuggestion {
        private String name;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;

        public PrescriptionSuggestion() {
        }

        public PrescriptionSuggestion(String name, String dosage, String frequency, String duration,
                String instructions) {
            this.name = name;
            this.dosage = dosage;
            this.frequency = frequency;
            this.duration = duration;
            this.instructions = instructions;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDosage() {
            return dosage;
        }

        public void setDosage(String dosage) {
            this.dosage = dosage;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
    }

    public SoapNoteResult generateSoapNote(String patientName, String patientAge, String gender,
            String symptoms, String vitals, String observations,
            String department, String specialization) {
        SoapNoteResult result = null;

        if (openRouterClient.isConfigured()) {
            result = tryAiSoapNote(patientName, patientAge, gender, symptoms, vitals, observations, department,
                    specialization);
        }

        if (result == null) {
            result = generateFallbackSoapNote(patientName, patientAge, gender, symptoms, vitals, observations,
                    department);
        }

        return result;
    }

    private SoapNoteResult tryAiSoapNote(String patientName, String patientAge, String gender,
            String symptoms, String vitals, String observations,
            String department, String specialization) {
        String systemPrompt = "You are an expert Hospital Clinical Documentation AI Assistant. "
                + "Generate a professional, high-standard SOAP Note (Subjective, Objective, Assessment, Plan) "
                + "for the attending doctor based on patient consultation inputs.\n"
                + "Respond ONLY with a valid JSON object matching this schema without any markdown wrapping or commentary:\n"
                + "{\n"
                + "  \"subjective\": \"Detailed patient narrative of chief complaint, history of presenting illness\",\n"
                + "  \"objective\": \"Physical examination observations, vital signs summary\",\n"
                + "  \"assessment\": \"Primary clinical diagnosis and differential considerations\",\n"
                + "  \"icd10Codes\": [\"I10 - Essential Hypertension\", \"R05.9 - Cough, unspecified\"],\n"
                + "  \"medications\": [\n"
                + "    {\"name\": \"Paracetamol\", \"dosage\": \"500mg\", \"frequency\": \"TDS (3 times daily)\", \"duration\": \"5 days\", \"instructions\": \"After meals for pain/fever\"}\n"
                + "  ],\n"
                + "  \"recommendedTests\": [\"Complete Blood Count (CBC)\", \"Chest X-Ray\"],\n"
                + "  \"lifestyleAdvice\": \"Adequate hydration, bed rest for 48 hours, avoid cold drinks\",\n"
                + "  \"followUp\": \"Review in 5 to 7 days if symptoms persist\",\n"
                + "  \"summary\": \"Concise 2-sentence clinical visit overview\"\n"
                + "}";

        String userPrompt = String.format(
                "Patient: %s (Age: %s, Gender: %s)\n"
                        + "Department: %s (Specialization: %s)\n"
                        + "Chief Complaints / Symptoms: %s\n"
                        + "Vital Signs: %s\n"
                        + "Doctor Observations / Notes: %s",
                patientName != null ? patientName : "Patient",
                patientAge != null ? patientAge : "Adult",
                gender != null ? gender : "Unspecified",
                department != null ? department : "General Medicine",
                specialization != null ? specialization : "Consultant",
                symptoms != null && !symptoms.isBlank() ? symptoms : "General unwellness",
                vitals != null && !vitals.isBlank() ? vitals : "BP 120/80 mmHg, HR 76 bpm, Temp 36.8 C",
                observations != null && !observations.isBlank() ? observations : "Alert, oriented, stable");

        try {
            String raw = openRouterClient.chat(systemPrompt, userPrompt);
            if (raw == null || raw.isBlank())
                return null;

            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);

            SoapNoteResult res = new SoapNoteResult();
            res.setSubjective(root.path("subjective").asText("Patient presents with complaints as recorded."));
            res.setObjective(root.path("objective").asText("Physical examination performed. Vitals recorded."));
            res.setAssessment(root.path("assessment").asText("Clinical impression based on clinical evaluation."));

            List<String> icdCodes = new ArrayList<>();
            JsonNode icdNode = root.path("icd10Codes");
            if (icdNode.isArray()) {
                icdNode.forEach(n -> icdCodes.add(n.asText()));
            }
            res.setIcd10Codes(icdCodes.isEmpty() ? List.of("Z00.00 - General adult medical examination") : icdCodes);

            List<PrescriptionSuggestion> meds = new ArrayList<>();
            JsonNode medsNode = root.path("medications");
            if (medsNode.isArray()) {
                for (JsonNode m : medsNode) {
                    meds.add(new PrescriptionSuggestion(
                            m.path("name").asText("Medication"),
                            m.path("dosage").asText("Standard dose"),
                            m.path("frequency").asText("As directed"),
                            m.path("duration").asText("5 days"),
                            m.path("instructions").asText("Take after food")));
                }
            }
            res.setMedications(meds);

            List<String> tests = new ArrayList<>();
            JsonNode testsNode = root.path("recommendedTests");
            if (testsNode.isArray()) {
                testsNode.forEach(t -> tests.add(t.asText()));
            }
            res.setRecommendedTests(tests);

            res.setLifestyleAdvice(root.path("lifestyleAdvice").asText("Maintain healthy hydration and rest."));
            res.setFollowUp(root.path("followUp").asText("Follow-up in 7 days or sooner if warning signs develop."));
            res.setSummary(root.path("summary").asText("Consultation completed with treatment plan instituted."));
            res.setAiUsed(true);

            return res;
        } catch (Exception e) {
            log.warn("Failed to parse AI SOAP Note response: {}", e.getMessage());
            return null;
        }
    }

    private SoapNoteResult generateFallbackSoapNote(String patientName, String patientAge, String gender,
            String symptoms, String vitals, String observations,
            String department) {
        SoapNoteResult res = new SoapNoteResult();
        String sym = symptoms != null ? symptoms.toLowerCase() : "general symptoms";
        String dept = department != null ? department : "General Medicine";

        res.setSubjective(String.format(
                "Patient %s presented with chief complaints of: %s. History of present illness reviewed.",
                patientName != null ? patientName : "Patient", symptoms != null ? symptoms : "routine evaluation"));

        res.setObjective(String.format(
                "Vital signs: %s. Physical exam: %s. General condition satisfactory, responsive and oriented.",
                vitals != null && !vitals.isBlank() ? vitals : "BP 120/80 mmHg, HR 72 bpm, SpO2 98%, Temp 37.0°C",
                observations != null && !observations.isBlank() ? observations
                        : "No acute distress noted on inspection"));

        if (sym.contains("chest") || sym.contains("heart") || "Cardiology".equalsIgnoreCase(dept)) {
            res.setAssessment("Cardiovascular assessment: Anginal symptoms / Essential hypertension to evaluate.");
            res.setIcd10Codes(
                    List.of("I20.9 - Angina pectoris, unspecified", "I10 - Essential (primary) hypertension"));
            res.setMedications(List.of(
                    new PrescriptionSuggestion("Aspirin (Ecosprin)", "75mg", "OD (Once daily)", "30 days",
                            "Take after lunch"),
                    new PrescriptionSuggestion("Atorvastatin", "20mg", "ON (At night)", "30 days",
                            "Take before sleep")));
            res.setRecommendedTests(List.of("12-Lead ECG", "Troponin I", "Echocardiogram", "Lipid Profile"));
            res.setLifestyleAdvice("Low salt, low cholesterol diet. Avoid strenuous exertion until full clearance.");
            res.setFollowUp("Follow-up in 3 to 5 days with test reports.");
        } else if (sym.contains("fever") || sym.contains("cough") || sym.contains("cold") || sym.contains("throat")) {
            res.setAssessment("Acute upper respiratory tract infection with mild pyrexia.");
            res.setIcd10Codes(
                    List.of("J06.9 - Acute upper respiratory infection, unspecified", "R50.9 - Fever, unspecified"));
            res.setMedications(List.of(
                    new PrescriptionSuggestion("Paracetamol", "500mg", "TDS (3 times daily)", "5 days",
                            "For fever and pain"),
                    new PrescriptionSuggestion("Cetirizine", "10mg", "ON (At night)", "5 days", "For nasal congestion"),
                    new PrescriptionSuggestion("Amoxicillin/Clavulanate", "625mg", "BD (Twice daily)", "5 days",
                            "If bacterial symptoms persist")));
            res.setRecommendedTests(List.of("Complete Blood Count (CBC)", "Chest X-Ray PA view (if cough > 7 days)"));
            res.setLifestyleAdvice("Steam inhalation twice daily, drink warm water, plenty of oral fluid intake.");
            res.setFollowUp("Return if high fever > 38.5°C persists beyond 3 days or breathing difficulty occurs.");
        } else {
            res.setAssessment("Clinical consultation in " + dept + ": Symptomatic assessment.");
            res.setIcd10Codes(List.of("Z00.00 - General adult medical examination"));
            res.setMedications(List.of(
                    new PrescriptionSuggestion("Multivitamin / B-Complex", "1 tablet", "OD (Morning)", "14 days",
                            "After breakfast"),
                    new PrescriptionSuggestion("Paracetamol", "500mg", "PRN (As needed)", "5 days", "For discomfort")));
            res.setRecommendedTests(List.of("Routine Blood Chemistry", "Urinalysis"));
            res.setLifestyleAdvice("Balanced nutrition, adequate hydration, regular sleep schedule.");
            res.setFollowUp("Review in 7 to 14 days or PRN.");
        }

        res.setSummary("Patient evaluated for " + (symptoms != null ? symptoms : "routine care")
                + ". Treatment plan and follow-up scheduled.");
        res.setAiUsed(false);
        return res;
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
