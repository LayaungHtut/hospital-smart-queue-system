import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getStaffReport } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatCard, type Column } from "@/components/portal/ui-kit";
import { SkeletonStatCard, SkeletonTable } from "@/components/ui/loading";
import type { DailyReport, ReportRow } from "@/types";

export const Route = createFileRoute("/staff/reports")({
  head: () => ({
    meta: [
      { title: "Reports — Staff Portal" },
      { name: "description", content: "Daily queue performance summary by hospital department." },
      { property: "og:title", content: "Reports — Staff Portal" },
      { property: "og:description", content: "Daily queue performance summary." },
    ],
  }),
  component: StaffReportsPage,
});

function StaffReportsPage() {
  const [report, setReport] = useState<DailyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const today = new Date().toISOString().split("T")[0] ?? new Date().toISOString().slice(0, 10);
    getStaffReport("DAILY_QUEUE", today)
      .then((r) => {
        setReport(r);
        setLoading(false);
      })
      .catch((err) => {
        console.error("[Reports] Failed to load report:", err);
        setError("Failed to load report data. Please try again later.");
        setLoading(false);
      });
  }, []);

  const columns: Column<ReportRow>[] = [
    { header: "Department", cell: (r) => r.department },
    { header: "Total Queues", cell: (r) => r.totalQueues },
    { header: "Completed", cell: (r) => r.completed },
    { header: "Cancelled", cell: (r) => r.cancelled },
    { header: "Missed", cell: (r) => r.missed },
    { header: "Avg. Waiting Time", cell: (r) => `${r.avgWaitingMinutes} min` },
  ];

  return (
    <StaffLayout title="Reports">
      <div className="space-y-6">
        {error && (
          <Panel title="Error">
            <p className="text-sm text-red-500">{error}</p>
          </Panel>
        )}
        {loading && !report ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
            </div>
            <Panel title="Daily Queue Report">
              <SkeletonTable rows={5} columns={6} />
            </Panel>
          </>
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Total Queues" value={report?.totalQueues ?? "—"} />
              <StatCard label="Completed" value={report?.completed ?? "—"} tone="success" />
              <StatCard label="Cancelled" value={report?.cancelled ?? "—"} tone="danger" />
              <StatCard label="Missed" value={report?.missed ?? "—"} tone="warning" />
            </div>
            <Panel title={`Daily Queue Report — ${report?.date ?? ""}`}>
              <DataTable
                rows={report?.rows ?? []}
                columns={columns}
                loading={loading}
                numbered={false}
                pageSize={10}
              />
            </Panel>
          </>
        )}
      </div>
    </StaffLayout>
  );
}
