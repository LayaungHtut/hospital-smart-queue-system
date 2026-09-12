import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getAuditLogs } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import { DataTable, Panel, type Column } from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { AuditLog } from "@/types";

export const Route = createFileRoute("/admin/audit-logs")({
  head: () => ({
    meta: [
      { title: "Audit Logs — Admin Portal" },
      {
        name: "description",
        content: "Track every account action performed inside the queue system.",
      },
      { property: "og:title", content: "Audit Logs — Admin Portal" },
      { property: "og:description", content: "Track account actions inside the queue system." },
    ],
  }),
  component: AuditLogsPage,
});

function AuditLogsPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAuditLogs()
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const columns: Column<AuditLog>[] = [
    { header: "Date & Time", cell: (r) => r.dateTime },
    { header: "User", cell: (r) => r.user },
    { header: "Action", cell: (r) => r.action },
    { header: "IP Address", cell: (r) => r.ipAddress },
  ];

  return (
    <AdminLayout title="Audit Logs">
      <Panel title="System Activity">
        {loading ? (
          <SkeletonTable rows={5} columns={4} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            searchable
            searchKeys={["user", "action"]}
            pageSize={10}
          />
        )}
      </Panel>
    </AdminLayout>
  );
}
