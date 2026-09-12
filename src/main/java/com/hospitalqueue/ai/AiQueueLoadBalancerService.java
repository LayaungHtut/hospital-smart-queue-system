package com.hospitalqueue.ai;

import com.hospitalqueue.model.Department;
import com.hospitalqueue.model.Doctor;
import com.hospitalqueue.model.Patient;
import com.hospitalqueue.model.Queue;
import com.hospitalqueue.repository.DepartmentRepository;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.QueueRepository;
import com.hospitalqueue.service.NotificationService;
import com.hospitalqueue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiQueueLoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(AiQueueLoadBalancerService.class);

    private final QueueRepository queueRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final QueueService queueService;
    private final NotificationService notificationService;
    private final OpenRouterClient openRouterClient;

    public AiQueueLoadBalancerService(QueueRepository queueRepository,
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            PatientRepository patientRepository,
            QueueService queueService,
            NotificationService notificationService,
            OpenRouterClient openRouterClient) {
        this.queueRepository = queueRepository;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.patientRepository = patientRepository;
        this.queueService = queueService;
        this.notificationService = notificationService;
        this.openRouterClient = openRouterClient;
    }

    public static class ReassignmentSuggestion {
        private String queueId;
        private String queueNumber;
        private String patientId;
        private String patientName;
        private String fromDoctorId;
        private String fromDoctorName;
        private int fromDoctorQueueSize;
        private String toDoctorId;
        private String toDoctorName;
        private int toDoctorQueueSize;
        private String department;
        private int estimatedMinutesSaved;
        private String reason;

        public String getQueueId() {
            return queueId;
        }

        public void setQueueId(String queueId) {
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

        public String getFromDoctorId() {
            return fromDoctorId;
        }

        public void setFromDoctorId(String fromDoctorId) {
            this.fromDoctorId = fromDoctorId;
        }

        public String getFromDoctorName() {
            return fromDoctorName;
        }

        public void setFromDoctorName(String fromDoctorName) {
            this.fromDoctorName = fromDoctorName;
        }

        public int getFromDoctorQueueSize() {
            return fromDoctorQueueSize;
        }

        public void setFromDoctorQueueSize(int fromDoctorQueueSize) {
            this.fromDoctorQueueSize = fromDoctorQueueSize;
        }

        public String getToDoctorId() {
            return toDoctorId;
        }

        public void setToDoctorId(String toDoctorId) {
            this.toDoctorId = toDoctorId;
        }

        public String getToDoctorName() {
            return toDoctorName;
        }

        public void setToDoctorName(String toDoctorName) {
            this.toDoctorName = toDoctorName;
        }

        public int getToDoctorQueueSize() {
            return toDoctorQueueSize;
        }

        public void setToDoctorQueueSize(int toDoctorQueueSize) {
            this.toDoctorQueueSize = toDoctorQueueSize;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public int getEstimatedMinutesSaved() {
            return estimatedMinutesSaved;
        }

        public void setEstimatedMinutesSaved(int estimatedMinutesSaved) {
            this.estimatedMinutesSaved = estimatedMinutesSaved;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class LoadBalancerReport {
        private String departmentName;
        private int totalWaitingPatients;
        private int activeDoctorsCount;
        private double averageWaitTimeMinutes;
        private boolean isImbalanced;
        private String aiAnalysisSummary;
        private List<ReassignmentSuggestion> suggestions = new ArrayList<>();

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public int getTotalWaitingPatients() {
            return totalWaitingPatients;
        }

        public void setTotalWaitingPatients(int totalWaitingPatients) {
            this.totalWaitingPatients = totalWaitingPatients;
        }

        public int getActiveDoctorsCount() {
            return activeDoctorsCount;
        }

        public void setActiveDoctorsCount(int activeDoctorsCount) {
            this.activeDoctorsCount = activeDoctorsCount;
        }

        public double getAverageWaitTimeMinutes() {
            return averageWaitTimeMinutes;
        }

        public void setAverageWaitTimeMinutes(double averageWaitTimeMinutes) {
            this.averageWaitTimeMinutes = averageWaitTimeMinutes;
        }

        public boolean isImbalanced() {
            return isImbalanced;
        }

        public void setImbalanced(boolean imbalanced) {
            isImbalanced = imbalanced;
        }

        public String getAiAnalysisSummary() {
            return aiAnalysisSummary;
        }

        public void setAiAnalysisSummary(String aiAnalysisSummary) {
            this.aiAnalysisSummary = aiAnalysisSummary;
        }

        public List<ReassignmentSuggestion> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<ReassignmentSuggestion> suggestions) {
            this.suggestions = suggestions;
        }
    }

    public LoadBalancerReport analyzeAndSuggest(String departmentName) {
        LoadBalancerReport report = new LoadBalancerReport();
        report.setDepartmentName(
                departmentName != null && !departmentName.isBlank() ? departmentName : "All Departments");

        List<Department> depts = departmentRepository.findAll();
        Map<Integer, Department> deptMap = depts.stream()
                .collect(Collectors.toMap(Department::getDepartmentId, d -> d, (a, b) -> a));

        List<Doctor> doctors = doctorRepository.findAll();
        if (departmentName != null && !departmentName.isBlank()
                && !"All Departments".equalsIgnoreCase(departmentName)) {
            Department target = departmentRepository.findByName(departmentName);
            if (target != null) {
                doctors = doctors.stream()
                        .filter(d -> d.getDepartmentId() == target.getDepartmentId())
                        .collect(Collectors.toList());
            }
        }

        List<Doctor> availableDoctors = doctors.stream().filter(Doctor::isAvailable).collect(Collectors.toList());
        report.setActiveDoctorsCount(availableDoctors.size());

        if (availableDoctors.isEmpty()) {
            report.setAiAnalysisSummary("No active doctors currently online in this department to balance.");
            report.setImbalanced(false);
            return report;
        }

        // Map doctors to their waiting queues
        Map<String, List<Queue>> doctorQueues = new HashMap<>();
        int totalWaiting = 0;
        for (Doctor doc : availableDoctors) {
            List<Queue> waiting = queueRepository.findWaitingQueuesByDoctor(doc.getDoctorId());
            doctorQueues.put(doc.getDoctorId(), waiting);
            totalWaiting += waiting.size();
        }
        report.setTotalWaitingPatients(totalWaiting);

        if (totalWaiting == 0) {
            report.setAiAnalysisSummary("All clinic queues are currently clear. No load balancing required.");
            report.setImbalanced(false);
            return report;
        }

        // Group available doctors by department
        Map<Integer, List<Doctor>> docsByDept = availableDoctors.stream()
                .collect(Collectors.groupingBy(Doctor::getDepartmentId));

        List<ReassignmentSuggestion> suggestions = new ArrayList<>();
        Set<String> patientIdsToFetch = new HashSet<>();

        for (Map.Entry<Integer, List<Doctor>> entry : docsByDept.entrySet()) {
            List<Doctor> deptDocs = entry.getValue();
            if (deptDocs.size() < 2)
                continue; // Need at least 2 doctors in the department to balance

            // Sort doctors by waiting queue size: most busy first, least busy last
            deptDocs.sort((d1, d2) -> Integer.compare(
                    doctorQueues.getOrDefault(d2.getDoctorId(), Collections.emptyList()).size(),
                    doctorQueues.getOrDefault(d1.getDoctorId(), Collections.emptyList()).size()));

            Doctor busyDoc = deptDocs.get(0);
            Doctor freeDoc = deptDocs.get(deptDocs.size() - 1);

            List<Queue> busyList = doctorQueues.getOrDefault(busyDoc.getDoctorId(), Collections.emptyList());
            List<Queue> freeList = doctorQueues.getOrDefault(freeDoc.getDoctorId(), Collections.emptyList());

            int diff = busyList.size() - freeList.size();
            if (diff >= 2) {
                // We should move (diff / 2) non-emergency queues from busyDoc to freeDoc
                int toMove = diff / 2;
                int moved = 0;

                // Pick from end of waiting queue (non-emergency)
                for (int i = busyList.size() - 1; i >= 0 && moved < toMove; i--) {
                    Queue q = busyList.get(i);
                    if (q.isEmergency() || "EMERGENCY".equalsIgnoreCase(q.getPriority())) {
                        continue; // Don't move critical emergencies automatically
                    }

                    ReassignmentSuggestion s = new ReassignmentSuggestion();
                    s.setQueueId(String.valueOf(q.getQueueId()));
                    s.setQueueNumber(q.getQueueNumber());
                    s.setPatientId(q.getPatientId());
                    patientIdsToFetch.add(q.getPatientId());

                    s.setFromDoctorId(busyDoc.getDoctorId());
                    s.setFromDoctorName(busyDoc.getName());
                    s.setFromDoctorQueueSize(busyList.size());

                    s.setToDoctorId(freeDoc.getDoctorId());
                    s.setToDoctorName(freeDoc.getName());
                    s.setToDoctorQueueSize(freeList.size());

                    Department d = deptMap.get(busyDoc.getDepartmentId());
                    s.setDepartment(d != null ? d.getDepartmentName() : "General Medicine");

                    long avgTime = busyDoc.getAverageConsultationMinutes() > 0 ? busyDoc.getAverageConsultationMinutes()
                            : 15;
                    s.setEstimatedMinutesSaved((int) (diff * avgTime / 2));
                    s.setReason(String.format(
                            "Reassigning to %s balances queue length (%d vs %d) and saves approx %d mins.",
                            freeDoc.getName(), busyList.size(), freeList.size(), s.getEstimatedMinutesSaved()));

                    suggestions.add(s);
                    moved++;
                }
            }
        }

        // Fill patient names
        if (!patientIdsToFetch.isEmpty()) {
            Map<String, Patient> ptMap = patientRepository.findByIds(patientIdsToFetch).stream()
                    .collect(Collectors.toMap(Patient::getPatientId, p -> p, (a, b) -> a));
            for (ReassignmentSuggestion s : suggestions) {
                Patient p = ptMap.get(s.getPatientId());
                s.setPatientName(p != null ? p.getName() : "Patient " + s.getPatientId());
            }
        }

        report.setSuggestions(suggestions);
        report.setImbalanced(!suggestions.isEmpty());

        if (!suggestions.isEmpty()) {
            int totalSaved = suggestions.stream().mapToInt(ReassignmentSuggestion::getEstimatedMinutesSaved).sum();
            report.setAiAnalysisSummary(String.format(
                    "Identified %d optimal queue reassignments across overloaded doctors. Executing this plan will reduce cumulative patient wait times by ~%d minutes.",
                    suggestions.size(), totalSaved));
        } else {
            report.setAiAnalysisSummary(
                    "Queues are currently well-balanced across available doctors in the department.");
        }

        return report;
    }

    @Transactional
    public Map<String, Object> applyLoadBalancerPlan(List<String> queueIds, String staffId) {
        if (queueIds == null || queueIds.isEmpty()) {
            return Map.of("success", false, "message", "No queues provided to balance");
        }

        int successCount = 0;
        LoadBalancerReport report = analyzeAndSuggest(null);
        Map<String, ReassignmentSuggestion> sugMap = report.getSuggestions().stream()
                .collect(Collectors.toMap(ReassignmentSuggestion::getQueueId, s -> s, (a, b) -> a));

        for (String qid : queueIds) {
            ReassignmentSuggestion sug = sugMap.get(qid);
            if (sug != null) {
                try {
                    long numericQueueId = Long.parseLong(qid);
                    queueService.reassignDoctor(numericQueueId, sug.getToDoctorId());
                    Queue q = queueRepository.findById(numericQueueId);
                    if (q != null) {
                        notificationService.notify(q.getPatientId(),
                                "Your queue " + q.getQueueNumber() + " has been auto-balanced to Dr. "
                                        + sug.getToDoctorName() + " to minimize your waiting time.");
                    }
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to reassign queue {}: {}", qid, e.getMessage());
                }
            }
        }

        return Map.of(
                "success", true,
                "reassignedCount", successCount,
                "message", String.format("Successfully auto-balanced %d patient queue(s). Patients have been notified.",
                        successCount));
    }
}
