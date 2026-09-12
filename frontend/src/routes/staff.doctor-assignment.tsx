import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Info, Sparkles, Scale, ArrowRight, CheckCircle2, Bot, AlertCircle } from "lucide-react";
import {
  getDepartments,
  getDoctors,
  getReassignableQueues,
  reassignQueue,
  setDoctorUnavailable,
  getAiLoadBalancerSuggestions,
  applyAiLoadBalancerPlan,
  type LoadBalancerReport,
  type ReassignmentSuggestion,
} from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import {
  DataTable,
  Field,
  Panel,
  StatusBadge,
  TabNav,
  inputClass,
  type Column,
} from "@/components/portal/ui-kit";
import { cn } from "@/lib/utils";
import { SkeletonTable } from "@/components/ui/loading";
import { useAuth } from "@/lib/auth";
import type { Department, DoctorDetail, QueueEntry } from "@/types";

export const Route = createFileRoute("/staff/doctor-assignment")({
  head: () => ({
    meta: [
      { title: "Doctor Assignment — Staff Portal" },
      { name: "description", content: "Reassign waiting patients and mark doctors unavailable." },
      { property: "og:title", content: "Doctor Assignment — Staff Portal" },
      { property: "og:description", content: "Reassign waiting patients between doctors." },
    ],
  }),
  component: DoctorAssignmentPage,
});

