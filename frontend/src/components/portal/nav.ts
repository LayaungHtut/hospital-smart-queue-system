import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bell,
  Building2,
  CalendarDays,
  ClipboardList,
  Clock,
  FileText,
  HelpCircle,
  LayoutDashboard,
  ScrollText,
  Settings,
  Settings2,
  Sparkles,
  Stethoscope,
  User,
  Users,
} from "lucide-react";
import type { NavItem } from "./PortalShell";

export const patientNav: NavItem[] = [
  { label: "Dashboard", to: "/dashboard", icon: LayoutDashboard },
  { label: "AI Symptom & Queue", to: "/queue/new", icon: Sparkles },
  { label: "My Queue", to: "/queue", icon: ClipboardList, exact: true },
  { label: "My Profile", to: "/profile", icon: User },
  { label: "My Appointments", to: "/appointments", icon: CalendarDays },
  { label: "Notifications", to: "/notifications", icon: Bell },
  { label: "Help", to: "/help", icon: HelpCircle },
];

export const doctorNav: NavItem[] = [
  { label: "Dashboard", to: "/doctor", icon: LayoutDashboard, exact: true },
  { label: "Consultation Queue", to: "/doctor/queue", icon: Activity },
  { label: "My Appointments", to: "/doctor/appointments", icon: CalendarDays },
  { label: "History & Logs", to: "/doctor/history", icon: ClipboardList },
  { label: "My Profile", to: "/doctor/profile", icon: User },
];

export const staffNav: NavItem[] = [
  { label: "Dashboard", to: "/staff", icon: LayoutDashboard, exact: true },
  { label: "Emergency Cases", to: "/staff/emergency", icon: AlertTriangle },
  { label: "Doctor Assignment", to: "/staff/doctor-assignment", icon: Stethoscope },
  { label: "Queue Monitor", to: "/staff/queue-monitor", icon: Activity },
  { label: "Appointments", to: "/staff/appointments", icon: CalendarDays },
  { label: "Reports", to: "/staff/reports", icon: FileText },
  { label: "Notifications", to: "/staff/notifications", icon: Bell },
  { label: "My Profile", to: "/staff/profile", icon: User },
];

export const adminNav: NavItem[] = [
  { label: "Dashboard", to: "/admin", icon: LayoutDashboard, exact: true },
  { label: "Doctors", to: "/admin/doctors", icon: Stethoscope },
  { label: "Departments", to: "/admin/departments", icon: Building2 },
  { label: "Schedules", to: "/admin/schedules", icon: CalendarDays },
  { label: "Queue Settings", to: "/admin/queue-settings", icon: Settings2 },
  { label: "Users", to: "/admin/users", icon: Users },
  { label: "Reports", to: "/admin/reports", icon: BarChart3 },
  { label: "System Settings", to: "/admin/system-settings", icon: Settings },
  { label: "Audit Logs", to: "/admin/audit-logs", icon: ScrollText },
  { label: "AI Duration", to: "/admin/ai-consultation-duration", icon: Clock },
  { label: "AI Assignment", to: "/admin/ai-smart-assignment", icon: Sparkles },
  { label: "AI Queue Flow", to: "/admin/ai-queue-flow", icon: Activity },
  { label: "AI Follow-up", to: "/admin/ai-followup", icon: FileText },
];
