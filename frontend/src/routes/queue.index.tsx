import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { cancelQueue, getPatientQueue } from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, type Column } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonTable } from "@/components/ui/loading";
import type { QueueEntry } from "@/types";

export const Route = createFileRoute("/queue/")({
  head: () => ({
    meta: [
      { title: "My Queue — Hospital Smart Queue" },
      { name: "description", content: "Track your current and past hospital queue tickets." },
      { property: "og:title", content: "My Queue — Hospital Smart Queue" },
      { property: "og:description", content: "Track your queue tickets and waiting times." },
    ],
  }),
  component: MyQueuePage,
});

function MyQueuePage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<QueueEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getPatientQueue(session?.userId)
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [session?.userId]);

  async function handleCancel(id: string | number) {
    await cancelQueue(id);
    setRows((prev) => prev.map((r) => (r.id === id ? { ...r, status: "CANCELLED" } : r)));
  }

  const columns: Column<QueueEntry>[] = [
    {
      header: "Queue No.",
      cell: (r) => <span className="font-medium text-primary">{r.queueNumber}</span>,
    },
    { header: "Department", cell: (r) => r.departmentName },
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Type", cell: (r) => <StatusBadge status={r.type} /> },
    { header: "Waiting", cell: (r) => `${r.waitingMinutes} min` },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    {
      header: "Action",
      cell: (r) =>
        r.status === "WAITING" ? (
          <button
            onClick={() => handleCancel(r.id)}
            className="rounded-md bg-danger-soft px-3 py-1 text-xs font-medium text-danger hover:bg-danger/20"
          >
            Cancel
          </button>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        ),
    },
  ];

  return (
    <PatientLayout title="My Queue">
      <Panel
        title="Queue History"
        action={
          <Link
            to="/queue/new"
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
          >
            New Queue
          </Link>
        }
      >
        {loading ? (
          <SkeletonTable rows={5} columns={7} />
        ) : (
          <DataTable rows={rows} columns={columns} loading={loading} pageSize={10} />
        )}
      </Panel>
    </PatientLayout>
  );
}