function DoctorAssignmentPage() {
  const { session } = useAuth();
  const [tab, setTab] = useState("loadbalancer");
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<DoctorDetail[]>([]);
  const [department, setDepartment] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [reason, setReason] = useState("");
  const [rows, setRows] = useState<QueueEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  // AI Load Balancer state
  const [aiReport, setAiReport] = useState<LoadBalancerReport | null>(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [selectedSuggestions, setSelectedSuggestions] = useState<string[]>([]);
  const [applyingAi, setApplyingAi] = useState(false);

  useEffect(() => {
    Promise.all([getDepartments(), getDoctors()])
      .then(([depts, docs]) => {
        setDepartments(depts);
        setDoctors(docs);
        const firstDept = depts[0]?.name || "Cardiology";
        setDepartment(firstDept);
        loadAiSuggestions(firstDept);
        getReassignableQueues(firstDept)
          .then((data) => {
            setRows(data);
            setLoading(false);
          })
          .catch((err) => {
            console.error(err);
            setLoading(false);
          });
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function loadAiSuggestions(deptName: string) {
    setAiLoading(true);
    try {
      const rep = await getAiLoadBalancerSuggestions(deptName);
      setAiReport(rep);
      setSelectedSuggestions(rep.suggestions?.map((s) => s.queueId) || []);
    } catch (err) {
      console.error(err);
    } finally {
      setAiLoading(false);
    }
  }

  async function search() {
    setLoading(true);
    const data = await getReassignableQueues(department);
    setRows(data);
    setLoading(false);
    loadAiSuggestions(department);
  }

  async function assign(queueId: string | number) {
    if (!doctorId) {
      setMessage("Select a doctor before assigning.");
      return;
    }
    await reassignQueue(queueId, doctorId, reason);
    setRows((prev) => prev.filter((r) => r.id !== queueId));
    setMessage("Queue reassigned. The patient has been notified.");
    loadAiSuggestions(department);
  }

  async function markUnavailable() {
    if (!doctorId) {
      setMessage("Select a doctor first.");
      return;
    }
    await setDoctorUnavailable(doctorId, reason);
    setMessage("Doctor marked as unavailable. Their queue is ready for reassignment.");
    loadAiSuggestions(department);
  }

  async function handleApplyAiPlan() {
    if (selectedSuggestions.length === 0) return;
    setApplyingAi(true);
    try {
      const res = await applyAiLoadBalancerPlan(
        selectedSuggestions,
        session?.userId ? String(session.userId) : "STAFF",
      );
      setMessage(res.message);
      await search();
    } catch (err) {
      console.error(err);
      setMessage("Failed to apply AI load balancing plan.");
    } finally {
      setApplyingAi(false);
    }
  }

  function toggleSuggestion(queueId: string) {
    setSelectedSuggestions((prev) =>
      prev.includes(queueId) ? prev.filter((id) => id !== queueId) : [...prev, queueId],
    );
  }

  const deptDoctors = doctors.filter(
    (d) => !department || d.department.toLowerCase() === department.toLowerCase(),
  );

  const columns: Column<QueueEntry>[] = [
    {
      header: "Queue No.",
      cell: (r) => <span className="font-medium text-primary">{r.queueNumber}</span>,
    },
    { header: "Patient Name", cell: (r) => r.patientName },
    { header: "Current Doctor", cell: (r) => r.doctorName },
    { header: "Waiting Time", cell: (r) => `${r.waitingMinutes} min` },
    { header: "Priority", cell: (r) => <StatusBadge status={r.type} /> },
    {
      header: "Action",
      cell: (r) => (
        <button
          onClick={() => assign(r.id)}
          className="rounded-md bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:opacity-90"
        >
          Assign
        </button>
      ),
    },
  ];

  return (
    <StaffLayout title="Doctor Assignment & Queue Balancing">
      <Panel>
        <TabNav
          value={tab}
          onChange={(newTab) => {
            setTab(newTab);
            if (newTab === "loadbalancer") loadAiSuggestions(department);
          }}
          tabs={[
            { value: "loadbalancer", label: "✨ AI Smart Load Balancer" },
            { value: "reassign", label: "Manual Reassign" },
            { value: "unavailable", label: "Doctor Unavailable" },
          ]}
        />

        {tab === "loadbalancer" ? (
          <div className="space-y-6 pt-2">
            {/* Header banner */}
            <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-sm sm:size-10">
                    <Scale className="size-4 sm:size-5" />
                  </div>
                  <div>
                    <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground sm:text-base">
                      AI Queue Load Balancer
                      <span className="rounded bg-primary/20 px-1.5 py-0.5 text-[9px] font-bold text-primary sm:px-2 sm:text-[10px]">
                        FREE AI
                      </span>
                    </h3>
                    <p className="mt-0.5 text-xs text-muted-foreground sm:text-sm">
                      Detects congested vs. idle doctors and proposes balanced reassignments.
                    </p>
                  </div>
                </div>

                <div className="flex w-full items-center gap-2 sm:w-auto">
                  <select
                    className={cn(inputClass, "flex-1 text-xs sm:w-48")}
                    value={department}
                    onChange={(e) => {
                      setDepartment(e.target.value);
                      loadAiSuggestions(e.target.value);
                    }}
                  >
                    <option value="">All Departments</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.name}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => loadAiSuggestions(department)}
                    disabled={aiLoading}
                    className="inline-flex shrink-0 items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-xs font-bold text-primary-foreground shadow hover:opacity-90 disabled:opacity-50 sm:px-4"
                  >
                    {aiLoading ? <Bot className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                    <span className="hidden sm:inline">Scan & Re-analyze</span><span className="sm:hidden">Scan</span>
                  </button>
                </div>
              </div>

              {/* Status summary */}
              {aiReport && (
                <div className="mt-4 grid gap-3 sm:grid-cols-3 border-t border-border/60 pt-4">
                  <div className="rounded-lg bg-card p-3 border border-border">
                    <div className="text-[11px] text-muted-foreground">Active Doctors Online</div>
                    <div className="text-lg font-bold text-foreground">
                      {aiReport.activeDoctorsCount} Doctors
                    </div>
                  </div>
                  <div className="rounded-lg bg-card p-3 border border-border">
                    <div className="text-[11px] text-muted-foreground">Waiting Patients</div>
                    <div className="text-lg font-bold text-foreground">
                      {aiReport.totalWaitingPatients} Patients
                    </div>
                  </div>
                  <div className="rounded-lg bg-card p-3 border border-border">
                    <div className="text-[11px] text-muted-foreground">Queue Load Balance Status</div>
                    <div className="text-sm font-bold flex items-center gap-1.5 mt-0.5">
                      {aiReport.isImbalanced ? (
                        <span className="text-amber-500 flex items-center gap-1">
                          <AlertCircle className="size-4" /> Congestion Imbalance Detected
                        </span>
                      ) : (
                        <span className="text-success flex items-center gap-1">
                          <CheckCircle2 className="size-4" /> Queues Well Balanced
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* AI Recommendations */}
            {aiLoading ? (
              <SkeletonTable rows={4} columns={5} />
            ) : aiReport?.suggestions && aiReport.suggestions.length > 0 ? (
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="text-sm font-bold text-foreground">
                      Proposed Reassignment Plan ({aiReport.suggestions.length} candidates)
                    </h3>
                    <p className="truncate text-xs text-muted-foreground">
                      {aiReport.aiAnalysisSummary}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleApplyAiPlan}
                    disabled={applyingAi || selectedSuggestions.length === 0}
                    className="inline-flex shrink-0 items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground shadow hover:opacity-90 disabled:opacity-50 sm:px-5 sm:py-2.5"
                  >
                    {applyingAi ? <Bot className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
                    Apply ({selectedSuggestions.length})
                  </button>
                </div>

                <div className="overflow-hidden rounded-xl border border-border bg-card">
                  <table className="w-full text-left text-xs">
                    <thead className="border-b border-border bg-accent/40 text-muted-foreground font-semibold">
                      <tr>
                        <th className="p-3 w-10 text-center">
                          <input
                            type="checkbox"
                            checked={selectedSuggestions.length === aiReport.suggestions.length}
                            onChange={(e) =>
                              setSelectedSuggestions(
                                e.target.checked
                                  ? aiReport.suggestions.map((s) => s.queueId)
                                  : [],
                              )
                            }
                          />
                        </th>
                        <th className="p-3">Queue No. & Patient</th>
                        <th className="p-3">Source Doctor (Congested)</th>
                        <th className="p-3">Target Doctor (Available)</th>
                        <th className="p-3">Wait Reduction</th>
                        <th className="p-3">AI Clinical & Queue Rationale</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {aiReport.suggestions.map((s) => {
                        const isChecked = selectedSuggestions.includes(s.queueId);
                        return (
                          <tr
                            key={s.queueId}
                            className={cn(
                              "transition hover:bg-accent/20",
                              isChecked && "bg-primary/5",
                            )}
                          >
                            <td className="p-3 text-center">
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={() => toggleSuggestion(s.queueId)}
                              />
                            </td>
                            <td className="p-3">
                              <div className="font-bold text-primary">{s.queueNumber}</div>
                              <div className="text-foreground">{s.patientName}</div>
                            </td>
                            <td className="p-3">
                              <div className="font-semibold text-foreground">{s.fromDoctorName}</div>
                              <div className="text-[11px] text-muted-foreground">
                                Queue size: {s.fromDoctorQueueSize} waiting
                              </div>
                            </td>
                            <td className="p-3">
                              <div className="font-semibold text-success flex items-center gap-1">
                                <ArrowRight className="size-3 text-primary" /> {s.toDoctorName}
                              </div>
                              <div className="text-[11px] text-muted-foreground">
                                Queue size: {s.toDoctorQueueSize} waiting
                              </div>
                            </td>
                            <td className="p-3">
                              <span className="rounded bg-success/15 px-2 py-0.5 font-bold text-success">
                                -{s.estimatedMinutesSaved} min
                              </span>
                            </td>
                            <td className="p-3 text-muted-foreground max-w-xs leading-relaxed">
                              {s.reason}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-border bg-card p-10 text-center space-y-2">
                <CheckCircle2 className="size-10 text-success mx-auto" />
                <h3 className="text-base font-bold text-foreground">Clinic Queues Balanced</h3>
                <p className="text-xs text-muted-foreground max-w-md mx-auto">
                  {aiReport?.aiAnalysisSummary ||
                    "Doctor queues are currently operating with balanced patient distributions. No rebalancing required."}
                </p>
              </div>
            )}

            {message && (
              <div className="flex items-start gap-2 rounded-lg bg-info-soft px-4 py-3 text-sm text-info">
                <Info className="mt-0.5 size-4 shrink-0" />
                <span>{message}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
            <div className="space-y-4 rounded-lg border border-border p-4">
              <h3 className="text-sm font-semibold text-foreground">Select Doctor</h3>
              <Field label="Department">
                <select
                  className={inputClass}
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                >
                  {departments.map((d) => (
                    <option key={d.id}>{d.name}</option>
                  ))}
                </select>
              </Field>
              <Field label="Select Doctor">
                <select
                  className={inputClass}
                  value={doctorId}
                  onChange={(e) => setDoctorId(e.target.value)}
                >
                  <option value="">-- Select Doctor --</option>
                  {deptDoctors.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Reason (Optional)">
                <textarea
                  className={inputClass}
                  rows={3}
                  placeholder="Enter reason..."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </Field>
              <button
                onClick={tab === "reassign" ? search : markUnavailable}
                className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
              >
                {tab === "reassign" ? "Search Queue" : "Mark Unavailable"}
              </button>
            </div>

            <div>
              <h3 className="mb-3 text-sm font-semibold text-foreground">
                {tab === "reassign" ? "Patients to Reassign" : "Queues awaiting a new doctor"}
              </h3>
              {loading ? (
                <SkeletonTable rows={5} columns={6} />
              ) : (
                <DataTable rows={rows} columns={columns} loading={loading} pageSize={6} />
              )}
              <div className="mt-4 flex items-start gap-2 rounded-lg bg-info-soft px-4 py-3 text-sm text-info">
                <Info className="mt-0.5 size-4 shrink-0" />
                <span>
                  {message ?? "Reassigned patients will receive a notification about the change."}
                </span>
              </div>
            </div>
          </div>
        )}
      </Panel>
    </StaffLayout>
  );
}
