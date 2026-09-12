import { createFileRoute } from "@tanstack/react-router";
import { useCallback, useEffect, useState } from "react";
import { RefreshCw } from "lucide-react";
import { getDepartments, getDoctors, getQueueMonitor } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import {
  DataTable,
  Field,
  Panel,
  StatusBadge,
  inputClass,
  type Column,
} from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { Department, DoctorDetail, QueueMonitorRow } from "@/types";

export const Route = createFileRoute("/staff/queue-monitor")({
  head: () => ({
    meta: [
      { title: "Queue Monitor — Staff Portal" },
      {
        name: "description",
        content: "Live view of every doctor's queue, status and waiting time.",
      },
      { property: "og:title", content: "Queue Monitor — Staff Portal" },
      { property: "og:description", content: "Live view of doctor queues and waiting times." },
    ],
  }),
  component: QueueMonitorPage,
});

function QueueMonitorPage() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<DoctorDetail[]>([]);
  const [department, setDepartment] = useState("ALL");
  const [doctor, setDoctor] = useState("ALL");
  const [rows, setRows] = useState<QueueMonitorRow[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setRows(await getQueueMonitor(department, doctor));
    setLoading(false);
  }, [department, doctor]);

  useEffect(() => {
    getDepartments().then(setDepartments).catch(console.error);
    getDoctors().then(setDoctors).catch(console.error);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const columns: Column<QueueMonitorRow>[] = [
    { header: "Department", cell: (r) => r.departmentName },
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    { header: "Now Serving", cell: (r) => r.nowServing ?? "-" },
    { header: "Waiting", cell: (r) => r.waiting },
    { header: "Completed", cell: (r) => r.completed },
    {
      header: "Avg. Waiting Time",
      cell: (r) => (r.avgWaitingMinutes != null ? `${r.avgWaitingMinutes} min` : "-"),
    },
  ];

  return (
    <StaffLayout title="Queue Monitor">
      <Panel>
        <div className="mb-5 flex flex-wrap items-end gap-3 sm:gap-4">
          <div className="w-full sm:w-56">
            <Field label="Department">
              <select
                className={inputClass}
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              >
                <option value="ALL">All Departments</option>
                {departments.map((d) => (
                  <option key={d.id}>{d.name}</option>
                ))}
              </select>
            </Field>
          </div>
          <div className="w-full sm:w-56">
            <Field label="Doctor">
              <select
                className={inputClass}
                value={doctor}
                onChange={(e) => setDoctor(e.target.value)}
              >
                <option value="ALL">All Doctors</option>
                {doctors.map((d) => (
                  <option key={d.id}>{d.name}</option>
                ))}
              </select>
            </Field>
          </div>
          <button
            onClick={load}
            className="w-full sm:ml-auto sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
          >
            <RefreshCw className="size-4" /> Refresh
          </button>
        </div>

        {loading ? (
          <SkeletonTable rows={5} columns={8} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            numbered={false}
            pageSize={10}
          />
        )}

        <div className="mt-4 flex flex-wrap gap-5 text-xs text-muted-foreground">
          <Legend color="bg-success" label="Consulting" />
          <Legend color="bg-warning" label="On Break" />
          <Legend color="bg-danger" label="Unavailable" />
        </div>
      </Panel>
    </StaffLayout>
  );
}

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-2">
      <span className={`size-2.5 rounded-full ${color}`} /> {label}
    </span>
  );
}
