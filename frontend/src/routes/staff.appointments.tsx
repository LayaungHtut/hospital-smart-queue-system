import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getAllAppointments } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, type Column } from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { Appointment } from "@/types";

export const Route = createFileRoute("/staff/appointments")({
  head: () => ({
    meta: [
      { title: "Appointments — Staff Portal" },
      { name: "description", content: "Check in and manage today's booked patient appointments." },
      { property: "og:title", content: "Appointments — Staff Portal" },
      { property: "og:description", content: "Check in and manage patient appointments." },
    ],
  }),
  component: StaffAppointmentsPage,
});

function StaffAppointmentsPage() {
  const [rows, setRows] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllAppointments()
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  function checkIn(id: string | number) {
    setRows((prev) => prev.map((r) => (r.id === id ? { ...r, status: "CONFIRMED" } : r)));
  }

  const columns: Column<Appointment>[] = [
    { header: "Patient", cell: (r) => r.patientName },
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Department", cell: (r) => r.departmentName },
    { header: "Date", cell: (r) => r.appointmentDate },
    { header: "Time", cell: (r) => r.appointmentTime },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    {
      header: "Action",
      cell: (r) =>
        r.status === "PENDING" ? (
          <button
            onClick={() => checkIn(r.id)}
            className="rounded-md bg-success-soft px-3 py-1 text-xs font-medium text-success"
          >
            Check in
          </button>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        ),
    },
  ];

  return (
    <StaffLayout title="Appointments">
      <Panel title="Today's Appointments">
        {loading ? (
          <SkeletonTable rows={5} columns={7} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            searchable
            searchKeys={["patientName", "doctorName", "departmentName"]}
            pageSize={10}
          />
        )}
      </Panel>
    </StaffLayout>
  );
}
