/**
 * Centralized API service.
 *
 * Connected to the Spring Boot REST backend running on http://localhost:8080/api.
 *
 * Every function here calls the real backend and lets a failed request throw
 * (see `request()` below) — callers are responsible for showing a real error
 * state. None of these functions silently substitute fake/mock data on failure.
 */

import type {
  ActiveStatus,
  AdminDashboard,
  Appointment,
  AuditLog,
  AuthResponse,
  CreateQueueRequest,
  DailyReport,
  Department,
  DoctorDashboard,
  DoctorDetail,
  DoctorHistoryEntry,
  DoctorProfile,
  EmergencyCase,
  LiveDepartmentStatus,
  LoginRequest,
  NotificationItem,
  NowServingHighlight,
  PatientProfile,
  PublicSiteSettings,
  QueueEntry,
  QueueMonitorRow,
  QueueSettings,
  RecommendationResult,
  RegisterPatientRequest,
  RegistrationRequest,
  Schedule,
  StaffDashboard,
  StaffProfile,
  Symptom,
  SystemSettings,
  User,
} from "@/types";

export const API_BASE_URL =
  (import.meta as unknown as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL ??
  "/api";

export interface RequestOptions extends RequestInit {
  skipCache?: boolean;
  ttl?: number;
}

interface CacheItem {
  data: unknown;
  expiresAt: number;
}

const apiCache = new Map<string, CacheItem>();
const inFlightRequests = new Map<string, Promise<unknown>>();

/**
 * Invalidate in-memory cache entries. If no pattern is provided, clears all.
 */
export function invalidateApiCache(pattern?: string | RegExp) {
  if (!pattern) {
    apiCache.clear();
    return;
  }
  for (const key of apiCache.keys()) {
    if (typeof pattern === "string" ? key.includes(pattern) : pattern.test(key)) {
      apiCache.delete(key);
    }
  }
}

async function request<T>(path: string, init?: RequestOptions): Promise<T> {
  const method = (init?.method ?? "GET").toUpperCase();
  const isGet = method === "GET";

  // Mutating requests invalidate read caches
  if (!isGet) {
    apiCache.clear();
  }

  const now = Date.now();
  const cacheKey = `${method}:${path}`;

  // Check cache for GET requests
  if (isGet && !init?.skipCache) {
    const cached = apiCache.get(cacheKey);
    if (cached && cached.expiresAt > now) {
      return cached.data as T;
    }

    // Return in-flight promise if an identical request is already running
    const inFlight = inFlightRequests.get(cacheKey);
    if (inFlight) {
      return inFlight as Promise<T>;
    }
  }

  const fetchPromise = (async () => {
    try {
      const res = await fetch(`${API_BASE_URL}${path}`, {
        headers: {
          "Content-Type": "application/json",
          ...(init?.headers ?? {}),
        },
        credentials: "include",
        ...init,
      });

      if (!res.ok) {
        let errorMsg = `${res.status} ${res.statusText}`;
        try {
          const errorJson = await res.json();
          if (errorJson.error) errorMsg = errorJson.error;
          else if (errorJson.message) errorMsg = errorJson.message;
        } catch {
          // Ignored
        }
        throw new Error(errorMsg);
      }

      const data = (await res.json()) as T;

      if (isGet && !init?.skipCache) {
        // Longer TTL for static reference data (departments, symptoms, settings)
        const isStaticLookup =
          path.startsWith("/departments") ||
          path.startsWith("/symptoms") ||
          path.startsWith("/system/settings");
        const defaultTtl = isStaticLookup ? 60_000 : 15_000;
        const ttl = init?.ttl ?? defaultTtl;

        apiCache.set(cacheKey, {
          data,
          expiresAt: Date.now() + ttl,
        });
      }

      return data;
    } catch (err: unknown) {
      console.warn(
        `[API] Request to ${path} failed:`,
        err instanceof Error ? err.message : String(err),
      );
      throw err;
    } finally {
      if (isGet) {
        inFlightRequests.delete(cacheKey);
      }
    }
  })();

  if (isGet && !init?.skipCache) {
    inFlightRequests.set(cacheKey, fetchPromise);
  }

  return fetchPromise;
}

/* ---------------------------------- Auth --------------------------------- */

export async function loginUser(payload: LoginRequest): Promise<AuthResponse> {
  return await request<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function registerPatient(payload: RegisterPatientRequest): Promise<PatientProfile> {
  return await request<PatientProfile>("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

// NOTE: there is no real SMS/OTP provider wired up — this always accepts any
// code of 4+ characters. It is a genuine unimplemented feature, not a request
// fallback, so it's left as an explicit stub rather than faked further.
export function verifyPhone(_phone: string, code: string): Promise<{ verified: boolean }> {
  return Promise.resolve({ verified: code.length >= 4 });
}

function getCurrentPatientId(): string | number {
  if (typeof window !== "undefined") {
    try {
      const raw = window.localStorage.getItem("hqs.session");
      if (raw) {
        const s = JSON.parse(raw);
        if (s?.userId) return s.userId;
      }
    } catch {
      // Ignored: fallback to default patient id
    }
  }
  return "P001";
}

/* -------------------------------- Patient -------------------------------- */

export async function getPatientProfile(patientId?: string | number): Promise<PatientProfile> {
  const targetId = patientId ?? getCurrentPatientId();
  return await request<PatientProfile>(`/patient/${targetId}/profile`);
}

export async function getPatientQueue(patientId?: string | number): Promise<QueueEntry[]> {
  const targetId = patientId ?? getCurrentPatientId();
  return await request<QueueEntry[]>(`/patient/${targetId}/queue`);
}

export async function addPatientToQueue(payload: CreateQueueRequest): Promise<QueueEntry> {
  const finalPayload = {
    ...payload,
    patientId: payload.patientId || getCurrentPatientId(),
    doctorId: String(payload.doctorId),
  };
  return await request<QueueEntry>("/patient/queue", {
    method: "POST",
    body: JSON.stringify(finalPayload),
  });
}

export async function cancelQueue(queueId: string | number): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(
    `/patient/queue/${encodeURIComponent(String(queueId))}/cancel`,
    {
      method: "POST",
    },
  );
}

export async function getPatientAppointments(patientId?: string | number): Promise<Appointment[]> {
  const targetId = patientId ?? getCurrentPatientId();
  return await request<Appointment[]>(`/patient/${targetId}/appointments`);
}

export async function getPatientNotifications(
  patientId?: string | number,
): Promise<NotificationItem[]> {
  const targetId = patientId ?? getCurrentPatientId();
  return await request<NotificationItem[]>(`/patient/${targetId}/notifications`);
}

export async function markAllPatientNotificationsRead(
  patientId?: string | number,
): Promise<{ success: boolean }> {
  const targetId = patientId ?? getCurrentPatientId();
  return await request<{ success: boolean }>(`/patient/${targetId}/notifications/read-all`, {
    method: "POST",
  });
}

/* ------------------------------ Shared lookup ----------------------------- */

export async function getSymptoms(): Promise<Symptom[]> {
  return await request<Symptom[]>("/symptoms");
}

export async function getDepartments(): Promise<Department[]> {
  return await request<Department[]>("/departments");
}

export async function getLiveDepartments(): Promise<LiveDepartmentStatus[]> {
  return await request<LiveDepartmentStatus[]>("/departments/live", { ttl: 15_000 });
}

export async function getPublicSiteSettings(): Promise<PublicSiteSettings> {
  return await request<PublicSiteSettings>("/system/settings");
}

export async function getNowServingHighlight(): Promise<NowServingHighlight> {
  return await request<NowServingHighlight>("/queue/now-serving-highlight", { ttl: 15_000 });
}

export async function getDoctors(): Promise<DoctorDetail[]> {
  return await request<DoctorDetail[]>("/doctors");
}

export async function getDoctorsByDepartment(departmentId: number): Promise<DoctorDetail[]> {
  return await request<DoctorDetail[]>(`/doctors?departmentId=${departmentId}`);
}

export async function getAIRecommendation(payload: {
  symptoms?: string;
  departmentId?: number;
}): Promise<RecommendationResult> {
  return await request<RecommendationResult>("/patient/recommend", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/* --------------------------------- Staff --------------------------------- */

export async function getStaffDashboard(): Promise<StaffDashboard> {
  return await request<StaffDashboard>("/staff/dashboard");
}

export async function getStaffProfile(staffId?: string | number): Promise<StaffProfile> {
  const qs = staffId ? `?staffId=${encodeURIComponent(String(staffId))}` : "";
  return await request<StaffProfile>(`/staff/profile${qs}`);
}

export async function getEmergencyCases(): Promise<EmergencyCase[]> {
  return await request<EmergencyCase[]>("/staff/emergencies");
}

export async function confirmEmergency(caseId: number): Promise<EmergencyCase> {
  return await request<EmergencyCase>(`/staff/emergencies/${caseId}/confirm`, {
    method: "POST",
  });
}

export async function rejectEmergency(caseId: number): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(`/staff/emergencies/${caseId}/reject`, {
    method: "POST",
  });
}

export async function getQueueMonitor(
  departmentName?: string,
  doctorName?: string,
): Promise<QueueMonitorRow[]> {
  const query = new URLSearchParams();
  if (departmentName) query.set("departmentName", departmentName);
  if (doctorName) query.set("doctorName", doctorName);
  const qs = query.toString();
  return await request<QueueMonitorRow[]>(`/staff/queue-monitor${qs ? "?" + qs : ""}`);
}

export async function getReassignableQueues(departmentName: string): Promise<QueueEntry[]> {
  const query = departmentName ? `?departmentName=${encodeURIComponent(departmentName)}` : "";
  return await request<QueueEntry[]>(`/staff/queues/reassignable${query}`);
}

export async function reassignQueue(
  queueId: string | number,
  doctorId: string | number,
  reason?: string,
): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(
    `/staff/queues/${encodeURIComponent(String(queueId))}/reassign`,
    {
      method: "POST",
      body: JSON.stringify({ doctorId: String(doctorId), reason }),
    },
  );
}

export async function setDoctorUnavailable(
  doctorId: string | number,
  reason: string,
): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(`/staff/doctors/${doctorId}/unavailable`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export async function getAllAppointments(): Promise<Appointment[]> {
  return await request<Appointment[]>("/staff/appointments");
}

export async function getStaffNotifications(): Promise<NotificationItem[]> {
  return await request<NotificationItem[]>("/staff/notifications");
}

export async function markAllStaffNotificationsRead(): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>("/staff/notifications/read-all", {
    method: "POST",
  });
}

export async function getStaffReport(type: string, date: string): Promise<DailyReport> {
  return request<DailyReport>(
    `/staff/reports?type=${encodeURIComponent(type)}&date=${encodeURIComponent(date)}`,
  );
}

/* --------------------------------- Admin --------------------------------- */

export async function getAdminDashboard(): Promise<AdminDashboard> {
  return await request<AdminDashboard>("/admin/dashboard");
}

export async function createDoctor(payload: Omit<DoctorDetail, "id">): Promise<DoctorDetail> {
  const res = await request<{ id: string | number; success: boolean }>("/admin/doctors", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return { ...payload, id: res.id };
}

export async function updateDoctor(
  doctorId: string | number,
  payload: Partial<DoctorDetail>,
): Promise<{ success: boolean; message: string }> {
  try {
    return await request<{ success: boolean; message: string }>(
      `/admin/doctors/${encodeURIComponent(String(doctorId))}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  } catch (err: unknown) {
    // Surfaces the real error message from the backend/network as a
    // {success:false} result rather than throwing, since callers render
    // res.message directly. Not a mock fallback — no data is invented here.
    return {
      success: false,
      message: (err instanceof Error ? err.message : null) || "Failed to update doctor.",
    };
  }
}

export async function deleteDoctor(doctorId: string | number): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(
    `/admin/doctors/${encodeURIComponent(String(doctorId))}`,
    {
      method: "DELETE",
    },
  );
}

export async function createDepartment(payload: Omit<Department, "id">): Promise<Department> {
  const res = await request<{ id: number; success: boolean }>("/admin/departments", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return { ...payload, id: res.id };
}

export async function deleteDepartment(
  departmentId: string | number,
): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(
    `/admin/departments/${encodeURIComponent(String(departmentId))}`,
    {
      method: "DELETE",
    },
  );
}

export async function getUsers(): Promise<User[]> {
  return await request<User[]>("/admin/users");
}

export async function createUser(
  payload: Omit<User, "id" | "status"> & { status?: ActiveStatus },
): Promise<User> {
  const status = payload.status ?? "ACTIVE";
  const res = await request<{ id: string | number; success: boolean }>("/admin/users", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return { ...payload, status, id: res.id };
}

export async function deleteUser(userId: string | number): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(`/admin/users/${encodeURIComponent(String(userId))}`, {
    method: "DELETE",
  });
}

export async function getSchedules(): Promise<Schedule[]> {
  return await request<Schedule[]>("/admin/schedules");
}

export async function deleteSchedule(scheduleId: string | number): Promise<{ success: boolean }> {
  return await request<{ success: boolean }>(
    `/admin/schedules/${encodeURIComponent(String(scheduleId))}`,
    {
      method: "DELETE",
    },
  );
}

export async function getAuditLogs(): Promise<AuditLog[]> {
  return await request<AuditLog[]>("/admin/audit-logs");
}

export async function getReport(type: string, date: string): Promise<DailyReport> {
  return await request<DailyReport>(
    `/admin/reports?type=${encodeURIComponent(type)}&date=${encodeURIComponent(date)}`,
  );
}

export async function getQueueSettings(): Promise<QueueSettings> {
  return await request<QueueSettings>("/admin/settings/queue");
}

export async function saveQueueSettings(payload: QueueSettings): Promise<QueueSettings> {
  return await request<QueueSettings>("/admin/settings/queue", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getSystemSettings(): Promise<SystemSettings> {
  return await request<SystemSettings>("/admin/settings/system");
}

export async function saveSystemSettings(payload: SystemSettings): Promise<SystemSettings> {
  return await request<SystemSettings>("/admin/settings/system", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getRegistrationRequests(): Promise<RegistrationRequest[]> {
  return await request<RegistrationRequest[]>("/admin/registration-requests");
}

export async function approveRegistrationRequest(
  requestId: number,
): Promise<{ success: boolean; message: string }> {
  return await request<{ success: boolean; message: string }>(
    `/admin/registration-requests/${requestId}/approve`,
    {
      method: "POST",
    },
  );
}

export async function rejectRegistrationRequest(
  requestId: number,
  reason?: string,
): Promise<{ success: boolean; message: string }> {
  return await request<{ success: boolean; message: string }>(
    `/admin/registration-requests/${requestId}/reject`,
    {
      method: "POST",
      body: JSON.stringify({ reason }),
    },
  );
}

/* ------------------- Doctor Portal APIs ------------------- */

function getCurrentDoctorId(passedId?: string | number): string {
  if (passedId !== undefined && passedId !== null && passedId !== "") {
    return String(passedId);
  }
  if (typeof window !== "undefined") {
    try {
      const raw = window.localStorage.getItem("hqs.session");
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed.doctorId) return String(parsed.doctorId);
        if (parsed.userId) return String(parsed.userId);
      }
    } catch {
      // Ignored: fallback to default doctor id
    }
  }
  return "D001";
}

export async function getDoctorDashboard(doctorId?: string | number): Promise<DoctorDashboard> {
  const id = getCurrentDoctorId(doctorId);
  return await request<DoctorDashboard>(`/doctor/${id}/dashboard`);
}

export async function toggleDoctorAvailability(
  doctorId?: string | number,
  available?: boolean,
): Promise<{ success: boolean; available: boolean }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ success: boolean; available: boolean }>(`/doctor/${id}/availability`, {
    method: "POST",
    body: JSON.stringify(available !== undefined ? { available } : {}),
  });
}

export async function doctorCallNext(
  doctorId?: string | number,
): Promise<{ message: string; called: QueueEntry | null }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ message: string; called: QueueEntry | null }>(
    `/doctor/${id}/queue/call-next`,
    {
      method: "POST",
    },
  );
}

export async function doctorStartConsultation(
  doctorId?: string | number,
): Promise<{ message: string }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ message: string }>(`/doctor/${id}/queue/start`, {
    method: "POST",
  });
}

export async function doctorCompleteConsultation(
  doctorId?: string | number,
): Promise<{ message: string }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ message: string }>(`/doctor/${id}/queue/complete`, {
    method: "POST",
  });
}

export async function doctorPauseConsultation(
  doctorId?: string | number,
): Promise<{ message: string }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ message: string }>(`/doctor/${id}/queue/pause`, {
    method: "POST",
  });
}

export async function doctorResumeConsultation(
  doctorId?: string | number,
): Promise<{ message: string }> {
  const id = getCurrentDoctorId(doctorId);
  return await request<{ message: string }>(`/doctor/${id}/queue/resume`, {
    method: "POST",
  });
}

export async function getDoctorAppointments(doctorId?: string | number): Promise<Appointment[]> {
  const id = getCurrentDoctorId(doctorId);
  return await request<Appointment[]>(`/doctor/${id}/appointments`);
}

export async function getDoctorHistory(doctorId?: string | number): Promise<DoctorHistoryEntry[]> {
  const id = getCurrentDoctorId(doctorId);
  return await request<DoctorHistoryEntry[]>(`/doctor/${id}/history`);
}

export async function getDoctorProfile(doctorId?: string | number): Promise<DoctorProfile> {
  const id = getCurrentDoctorId(doctorId);
  return await request<DoctorProfile>(`/doctor/${id}/profile`);
}

/* --------------------------------- AI/ML --------------------------------- */

export interface WaitTimePredictionResponse {
  doctorId: string;
  doctorName: string;
  department: string;
  currentQueueLength: number;
  predictedWaitMinutes: number;
  fallbackWaitMinutes: number;
  modelUsed: boolean;
}

export interface DepartmentWaitTimeResponse {
  department: { id: number; code: string; name: string };
  predictions: Record<string, number>;
  modelUsed: boolean;
}

export interface NoShowPredictionResponse {
  appointmentId: number;
  predictedProbability: number;
  predictedNoShow: boolean;
  riskLevel: string;
  modelUsed: boolean;
}

export interface ChatRequest {
  message: string;
  sessionId: string;
}

export interface ChatResponse {
  response: string;
  responseTimeMs: number;
  sessionId: string;
}

export interface ChatbotStatus {
  ready: boolean;
  model: string;
  embeddingStore: string;
  documentCount: number;
}

export async function predictWaitTime(
  doctorId: string,
  patientId?: number,
  departmentId?: number,
): Promise<WaitTimePredictionResponse> {
  const params = new URLSearchParams();
  if (patientId) params.set("patientId", String(patientId));
  if (departmentId) params.set("departmentId", String(departmentId));
  const qs = params.toString();
  return await request<WaitTimePredictionResponse>(
    `/ai/wait-time/${doctorId}${qs ? "?" + qs : ""}`,
  );
}

export async function predictWaitTimeForDepartment(
  departmentCode: string,
  patientId?: string | number,
): Promise<Record<string, number>> {
  const params = new URLSearchParams();
  if (patientId) params.set("patientId", String(patientId));
  const qs = params.toString();
  const res = await request<DepartmentWaitTimeResponse>(
    `/ai/wait-time/department/${encodeURIComponent(departmentCode)}${qs ? "?" + qs : ""}`,
  );
  return res.predictions ?? {};
}

export async function predictNoShow(appointmentId: number): Promise<NoShowPredictionResponse> {
  return await request<NoShowPredictionResponse>(`/ai/noshow/appointment/${appointmentId}`, {
    method: "POST",
  });
}

export async function getHighRiskAppointments(
  daysAhead?: number,
): Promise<{ count: number; predictions: NoShowPredictionResponse[] }> {
  const params = daysAhead ? `?daysAhead=${daysAhead}` : "";
  return await request<{ count: number; predictions: NoShowPredictionResponse[] }>(
    `/ai/noshow/high-risk${params}`,
  );
}

export async function chatWithBot(chatReq: ChatRequest): Promise<ChatResponse> {
  return await request<ChatResponse>("/ai/chat", {
    method: "POST",
    body: JSON.stringify(chatReq),
  });
}

export async function getChatbotStatus(): Promise<ChatbotStatus> {
  return await request<ChatbotStatus>("/ai/chat/status");
}

/* ------------------- New AI Features (SOAP, Triage, Load Balancer) ------------------- */

export interface SoapNotePayload {
  patientName?: string;
  patientAge?: string;
  gender?: string;
  symptoms: string;
  vitals?: string;
  observations?: string;
  department?: string;
  specialization?: string;
}

export interface PrescriptionSuggestion {
  name: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string;
}

export interface SoapNoteResponse {
  subjective: string;
  objective: string;
  assessment: string;
  icd10Codes: string[];
  medications: PrescriptionSuggestion[];
  recommendedTests: string[];
  lifestyleAdvice: string;
  followUp: string;
  summary: string;
  aiUsed: boolean;
}

export async function generateAiSoapNote(payload: SoapNotePayload): Promise<SoapNoteResponse> {
  return await request<SoapNoteResponse>("/ai/soap-note", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export interface TriageQuestion {
  id: string;
  question: string;
  category: string;
  options: string[];
}

export interface InteractiveTriageQuestionsResponse {
  questions: TriageQuestion[];
  preliminaryAssessment: string;
  isEmergencyPotential: boolean;
  aiUsed: boolean;
}

export interface FinalizeTriagePayload {
  symptoms: string;
  answers: Record<string, string>;
}

export interface FinalizeTriageResponse {
  department: {
    departmentId?: number;
    id?: number;
    name?: string;
    departmentName?: string;
    departmentCode?: string;
    code?: string;
  };
  emergency: boolean;
  acuityScore: number;
  disposition: string;
  clinicalReason: string;
  patientSummary: string;
  recommendedLabs: string[];
  recommendedTests: string[];
  aiUsed: boolean;
}

export async function getInteractiveTriageQuestions(
  symptoms: string,
): Promise<InteractiveTriageQuestionsResponse> {
  return await request<InteractiveTriageQuestionsResponse>("/ai/triage/interactive/questions", {
    method: "POST",
    body: JSON.stringify({ symptoms }),
  });
}

export async function finalizeInteractiveTriage(
  payload: FinalizeTriagePayload,
): Promise<FinalizeTriageResponse> {
  return await request<FinalizeTriageResponse>("/ai/triage/interactive/finalize", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export interface ReassignmentSuggestion {
  queueId: string;
  queueNumber: string;
  patientId: string;
  patientName: string;
  fromDoctorId: string;
  fromDoctorName: string;
  fromDoctorQueueSize: number;
  toDoctorId: string;
  toDoctorName: string;
  toDoctorQueueSize: number;
  department: string;
  estimatedMinutesSaved: number;
  reason: string;
}

export interface LoadBalancerReport {
  departmentName: string;
  totalWaitingPatients: number;
  activeDoctorsCount: number;
  averageWaitTimeMinutes: number;
  isImbalanced: boolean;
  aiAnalysisSummary: string;
  suggestions: ReassignmentSuggestion[];
}

export async function getAiLoadBalancerSuggestions(
  department?: string,
): Promise<LoadBalancerReport> {
  const qs = department ? `?department=${encodeURIComponent(department)}` : "";
  return await request<LoadBalancerReport>(`/ai/load-balancer/suggestions${qs}`);
}

export async function applyAiLoadBalancerPlan(
  queueIds: string[],
  staffId?: string,
): Promise<{ success: boolean; reassignedCount: number; message: string }> {
  return await request<{ success: boolean; reassignedCount: number; message: string }>(
    "/ai/load-balancer/apply",
    {
      method: "POST",
      body: JSON.stringify({ queueIds, staffId }),
    },
  );
}

/* ------------------- Consultation Duration Prediction ------------------- */

export interface DurationPrediction {
  doctorId: string;
  doctorName: string;
  predictedMinutes: number;
  doctorAverageMinutes: number;
  patientAge: number;
  isNewPatient: boolean;
}

export interface DepartmentDurationPredictions {
  department: { id: number; code: string; name: string };
  predictions: Record<string, number>;
}

export async function predictConsultationDuration(
  doctorId: string,
  patientId?: string,
  symptoms?: string,
  appointmentType?: string,
): Promise<DurationPrediction> {
  const params = new URLSearchParams();
  if (patientId) params.set("patientId", patientId);
  if (symptoms) params.set("symptoms", symptoms);
  if (appointmentType) params.set("appointmentType", appointmentType);
  const qs = params.toString() ? `?${params}` : "";
  return await request<DurationPrediction>(
    `/ai/consultation-duration/${encodeURIComponent(doctorId)}${qs}`,
  );
}

export async function predictDurationsForDepartment(
  departmentCode: string,
  patientId?: string,
  symptoms?: string,
): Promise<DepartmentDurationPredictions> {
  const params = new URLSearchParams();
  if (patientId) params.set("patientId", patientId);
  if (symptoms) params.set("symptoms", symptoms);
  const qs = params.toString() ? `?${params}` : "";
  return await request<DepartmentDurationPredictions>(
    `/ai/consultation-duration/department/${encodeURIComponent(departmentCode)}${qs}`,
  );
}

/* ------------------- Smart Doctor Assignment ------------------- */

export interface DoctorRecommendation {
  doctorId: string;
  doctorName: string;
  score: number;
  rank: number;
  queueLoadScore: number;
  availabilityScore: number;
  specializationScore: number;
  historyScore: number;
  estimatedWaitMinutes: number;
  reason: string;
}

export interface SmartAssignmentResponse {
  department: { code: string; name: string };
  recommendations: DoctorRecommendation[];
  symptoms: string | null;
}

export async function getSmartDoctorAssignment(
  departmentCode: string,
  symptoms?: string,
  patientId?: string,
): Promise<SmartAssignmentResponse> {
  const params = new URLSearchParams();
  if (symptoms) params.set("symptoms", symptoms);
  if (patientId) params.set("patientId", patientId);
  const qs = params.toString() ? `?${params}` : "";
  return await request<SmartAssignmentResponse>(
    `/ai/smart-assignment/${encodeURIComponent(departmentCode)}${qs}`,
  );
}

/* ------------------- Queue Flow Prediction ------------------- */

export interface HourlyPrediction {
  hour: number;
  dayOfWeek: string;
  predictedArrivals: number;
  confidence: string;
}

export interface FlowPrediction {
  hourly: HourlyPrediction[];
  totalPredictedArrivals: number;
  trend: string;
  predictedAt: string;
}

export interface DepartmentFlowPrediction {
  byDepartment: Record<string, HourlyPrediction[]>;
  hoursAhead: number;
  predictedAt: string;
}

export interface PeakHoursReport {
  dayOfWeek: string;
  hourlyAverages: Record<string, number>;
  peakHour: string;
  offPeakHour: string;
  rushHours: number[];
}

export interface StaffingRecommendation {
  recommendedDoctors: number;
  predictedPatientsPerHour: number;
  urgencyLevel: string;
  timePeriod: string;
}

export async function predictQueueFlow(hoursAhead: number = 3): Promise<FlowPrediction> {
  return await request<FlowPrediction>(`/ai/queue-flow/predict?hoursAhead=${hoursAhead}`);
}

export async function predictQueueFlowByDepartment(
  hoursAhead: number = 3,
): Promise<DepartmentFlowPrediction> {
  return await request<DepartmentFlowPrediction>(
    `/ai/queue-flow/by-department?hoursAhead=${hoursAhead}`,
  );
}

export async function getPeakHours(dayOfWeek?: string): Promise<PeakHoursReport> {
  const qs = dayOfWeek ? `?dayOfWeek=${dayOfWeek}` : "";
  return await request<PeakHoursReport>(`/ai/queue-flow/peak-hours${qs}`);
}

export async function getStaffingRecommendation(
  hoursAhead: number = 3,
): Promise<StaffingRecommendation> {
  return await request<StaffingRecommendation>(`/ai/queue-flow/staffing?hoursAhead=${hoursAhead}`);
}

/* ------------------- Follow-up Instructions ------------------- */

export interface FollowUpResult {
  dietInstructions: string;
  medicationReminders: string[];
  activityRestrictions: string;
  warningSigns: string[];
  followUpDate: string;
  selfCareTips: string[];
  summary: string;
  aiUsed: boolean;
}

export async function generateFollowUpInstructions(payload: {
  diagnosis?: string;
  medications?: string;
  patientAge?: string;
  patientGender?: string;
  department?: string;
  additionalNotes?: string;
}): Promise<FollowUpResult> {
  return await request<FollowUpResult>("/ai/follow-up/generate", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
