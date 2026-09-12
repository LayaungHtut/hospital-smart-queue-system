import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getDoctorAppointments, getDoctorProfile } from "@/services/api";
import { DoctorLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, type Column } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonTable } from "@/components/ui/loading";
import type { Appointment } from "@/types";

export const Route = createFileRoute("/doctor/appointments")({
  head: () => ({
    meta: [
      { title: "My Appointments — Doctor Portal" },
      { name: "description", content: "Doctor scheduled patient appointments and consultations." },
      { property: "og:title", content: "My Appointments — Doctor Portal" },
      { property: "og:description", content: "Doctor scheduled appointments." },
    ],
  }),
  component: DoctorAppointmentsPage,
});

function DoctorAppointmentsPage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [doctorId, setDoctorId] = useState<string | number>(
    session?.doctorId ?? session?.userId ?? "D001",
  );

  // Sync doctorId from session
  useEffect(() => {
    const currentDocId = session?.doctorId ?? session?.userId;
    if (currentDocId && currentDocId !== doctorId) {
      setDoctorId(currentDocId);
    } else if (!session?.doctorId && session?.name && session?.role === "DOCTOR") {
      getDoctorProfile(session.name)
        .then((profile) => {
          if (profile?.id) {
            setDoctorId(profile.id);
          }
        })
        .catch(console.error);
    }
  }, [session, doctorId]);

  useEffect(() => {
    getDoctorAppointments(doctorId)
      .then((res) => {
        setRows(res);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [doctorId]);

  const columns: Column<Appointment>[] = [
    {
      header: "Date",
      cell: (r) => <span className="font-medium text-foreground">{r.appointmentDate}</span>,
    },
    {
      header: "Scheduled Time",
      cell: (r) => <span className="font-bold text-primary">{r.appointmentTime}</span>,
    },
    { header: "Patient Name", cell: (r) => <span className="font-semibold">{r.patientName}</span> },
    { header: "Department", cell: (r) => r.departmentName },
    { header: "Notes / Symptoms", cell: (r) => r.notes ?? "Regular checkup" },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
  ];

  return (
    <DoctorLayout title="My Appointments">
      <Panel title={`Doctor Scheduled Appointments (${rows.length})`}>
        {loading ? (
          <SkeletonTable rows={5} columns={6} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            emptyMessage="No appointments scheduled for your profile."
            pageSize={10}
          />
        )}
      </Panel>
    </DoctorLayout>
  );
}
