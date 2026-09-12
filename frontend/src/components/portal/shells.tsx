import type { ReactNode } from "react";
import { PortalShell } from "./PortalShell";
import { adminNav, doctorNav, patientNav, staffNav } from "./nav";
import { useAuth } from "@/lib/auth";

export function PatientLayout({ title, children }: { title: string; children: ReactNode }) {
  const { session } = useAuth();
  return (
    <PortalShell
      portal="patient"
      brand="HOSPITAL"
      brandSub="Queue System"
      title={title}
      nav={patientNav}
      userName={session?.name ?? ""}
      userRole="Patient"
      showClock={false}
    >
      {children}
    </PortalShell>
  );
}

export function DoctorLayout({ title, children }: { title: string; children: ReactNode }) {
  const { session } = useAuth();
  return (
    <PortalShell
      portal="doctor"
      brand="Doctor Portal"
      brandSub="Consultation Suite"
      title={title}
      nav={doctorNav}
      userName={session?.name ?? ""}
      userRole="Specialist Doctor"
      showClock={true}
    >
      {children}
    </PortalShell>
  );
}

export function StaffLayout({ title, children }: { title: string; children: ReactNode }) {
  const { session } = useAuth();
  return (
    <PortalShell
      portal="staff"
      brand="Staff Portal"
      title={title}
      nav={staffNav}
      userName={session?.name ?? ""}
      userRole="Receptionist"
      showClock={true}
    >
      {children}
    </PortalShell>
  );
}

export function AdminLayout({ title, children }: { title: string; children: ReactNode }) {
  const { session } = useAuth();
  return (
    <PortalShell
      portal="admin"
      brand="Admin Portal"
      title={title}
      nav={adminNav}
      userName={session?.name ?? ""}
      userRole="Administrator"
      showClock={true}
    >
      {children}
    </PortalShell>
  );
}
