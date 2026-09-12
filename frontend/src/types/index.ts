/**
 * Data contract shared with the Spring Boot backend.
 * Field names mirror the JSON returned by the API exactly.
 */

export type QueueStatus =
  "WAITING" | "CALLED" | "IN_CONSULTATION" | "PAUSED" | "COMPLETED" | "CANCELLED" | "MISSED";

export type QueueType = "NORMAL" | "APPOINTMENT" | "EMERGENCY";

export type DoctorStatus = "CONSULTING" | "ON_BREAK" | "UNAVAILABLE";

export type ActiveStatus = "ACTIVE" | "INACTIVE";

export type Priority = "LOW" | "MEDIUM" | "HIGH";

export type UserRole = "PATIENT" | "DOCTOR" | "STAFF" | "ADMIN";

export interface Patient {
  id: string | number;
  name: string;
  status: string;
  queueNumber: string | number;
  isEmergency: boolean;
}

export interface Doctor {
  id: string | number;
  name: string;
  department: string;
  qualification?: string | undefined;
  specialization?: string | undefined;
  experienceYears?: number | undefined;
  currentPatientId: number;
}

export interface DoctorDetail extends Doctor {
  doctorCode: string;
  departmentId?: number | undefined;
  qualification?: string | undefined;
  specialization?: string | undefined;
  experienceYears: number;
  estimatedWaitingMinutes: number;
  status: ActiveStatus;
  phone: string;
  availabilityStatus: DoctorStatus;
}

export interface Department {
  id: number;
  departmentCode: string;
  name: string;
  location: string;
  status: ActiveStatus;
}

export interface Symptom {
  id: number;
  name: string;
  departmentId: number;
}

export interface RecommendationDoctor {
  id: string | number;
  doctorCode?: string;
  name: string;
  department: string;
  qualification?: string;
  specialization?: string;
  experienceYears?: number;
  estimatedWaitingMinutes: number;
  available: boolean;
  availabilityStatus?: DoctorStatus;
}

export interface RecommendationResult {
  department: {
    id: number;
    departmentCode: string;
    name: string;
  };
  emergency: boolean;
  reason: string;
  aiUsed: boolean;
  doctors: RecommendationDoctor[];
}

export interface QueueEntry {
  id: number | string;
  queueNumber: string;
  patientId: string | number;
  patientName: string;
  departmentId: number;
  departmentName: string;
  doctorId: string | number;
  doctorName: string;
  position: number;
  estimatedWaitingMinutes: number;
  waitingMinutes: number;
  status: QueueStatus;
  type: QueueType;
  emergency?: boolean;
  createdAt: string;
}

export interface CreateQueueRequest {
  patientId: string | number;
  symptomIds: number[];
  departmentId: number;
  doctorId: string | number;
  emergency?: boolean;
}

export interface Appointment {
  id: number | string;
  patientId: string | number;
  patientName: string;
  doctorId: string | number;
  doctorName: string;
  departmentName: string;
  appointmentDate: string;
  appointmentTime: string;
  status: string;
  notes?: string;
}

export interface EmergencyCase {
  id: number;
  patientCode: string;
  patientId: string | number;
  patientName: string;
  symptoms: string;
  requestedTime: string;
  priority: Priority;
  resolved: boolean;
  handledBy: string | null;
  confirmedTime: string | null;
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  time: string;
  read: boolean;
  important: boolean;
  category: "EMERGENCY" | "QUEUE" | "DOCTOR" | "SCHEDULE";
}

export interface PatientProfile {
  id: string | number;
  name: string;
  phone: string;
  dateOfBirth: string;
  gender: string;
  email?: string;
  address?: string;
  photoUrl?: string;
}

export interface StaffProfile {
  id: string | number;
  staffCode: string;
  name: string;
  role: string;
  department: string;
  phone: string;
  email: string;
  workingShift: string;
  joiningDate: string;
  avatarUrl: string;
}

