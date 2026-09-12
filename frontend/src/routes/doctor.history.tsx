import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getDoctorHistory, getDoctorProfile } from "@/services/api";
import { DoctorLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, type Column } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonTable } from "@/components/ui/loading";

export const Route = createFileRoute("/doctor/history")({
  head: () => ({
    meta: [
      { title: "Consultation History — Doctor Portal" },
      { name: "description", content: "Doctor consultation history and logs." },
      { property: "og:title", content: "Consultation History — Doctor Portal" },
      { property: "og:description", content: "Doctor consultation history." },
    ],
  }),
  component: DoctorHistoryPage,
});

interface HistoryEntry {
  id: number;
  queueNumber: string;
  patientId: string | number;
  status: string;
  reason: string;
  createdAt: string;
}

function DoctorHistoryPage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<HistoryEntry[]>([]);
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
    getDoctorHistory(doctorId)
      .then((res) => {
        setRows(res);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [doctorId]);

  const columns: Column<HistoryEntry>[] = [
    {
      header: "Queue No.",
      cell: (r) => <span className="font-bold text-primary">{r.queueNumber}</span>,
    },
    { header: "Patient ID", cell: (r) => r.patientId },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    { header: "Action Note", cell: (r) => r.reason ?? "Consultation completed" },
    { header: "Timestamp", cell: (r) => r.createdAt },
  ];

  return (
    <DoctorLayout title="Consultation History & Logs">
      <Panel title={`Past Consultations & Ticket Actions (${rows.length})`}>
        {loading ? (
          <SkeletonTable rows={5} columns={5} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            emptyMessage="No recorded consultation history yet."
            pageSize={10}
          />
        )}
      </Panel>
    </DoctorLayout>
  );
}
