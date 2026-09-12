import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getPatientAppointments } from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, type Column } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonTable } from "@/components/ui/loading";
import type { Appointment } from "@/types";

export const Route = createFileRoute("/appointments")({
  head: () => ({
    meta: [
      { title: "My Appointments — Hospital Smart Queue" },
      { name: "description", content: "View and manage your scheduled hospital appointments." },
      { property: "og:title", content: "My Appointments — Hospital Smart Queue" },
      { property: "og:description", content: "View your upcoming hospital appointments." },
    ],
  }),
  component: AppointmentsPage,
});

function AppointmentsPage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getPatientAppointments(session?.userId)
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [session?.userId]);

  const columns: Column<Appointment>[] = [
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Department", cell: (r) => r.departmentName },
    { header: "Date", cell: (r) => r.appointmentDate },
    { header: "Time", cell: (r) => r.appointmentTime },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
  ];

  return (
    <PatientLayout title="My Appointments">
      <Panel title="Upcoming Appointments">
        {loading ? (
          <SkeletonTable rows={5} columns={5} />
        ) : (
          <DataTable rows={rows} columns={columns} loading={loading} pageSize={10} />
        )}
      </Panel>
    </PatientLayout>
  );
}