export interface User {
  id: string | number;
  userCode?: string | undefined;
  name: string;
  role: string;
  contact: string;
  phone?: string;
  email?: string;
  qualification?: string;
  experienceYears?: number;
  department?: string;
  status: ActiveStatus;
}

export interface Schedule {
  id: string | number;
  doctorName: string;
  department: string;
  day: string;
  startTime: string;
  endTime: string;
  breakTime: string;
  room: string;
}

export interface AuditLog {
  id: string | number;
  dateTime: string;
  user: string;
  action: string;
  ipAddress: string;
}

export interface ReportRow {
  appointmentId?: string;
  department?: string;
  doctorCode?: string;
  doctorName?: string;
  qualification?: string;
  specialization?: string;
  experienceYears?: number;
  totalServed?: number;
  currentWaiting?: number;
  avgConsultationMinutes?: number;
  queueNumber?: string;
  patientId?: string;
  patientName?: string;
  assignedDoctor?: string;
  time?: string;
  timeSlot?: string;
  date?: string;
  servingTime?: string;
  waitingTime?: string;
  confirmed?: string;
  totalQueues?: number;
  completed?: number;
  cancelled?: number;
  missed?: number;
  avgWaitingMinutes?: number;
  status?: string;
}

export interface DailyReport {
  type?: string;
  date: string;
  totalQueues: number;
  completed: number;
  cancelled: number;
  missed: number;
  rows: ReportRow[];
}

export interface QueueMonitorRow {
  departmentName: string;
  doctorName: string;
  status: DoctorStatus;
  nowServing: string | null;
  waiting: number;
  completed: number;
  avgWaitingMinutes: number | null;
}

export interface StaffDashboard {
  totalQueuesToday: number;
  emergencyPending: number;
  activeDoctors: number;
  checkedInAppointments: number;
  breakdown: { label: string; value: number; percent: number; color: string }[];
  highlights: { label: string; value: number }[];
}

export interface DoctorDashboard {
  doctor: {
    id: string | number;
    name: string;
    doctorCode: string;
    department: string;
    departmentId: number;
    specialization: string;
    available: boolean;
    phone: string;
    email: string;
    maxQueueSize: number;
    averageConsultationMinutes: number;
  };
  stats: {
    totalPatientsToday: number;
    waitingCount: number;
    completedCount: number;
    averageConsultationMinutes: number;
  };
  called: QueueEntry | null;
  serving: QueueEntry | null;
  waiting: QueueEntry[];
  todayAppointments: Appointment[];
}

export interface DoctorProfile {
  id: string | number;
  name: string;
  doctorCode: string;
  department: string;
  departmentId: number;
  specialization: string;
  phone: string;
  email: string;
  available: boolean;
  averageConsultationMinutes: number;
  maxQueueSize: number;
  queueOpenTime: string;
  queueCloseTime: string;
}

export interface AdminDashboard {
  totalPatientsToday: number;
  totalQueuesToday: number;
  averageWaitingMinutes: number;
  doctorsOnDuty: number;
  weekly: { date: string; queues: number; completed: number }[];
  byDepartment: { name: string; percent: number; color: string }[];
}

export interface QueueSettings {
  registrationStartTime: string;
  registrationEndTime: string;
  maxWaitingMinutes: number;
  notifyBeforeTurns: number;
  autoCancelAfterMissedTurn: boolean;
  allowFutureBooking: boolean;
}

export interface SystemSettings {
  hospitalName: string;
  timeZone: string;
  dateFormat: string;
  timeFormat: string;
  logoUrl: string;
  contactPhone?: string | undefined;
  contactEmail?: string | undefined;
  operatingHours?: string | undefined;
}

export interface LoginRequest {
  username: string;
  password: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  userId: string | number;
  name: string;
  role: UserRole;
  doctorId?: string | number;
}

export interface RegisterPatientRequest {
  name: string;
  phone: string;
  password?: string;
  dateOfBirth: string;
  gender: string;
  email?: string;
  address?: string;
}

export interface RegistrationRequest {
  id: number;
  name: string;
  phone: string;
  email: string;
  dateOfBirth: string;
  gender: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
}
