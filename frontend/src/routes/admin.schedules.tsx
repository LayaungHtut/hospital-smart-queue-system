import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { deleteSchedule, getSchedules } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import { DataTable, Panel, type Column } from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { Schedule } from "@/types";

export const Route = createFileRoute("/admin/schedules")({
  head: () => ({
    meta: [
      { title: "Schedule Management — Admin Portal" },
      { name: "description", content: "Manage doctor working days, consultation hours and rooms." },
      { property: "og:title", content: "Schedule Management — Admin Portal" },
      { property: "og:description", content: "Manage doctor working days and consultation hours." },
    ],
  }),
  component: AdminSchedulesPage,
});

function AdminSchedulesPage() {
  const [rows, setRows] = useState<Schedule[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSchedules()
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function remove(id: string | number) {
    await deleteSchedule(id);
    setRows((prev) => prev.filter((s) => s.id !== id));
  }

  const columns: Column<Schedule>[] = [
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Department", cell: (r) => r.department },
    { header: "Day", cell: (r) => r.day },
    { header: "Start", cell: (r) => r.startTime },
    { header: "End", cell: (r) => r.endTime },
    { header: "Break", cell: (r) => r.breakTime },
    { header: "Room", cell: (r) => r.room },
    {
      header: "Action",
      cell: (r) => (
        <button
          onClick={() => remove(r.id)}
          className="text-xs font-medium text-danger hover:underline"
        >
          Delete
        </button>
      ),
    },
  ];

  return (
    <AdminLayout title="Schedule Management">
      <Panel title="Doctor Schedules">
        {loading ? (
          <SkeletonTable rows={5} columns={8} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            searchable
            searchKeys={["doctorName", "department", "day"]}
            pageSize={10}
          />
        )}
      </Panel>
    </AdminLayout>
  );
}
