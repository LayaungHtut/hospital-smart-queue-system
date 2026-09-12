import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/portal/shells";
import { Panel, StatCard, Field, TabNav, inputClass } from "@/components/portal/ui-kit";
import { SkeletonStatCard } from "@/components/ui/loading";
import {
  predictQueueFlow,
  predictQueueFlowByDepartment,
  getPeakHours,
  getStaffingRecommendation,
  getDepartments,
  type FlowPrediction,
  type DepartmentFlowPrediction,
  type PeakHoursReport,
  type StaffingRecommendation,
} from "@/services/api";
import type { Department } from "@/types";

export const Route = createFileRoute("/admin/ai-queue-flow")({
  head: () => ({ meta: [{ title: "AI Queue Flow Prediction" }] }),
  component: QueueFlowPredictionPage,
});

function QueueFlowPredictionPage() {
  const [tab, setTab] = useState("forecast");
  const [hoursAhead, setHoursAhead] = useState(3);
  const [selectedDay, setSelectedDay] = useState("MONDAY");
  const [departments, setDepartments] = useState<Department[]>([]);
  const [flow, setFlow] = useState<FlowPrediction | null>(null);
  const [deptFlow, setDeptFlow] = useState<DepartmentFlowPrediction | null>(null);
  const [peakHours, setPeakHours] = useState<PeakHoursReport | null>(null);
  const [staffing, setStaffing] = useState<StaffingRecommendation | null>(null);
  const [loading, setLoading] = useState(true);
  const [predicting, setPredicting] = useState(false);

  const tabs = [
    { value: "forecast", label: "Arrival Forecast" },
    { value: "departments", label: "By Department" },
    { value: "peak", label: "Peak Hours" },
    { value: "staffing", label: "Staffing" },
  ];

  useEffect(() => {
    getDepartments().then(setDepartments).catch(console.error);
    loadAll();
  }, []);

  async function loadAll() {
    setLoading(true);
    try {
      const [f, df, p, s] = await Promise.all([
        predictQueueFlow(hoursAhead),
        predictQueueFlowByDepartment(hoursAhead),
        getPeakHours(selectedDay),
        getStaffingRecommendation(hoursAhead),
      ]);
      setFlow(f);
      setDeptFlow(df);
      setPeakHours(p);
      setStaffing(s);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function refresh() {
    setPredicting(true);
    await loadAll();
    setPredicting(false);
  }

  const trendColor = (trend: string) =>
    trend === "INCREASING" ? "text-danger" : trend === "DECREASING" ? "text-success" : "text-muted-foreground";

  return (
    <AdminLayout title="AI Queue Flow Prediction">
      <div className="space-y-6">
        <Panel title="Controls">
          <div className="flex flex-wrap items-end gap-4">
            <Field label="Hours Ahead">
              <select
                className={inputClass}
                value={hoursAhead}
                onChange={(e) => setHoursAhead(Number(e.target.value))}
              >
                {[1, 2, 3, 4, 5, 6].map((h) => (
                  <option key={h} value={h}>
                    {h} hours
                  </option>
                ))}
              </select>
            </Field>
            <div className="flex items-end">
              <button
                onClick={refresh}
                disabled={predicting}
                className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {predicting ? "Refreshing..." : "Refresh Predictions"}
              </button>
            </div>
          </div>
        </Panel>

        {staffing && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Predicted Arrivals" value={flow?.totalPredictedArrivals.toFixed(1) ?? "—"} tone="primary" />
            <StatCard label="Trend" value={flow?.trend ?? "—"} tone={flow?.trend === "INCREASING" ? "danger" : flow?.trend === "DECREASING" ? "success" : "primary"} />
            <StatCard label="Recommended Staff" value={staffing.recommendedDoctors} tone="success" />
            <StatCard label="Urgency" value={staffing.urgencyLevel} tone={staffing.urgencyLevel === "HIGH" ? "danger" : staffing.urgencyLevel === "MEDIUM" ? "warning" : "success"} />
          </div>
        )}

        <TabNav tabs={tabs} value={tab} onChange={setTab} />

        {tab === "forecast" && flow && (
          <Panel title="Hourly Arrival Forecast">
            <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-6">
              {flow.hourly.map((h) => (
                <div key={h.hour} className="rounded-xl border border-border p-4 text-center">
                  <p className="text-xs text-muted-foreground">
                    {String(h.hour).padStart(2, "0")}:00
                  </p>
                  <p className="mt-1 text-2xl font-bold">{h.predictedArrivals.toFixed(1)}</p>
                  <p className="text-xs text-muted-foreground">patients</p>
                  <span
                    className={`mt-1 inline-block rounded-full px-2 py-0.5 text-[10px] font-medium ${
                      h.confidence === "HIGH"
                        ? "bg-success-soft text-success"
                        : h.confidence === "MEDIUM"
                        ? "bg-primary/10 text-primary"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {h.confidence}
                  </span>
                </div>
              ))}
            </div>
          </Panel>
        )}

        {tab === "departments" && deptFlow && (
          <Panel title="Predictions by Department">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-muted-foreground">
                    <th className="pb-3 font-medium">Department</th>
                    {Array.from({ length: hoursAhead }, (_, i) => {
                      const now = new Date();
                      now.setHours(now.getHours() + i);
                      return (
                        <th key={i} className="pb-3 text-center font-medium">
                          {String(now.getHours()).padStart(2, "0")}:00
                        </th>
                      );
                    })}
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(deptFlow.byDepartment).map(([code, hourly]) => (
                    <tr key={code} className="border-b border-border/50 hover:bg-accent/50">
                      <td className="py-3 font-medium">{code}</td>
                      {hourly.map((h) => (
                        <td key={h.hour} className="py-3 text-center">
                          <span
                            className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                              h.predictedArrivals >= 2
                                ? "bg-danger-soft text-danger"
                                : h.predictedArrivals >= 1
                                ? "bg-warning-soft text-warning"
                                : "bg-success-soft text-success"
                            }`}
                          >
                            {h.predictedArrivals.toFixed(1)}
                          </span>
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        )}

        {tab === "peak" && peakHours && (
          <Panel title={`Peak Hours Analysis — ${peakHours.dayOfWeek}`}>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-6">
              <StatCard label="Peak Hour" value={peakHours.peakHour} tone="danger" />
              <StatCard label="Off-Peak Hour" value={peakHours.offPeakHour} tone="success" />
              <StatCard
                label="Rush Hours"
                value={peakHours.rushHours.length > 0 ? peakHours.rushHours.map((h) => `${h}:00`).join(", ") : "None"}
                tone="warning"
              />
              <Field label="Analyze Day">
                <select
                  className={inputClass}
                  value={selectedDay}
                  onChange={(e) => {
                    setSelectedDay(e.target.value);
                    getPeakHours(e.target.value).then(setPeakHours).catch(console.error);
                  }}
                >
                  {["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <div className="grid gap-2 sm:grid-cols-6 lg:grid-cols-13">
              {Object.entries(peakHours.hourlyAverages).map(([hour, avg]) => {
                const maxAvg = Math.max(...Object.values(peakHours.hourlyAverages), 1);
                const heightPct = (avg / maxAvg) * 100;
                return (
                  <div key={hour} className="flex flex-col items-center gap-1">
                    <div className="flex h-32 w-full items-end justify-center">
                      <div
                        className={`w-full max-w-[40px] rounded-t ${
                          avg >= 4 ? "bg-danger" : avg >= 2 ? "bg-warning" : "bg-primary/60"
                        }`}
                        style={{ height: `${Math.max(heightPct, 4)}%` }}
                      />
                    </div>
                    <p className="text-[10px] text-muted-foreground">{hour}:00</p>
                    <p className="text-xs font-medium">{avg.toFixed(1)}</p>
                  </div>
                );
              })}
            </div>
          </Panel>
        )}

        {tab === "staffing" && staffing && (
          <Panel title="Staffing Recommendation">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Recommended Doctors" value={staffing.recommendedDoctors} tone="primary" />
              <StatCard label="Patients/Hour" value={staffing.predictedPatientsPerHour.toFixed(1)} tone="warning" />
              <StatCard label="Urgency Level" value={staffing.urgencyLevel} tone={staffing.urgencyLevel === "HIGH" ? "danger" : staffing.urgencyLevel === "MEDIUM" ? "warning" : "success"} />
              <StatCard label="Time Period" value={staffing.timePeriod} tone="primary" />
            </div>
            <div className="mt-4 rounded-xl border border-border p-4">
              <p className="text-sm text-muted-foreground">
                Based on predicted flow of <strong>{staffing.predictedPatientsPerHour.toFixed(1)}</strong> patients/hour
                over the next <strong>{hoursAhead}</strong> hours during the <strong>{staffing.timePeriod}</strong> period.
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Recommended: <strong>{staffing.recommendedDoctors}</strong> active doctors to handle the load efficiently.
              </p>
            </div>
          </Panel>
        )}

        {loading && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
