package com.hospitalqueue.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalqueue.model.Department;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.rule.DepartmentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiInteractiveTriageService {

    private static final Logger log = LoggerFactory.getLogger(AiInteractiveTriageService.class);

    private final OpenRouterClient openRouterClient;
    private final DepartmentRule departmentRule;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiInteractiveTriageService(OpenRouterClient openRouterClient,
            DepartmentRule departmentRule,
            DepartmentRepository departmentRepository) {
        this.openRouterClient = openRouterClient;
        this.departmentRule = departmentRule;
        this.departmentRepository = departmentRepository;
    }

    public static class TriageQuestion {
        private String id;
        private String question;
        private String category;
        private List<String> options;

        public TriageQuestion() {
        }

        public TriageQuestion(String id, String question, String category, List<String> options) {
            this.id = id;
            this.question = question;
            this.category = category;
            this.options = options;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }
    }

    public static class InteractiveTriageResponse {
        private List<TriageQuestion> questions;
        private String preliminaryAssessment;
        private boolean isEmergencyPotential;
        private boolean aiUsed;

        public List<TriageQuestion> getQuestions() {
            return questions;
        }

        public void setQuestions(List<TriageQuestion> questions) {
            this.questions = questions;
        }

        public String getPreliminaryAssessment() {
            return preliminaryAssessment;
        }

        public void setPreliminaryAssessment(String preliminaryAssessment) {
            this.preliminaryAssessment = preliminaryAssessment;
        }

        public boolean isEmergencyPotential() {
            return isEmergencyPotential;
        }

        public void setEmergencyPotential(boolean emergencyPotential) {
            isEmergencyPotential = emergencyPotential;
        }

        public boolean isAiUsed() {
            return aiUsed;
        }

        public void setAiUsed(boolean aiUsed) {
            this.aiUsed = aiUsed;
        }
    }

    public static class FinalizedTriageResult {
        private Department department;
        private boolean emergency;
        private int acuityScore; // 1 to 5
        private String disposition; // emergency, urgent, routine
        private String clinicalReason;
        private String patientSummary;
        private List<String> recommendedLabs;
        private List<String> recommendedTests;
        private boolean aiUsed;

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public boolean isEmergency() {
            return emergency;
        }

        public void setEmergency(boolean emergency) {
            this.emergency = emergency;
        }

        public int getAcuityScore() {
            return acuityScore;
        }

        public void setAcuityScore(int acuityScore) {
            this.acuityScore = acuityScore;
        }

        public String getDisposition() {
            return disposition;
        }

        public void setDisposition(String disposition) {
            this.disposition = disposition;
        }

        public String getClinicalReason() {
            return clinicalReason;
        }

        public void setClinicalReason(String clinicalReason) {
            this.clinicalReason = clinicalReason;
        }

        public String getPatientSummary() {
            return patientSummary;
        }

        public void setPatientSummary(String patientSummary) {
            this.patientSummary = patientSummary;
        }

        public List<String> getRecommendedLabs() {
            return recommendedLabs;
        }

        public void setRecommendedLabs(List<String> recommendedLabs) {
            this.recommendedLabs = recommendedLabs;
        }

        public List<String> getRecommendedTests() {
            return recommendedTests;
        }

        public void setRecommendedTests(List<String> recommendedTests) {
            this.recommendedTests = recommendedTests;
        }

        public boolean isAiUsed() {
            return aiUsed;
        }

        public void setAiUsed(boolean aiUsed) {
            this.aiUsed = aiUsed;
        }
    }

    public InteractiveTriageResponse generateQuestions(String symptoms) {
        InteractiveTriageResponse res = null;
        if (openRouterClient.isConfigured()) {
            res = tryAiGenerateQuestions(symptoms);
        }
        if (res == null) {
            res = fallbackQuestions(symptoms);
        }
        return res;
    }

    private InteractiveTriageResponse tryAiGenerateQuestions(String symptoms) {
        String systemPrompt = "You are an emergency hospital triage nurse AI. "
                + "A patient gives an initial symptom description. Generate exactly 3 crucial clarifying triage questions "
                + "with multiple-choice quick answers to accurately evaluate severity, duration, and red flags.\n"
                + "Respond ONLY with a valid JSON object matching this schema:\n"
                + "{\n"
                + "  \"preliminaryAssessment\": \"Brief 1-sentence note about initial symptoms\",\n"
                + "  \"isEmergencyPotential\": true/false,\n"
                + "  \"questions\": [\n"
                + "    {\"id\": \"q1\", \"question\": \"How long have you had these symptoms?\", \"category\": \"DURATION\", \"options\": [\"Less than 24 hours\", \"2 to 3 days\", \"Over 1 week\"]},\n"
                + "    {\"id\": \"q2\", \"question\": \"How would you rate the pain or severity from 1 to 10?\", \"category\": \"SEVERITY\", \"options\": [\"Mild (1-3)\", \"Moderate (4-6)\", \"Severe (7-10)\"]},\n"
                + "    {\"id\": \"q3\", \"question\": \"Are you experiencing shortness of breath or dizziness?\", \"category\": \"RED_FLAG\", \"options\": [\"None of these\", \"Mild dizziness\", \"Severe breathing difficulty\"]}\n"
                + "  ]\n"
                + "}";

        try {
            String raw = openRouterClient.chat(systemPrompt, "Initial Symptoms: " + symptoms);
            if (raw == null || raw.isBlank())
                return null;

            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);

            InteractiveTriageResponse res = new InteractiveTriageResponse();
            res.setPreliminaryAssessment(
                    root.path("preliminaryAssessment").asText("Initial symptom analysis initiated."));
            res.setEmergencyPotential(root.path("isEmergencyPotential").asBoolean(false));

            List<TriageQuestion> list = new ArrayList<>();
            JsonNode qNode = root.path("questions");
            if (qNode.isArray()) {
                for (JsonNode q : qNode) {
                    List<String> opts = new ArrayList<>();
                    q.path("options").forEach(o -> opts.add(o.asText()));
                    list.add(new TriageQuestion(
                            q.path("id").asText("q" + (list.size() + 1)),
                            q.path("question").asText(),
                            q.path("category").asText("GENERAL"),
                            opts.isEmpty() ? List.of("Mild", "Moderate", "Severe") : opts));
                }
            }
            res.setQuestions(list.isEmpty() ? fallbackQuestions(symptoms).getQuestions() : list);
            res.setAiUsed(true);
            return res;
        } catch (Exception e) {
            log.warn("Failed to parse interactive triage questions: {}", e.getMessage());
            return null;
        }
    }

    private InteractiveTriageResponse fallbackQuestions(String symptoms) {
        InteractiveTriageResponse res = new InteractiveTriageResponse();
        boolean emerg = departmentRule.containsEmergencySymptom(symptoms);
        res.setEmergencyPotential(emerg);
        res.setPreliminaryAssessment("Standard clinical triage questions for: " + symptoms);

        List<TriageQuestion> list = new ArrayList<>();
        list.add(new TriageQuestion("q1", "How long have you been feeling these symptoms?", "DURATION",
                List.of("Less than 6 hours", "1 to 3 days", "4 to 7 days", "More than a week")));
        list.add(new TriageQuestion("q2", "How would you rate the pain / discomfort level?", "SEVERITY",
                List.of("Mild (1-3)", "Moderate (4-6)", "Severe (7-8)", "Unbearable / Critical (9-10)")));
        list.add(new TriageQuestion("q3", "Do you have any associated critical warning signs?", "RED_FLAG",
                List.of("No warning signs", "High fever (> 38.5°C)", "Shortness of breath / Chest tightness",
                        "Dizziness / Fainting")));

        res.setQuestions(list);
        res.setAiUsed(false);
        return res;
    }

    public FinalizedTriageResult finalizeTriage(String symptoms, Map<String, String> answers) {
        FinalizedTriageResult res = null;
        if (openRouterClient.isConfigured()) {
            res = tryAiFinalizeTriage(symptoms, answers);
        }
        if (res == null) {
            res = fallbackFinalizeTriage(symptoms, answers);
        }
        return res;
    }

    private FinalizedTriageResult tryAiFinalizeTriage(String symptoms, Map<String, String> answers) {
        String systemPrompt = "You are a senior hospital triage officer. Given initial symptoms and patient responses to clarifying questions, "
                + "determine the appropriate Department (Cardiology, Neurology, Orthopedics, General Medicine, Pediatrics, Dermatology), "
                + "Emergency classification (true/false), Acuity score (1=Routine, 2=Low, 3=Moderate, 4=Urgent, 5=Resuscitation/Emergency), "
                + "Disposition (routine, urgent, emergency), Clinical Reason, Pre-Consultation Summary, and Recommended Tests/Labs.\n"
                + "Respond ONLY with a valid JSON object matching this schema:\n"
                + "{\n"
                + "  \"department\": \"Cardiology\",\n"
                + "  \"emergency\": false,\n"
                + "  \"acuityScore\": 3,\n"
                + "  \"disposition\": \"urgent\",\n"
                + "  \"clinicalReason\": \"Symptoms indicate cardiac evaluation needed due to duration and severity.\",\n"
                + "  \"patientSummary\": \"Patient reports chest tightness for 2 days, severity 6/10 without fainting.\",\n"
                + "  \"recommendedTests\": [\"12-Lead ECG\", \"Chest X-Ray\"],\n"
                + "  \"recommendedLabs\": [\"Troponin I\", \"CBC\"]\n"
                + "}";

        StringBuilder sb = new StringBuilder();
        sb.append("Initial Symptoms: ").append(symptoms).append("\nAnswers:\n");
        if (answers != null) {
            answers.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        try {
            String raw = openRouterClient.chat(systemPrompt, sb.toString());
            if (raw == null || raw.isBlank())
                return null;

            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);

            FinalizedTriageResult res = new FinalizedTriageResult();
            String deptName = root.path("department").asText("General Medicine");
            Department dept = departmentRepository.findByName(deptName);
            if (dept == null) {
                int deptId = departmentRule.mapSymptomToDepartment(symptoms);
                dept = deptId > 0 ? departmentRepository.findById(deptId) : departmentRepository.findAll().get(0);
            }
            res.setDepartment(dept);
            res.setEmergency(root.path("emergency").asBoolean(false));
            res.setAcuityScore(root.path("acuityScore").asInt(3));
            res.setDisposition(root.path("disposition").asText(res.isEmergency() ? "emergency" : "routine"));
            res.setClinicalReason(root.path("clinicalReason").asText("AI-assisted multi-parameter triage analysis."));
            res.setPatientSummary(root.path("patientSummary").asText("Clinical intake summary generated."));

            List<String> tests = new ArrayList<>();
            root.path("recommendedTests").forEach(t -> tests.add(t.asText()));
            res.setRecommendedTests(tests.isEmpty() ? List.of("Basic Vital Signs", "Physical Exam") : tests);

            List<String> labs = new ArrayList<>();
            root.path("recommendedLabs").forEach(l -> labs.add(l.asText()));
            res.setRecommendedLabs(labs.isEmpty() ? List.of("Routine Panel") : labs);

            res.setAiUsed(true);
            return res;
        } catch (Exception e) {
            log.warn("Failed to finalize AI interactive triage: {}", e.getMessage());
            return null;
        }
    }

    private FinalizedTriageResult fallbackFinalizeTriage(String symptoms, Map<String, String> answers) {
        FinalizedTriageResult res = new FinalizedTriageResult();
        int deptId = departmentRule.mapSymptomToDepartment(symptoms);
        Department dept = deptId > 0 ? departmentRepository.findById(deptId) : null;
        if (dept == null) {
            List<Department> all = departmentRepository.findAll();
            dept = all.isEmpty() ? null : all.get(0);
        }
        res.setDepartment(dept);

        boolean isEmergency = departmentRule.containsEmergencySymptom(symptoms);
        if (answers != null) {
            for (String ans : answers.values()) {
                String a = ans.toLowerCase();
                if (a.contains("unbearable") || a.contains("critical") || a.contains("shortness of breath")
                        || a.contains("chest tightness") || a.contains("fainting")) {
                    isEmergency = true;
                }
            }
        }

        res.setEmergency(isEmergency);
        res.setAcuityScore(isEmergency ? 5 : 3);
        res.setDisposition(isEmergency ? "emergency" : "routine");
        res.setClinicalReason("Refined via interactive rule assessment based on duration and severity.");

        StringBuilder sum = new StringBuilder("Symptoms: " + symptoms);
        if (answers != null && !answers.isEmpty()) {
            sum.append(" | Responses: ").append(String.join("; ", answers.values()));
        }
        res.setPatientSummary(sum.toString());
        res.setRecommendedTests(List.of("Physical Examination", "Vitals Assessment"));
        res.setRecommendedLabs(List.of("Complete Blood Count (CBC)"));
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
