import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Download, Printer } from "lucide-react";
import { getReport } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import {
  DataTable,
  Field,
  Panel,
  StatCard,
  inputClass,
  type Column,
} from "@/components/portal/ui-kit";
import { SkeletonStatCard, SkeletonTable } from "@/components/ui/loading";
import type { DailyReport, ReportRow } from "@/types";

export const Route = createFileRoute("/admin/reports")({
  head: () => ({
    meta: [
      { title: "Reports & Analytics — Admin Portal" },
      {
        name: "description",
        content: "Generate hospital appointment, queue, and doctor performance reports.",
      },
      { property: "og:title", content: "Reports & Analytics — Admin Portal" },
      {
        property: "og:description",
        content: "Generate hospital queue reports with CSV and PDF export.",
      },
    ],
  }),
  component: AdminReportsPage,
});

function AdminReportsPage() {
  const [type, setType] = useState("APPOINTMENT");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [report, setReport] = useState<DailyReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getReport(type ?? "APPOINTMENT", date ?? new Date().toISOString().split("T")[0])
      .then((data) => {
        setReport(data);
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function generate() {
    setLoading(true);
    try {
      const data = await getReport(type, date);
      setReport(data);
    } catch (err) {
      console.error("Failed to generate report:", err);
    } finally {
      setLoading(false);
    }
  }

  function exportCSV() {
    if (!report || !report.rows || report.rows.length === 0) {
      alert("No data available to export.");
      return;
    }

    let headers: string[] = [];
    let rowsData: (string | number)[][] = [];

    if (type === "APPOINTMENT") {
      headers = [
        "Appointment ID",
        "Patient ID",
        "Patient Name",
        "Doctor Name",
        "Department",
        "Date",
        "Time Slot",
        "Status",
      ];
      rowsData = report.rows.map((r) => [
        r.appointmentId || "",
        r.patientId || "",
        `"${r.patientName || ""}"`,
        `"${r.doctorName || ""}"`,
        `"${r.department || ""}"`,
        `"${r.date || date}"`,
        `"${r.timeSlot || ""}"`,
        r.status || "CONFIRMED",
      ]);
    } else if (type === "QUEUE") {
      headers = [
        "Queue Number",
        "Patient Name",
        "Doctor Name",
        "Department",
        "Waiting Time",
        "Serving Time",
        "Status",
      ];
      rowsData = report.rows.map((r) => [
        r.queueNumber || "",
        `"${r.patientName || ""}"`,
        `"${r.doctorName || ""}"`,
        `"${r.department || ""}"`,
        `"${r.waitingTime || "15 min"}"`,
        `"${r.servingTime || "15 min"}"`,
        r.status || "WAITING",
      ]);
    } else if (type === "DOCTOR_PERFORMANCE") {
      headers = [
        "Doctor Code",
        "Doctor Name",
        "Department",
        "Qualification",
        "Experience (Years)",
        "Total Served",
        "Current Waiting",
        "Avg Consultation (Min)",
        "Status",
      ];
      rowsData = report.rows.map((r) => [
        r.doctorCode || "",
        `"${r.doctorName || ""}"`,
        `"${r.department || ""}"`,
        `"${r.qualification || ""}"`,
        r.experienceYears ?? 5,
        r.totalServed ?? 0,
        r.currentWaiting ?? 0,
        r.avgConsultationMinutes ?? 15,
        r.status || "ACTIVE",
      ]);
    } else if (type === "EMERGENCY") {
      headers = [
        "Queue Number",
        "Patient ID",
        "Patient Name",
        "Department",
        "Assigned Doctor",
        "Registration Time",
        "Triage Confirmation",
        "Status",
      ];
      rowsData = report.rows.map((r) => [
        r.queueNumber || "",
        r.patientId || "",
        `"${r.patientName || ""}"`,
        `"${r.department || ""}"`,
        `"${r.assignedDoctor || ""}"`,
        `"${r.time || ""}"`,
        r.confirmed || "CONFIRMED",
        r.status || "WAITING",
      ]);
    } else {
      headers = [
        "Department",
        "Total Queues",
        "Completed",
        "Cancelled",
        "Missed",
        "Avg Waiting Time (Min)",
      ];
      rowsData = report.rows.map((r) => [
        `"${r.department || ""}"`,
        r.totalQueues ?? 0,
        r.completed ?? 0,
        r.cancelled ?? 0,
        r.missed ?? 0,
        r.avgWaitingMinutes ?? 0,
      ]);
    }

    const csvContent =
      "data:text/csv;charset=utf-8," +
      [headers.join(","), ...rowsData.map((e) => e.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Hospital_Report_${type}_${date}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  function exportPDF() {
    window.print();
  }

  const appointmentColumns: Column<ReportRow>[] = [
    {
      header: "Appt ID",
      cell: (r) => <span className="font-mono text-primary font-medium">{r.appointmentId}</span>,
    },
    {
      header: "Patient ID",
      cell: (r) => <span className="font-mono text-muted-foreground">{r.patientId}</span>,
    },
    {
      header: "Patient Name",
      cell: (r) => <span className="font-semibold text-foreground">{r.patientName}</span>,
    },
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Department", cell: (r) => r.department },
    { header: "Date", cell: (r) => r.date || date },
    { header: "Time Slot", cell: (r) => <span className="font-medium">{r.timeSlot}</span> },
    {
      header: "Status",
      cell: (r) => (
        <span
          className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${r.status === "COMPLETED" ? "bg-success/15 text-success" : "bg-primary/15 text-primary"}`}
        >
          {r.status}
        </span>
      ),
    },
  ];

  const queueColumns: Column<ReportRow>[] = [
    {
      header: "Queue #",
      cell: (r) => <span className="font-mono font-bold text-primary">{r.queueNumber}</span>,
    },
    { header: "Patient Name", cell: (r) => <span className="font-semibold">{r.patientName}</span> },
    { header: "Doctor", cell: (r) => r.doctorName },
    { header: "Department", cell: (r) => r.department },
    {
      header: "Waiting Time",
      cell: (r) => (
        <span className="font-medium text-amber-600 dark:text-amber-400">{r.waitingTime}</span>
      ),
    },
    { header: "Serving Time", cell: (r) => r.servingTime || "15 min" },
    {
      header: "Status",
      cell: (r) => (
        <span
          className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${r.status === "COMPLETED" ? "bg-success/15 text-success" : r.status === "CANCELLED" ? "bg-danger/15 text-danger" : "bg-primary/15 text-primary"}`}
        >
          {r.status}
        </span>
      ),
    },
  ];

  const doctorColumns: Column<ReportRow>[] = [
    {
      header: "Doctor Code",
      cell: (r) => <span className="font-mono text-primary font-medium">{r.doctorCode}</span>,
    },
    {
      header: "Doctor Name",
      cell: (r) => <span className="font-semibold text-foreground">{r.doctorName}</span>,
    },
    { header: "Degree", cell: (r) => r.qualification || "MBBS, M.Med.Sc" },
    { header: "Department", cell: (r) => r.department },
    { header: "Experience", cell: (r) => `${r.experienceYears || 5} yrs` },
    {
      header: "Completed Visits",
      cell: (r) => <span className="text-success font-bold">{r.totalServed}</span>,
    },
    { header: "Active Queue", cell: (r) => r.currentWaiting },
    { header: "Avg. Consult", cell: (r) => `${r.avgConsultationMinutes} min` },
    {
      header: "Status",
      cell: (r) => (
        <span className="rounded-full px-2.5 py-0.5 text-xs font-semibold bg-success/15 text-success">
          {r.status}
        </span>
      ),
    },
  ];

  const emergencyColumns: Column<ReportRow>[] = [
    {
      header: "Queue #",
      cell: (r) => <span className="font-bold text-danger">{r.queueNumber}</span>,
    },
    { header: "Patient ID", cell: (r) => <span className="font-mono">{r.patientId}</span> },
    { header: "Patient Name", cell: (r) => <span className="font-semibold">{r.patientName}</span> },
    { header: "Department", cell: (r) => r.department },
    { header: "Assigned Doctor", cell: (r) => r.assignedDoctor },
    { header: "Time", cell: (r) => r.time },
    {
      header: "Triage Status",
      cell: (r) => (
        <span className="rounded bg-danger/15 px-2 py-0.5 text-[11px] font-bold text-danger">
          {r.confirmed}
        </span>
      ),
    },
    {
      header: "Status",
      cell: (r) => <span className="font-medium text-xs uppercase">{r.status}</span>,
    },
  ];

  const dailyColumns: Column<ReportRow>[] = [
    { header: "Department", cell: (r) => <span className="font-semibold">{r.department}</span> },
    { header: "Total Queues", cell: (r) => r.totalQueues },
    {
      header: "Completed",
      cell: (r) => <span className="text-success font-medium">{r.completed}</span>,
    },
    {
      header: "Cancelled",
      cell: (r) => <span className="text-danger font-medium">{r.cancelled}</span>,
    },
    {
      header: "Missed",
      cell: (r) => <span className="text-amber-500 font-medium">{r.missed}</span>,
    },
    { header: "Avg. Waiting Time", cell: (r) => `${r.avgWaitingMinutes} min` },
  ];

  let activeColumns = appointmentColumns;
  if (type === "QUEUE") activeColumns = queueColumns;
  else if (type === "DOCTOR_PERFORMANCE") activeColumns = doctorColumns;
  else if (type === "EMERGENCY") activeColumns = emergencyColumns;
  else if (type === "DAILY_QUEUE") activeColumns = dailyColumns;

  const reportTitle =
    type === "APPOINTMENT"
      ? "Patient Appointments Report"
      : type === "QUEUE"
        ? "Live Patient Queue Report"
        : type === "DOCTOR_PERFORMANCE"
          ? "Doctor Performance & Workload Report"
          : type === "EMERGENCY"
            ? "Emergency Triage Audit Report"
            : "Daily Department Report";

  return (
    <AdminLayout title="Reports & Analytics">
      <div className="space-y-6">
        <Panel>
          <div className="flex flex-wrap items-end gap-3 sm:gap-4">
            <div className="w-full sm:w-60">
              <Field label="Report Type">
                <select
                  className={inputClass}
                  value={type}
                  onChange={(e) => setType(e.target.value)}
                >
                  <option value="APPOINTMENT">Appointment Report</option>
                  <option value="QUEUE">Queue Report</option>
                  <option value="DOCTOR_PERFORMANCE">Doctor Performance Report</option>
                  <option value="DAILY_QUEUE">Daily Department Report</option>
                  <option value="EMERGENCY">Emergency Triage Report</option>
                </select>
              </Field>
            </div>
            <div className="w-full sm:w-48">
              <Field label="Date">
                <input
                  type="date"
                  className={inputClass}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </Field>
            </div>
            <button
              onClick={generate}
              className="w-full rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 shadow-sm sm:w-auto"
            >
              Generate Report
            </button>
            <div className="flex w-full items-center gap-2 sm:ml-auto sm:w-auto">
              <button
                onClick={exportCSV}
                className="inline-flex flex-1 items-center justify-center gap-2 rounded-lg border border-border bg-card px-3 py-2.5 text-xs font-semibold text-foreground hover:bg-muted shadow-sm sm:flex-none sm:px-4 sm:text-sm"
              >
                <Download className="size-4 text-primary" />{" "}
                <span className="hidden sm:inline">Export Excel/CSV</span>
                <span className="sm:hidden">Export CSV</span>
              </button>
              <button
                onClick={exportPDF}
                className="inline-flex flex-1 items-center justify-center gap-2 rounded-lg border border-border bg-card px-3 py-2.5 text-xs font-semibold text-foreground hover:bg-muted shadow-sm sm:flex-none sm:px-4 sm:text-sm"
              >
                <Printer className="size-4 text-primary" />{" "}
                <span className="hidden sm:inline">Print / PDF</span>
                <span className="sm:hidden">Print</span>
              </button>
            </div>
          </div>
        </Panel>

        {loading && !report ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
            </div>
            <Panel title="Report Result">
              <SkeletonTable rows={5} columns={7} />
            </Panel>
          </>
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Total Records" value={report?.totalQueues ?? 0} />
              <StatCard label="Completed / Served" value={report?.completed ?? 0} tone="success" />
              <StatCard label="Cancelled" value={report?.cancelled ?? 0} tone="danger" />
              <StatCard label="Missed / No-Show" value={report?.missed ?? 0} tone="warning" />
            </div>

            <Panel title={`${reportTitle} — ${report?.date ?? date}`}>
              <DataTable
                rows={report?.rows ?? []}
                columns={activeColumns}
                loading={loading}
                numbered={false}
                pageSize={10}
              />
            </Panel>
          </>
        )}
      </div>
    </AdminLayout>
  );
}
