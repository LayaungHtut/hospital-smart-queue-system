import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/portal/shells";
import { Panel, StatCard, Field, StatusBadge, inputClass } from "@/components/portal/ui-kit";
import { SkeletonStatCard } from "@/components/ui/loading";
import {
  getDepartments,
  getSmartDoctorAssignment,
  type DoctorRecommendation,
} from "@/services/api";
import type { Department } from "@/types";

export const Route = createFileRoute("/admin/ai-smart-assignment")({
  head: () => ({ meta: [{ title: "AI Smart Doctor Assignment" }] }),
  component: SmartDoctorAssignmentPage,
});

function SmartDoctorAssignmentPage() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [selectedDept, setSelectedDept] = useState("");
  const [symptoms, setSymptoms] = useState("");
  const [recommendations, setRecommendations] = useState<DoctorRecommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    getDepartments()
      .then((depts) => {
        setDepartments(depts);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function search() {
    if (!selectedDept) return;
    setSearching(true);
    const result = await getSmartDoctorAssignment(selectedDept, symptoms || undefined);
    setRecommendations(result.recommendations);
    setSearching(false);
  }

  return (
    <AdminLayout title="AI Smart Doctor Assignment">
      <div className="space-y-6">
        <Panel title="Find the Best Doctor">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <Field label="Department">
              <select
                className={inputClass}
                value={selectedDept}
                onChange={(e) => setSelectedDept(e.target.value)}
              >
                <option value="">Select Department</option>
                {departments.map((d) => (
                  <option key={d.departmentCode} value={d.departmentCode}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Symptoms (optional)">
              <input
                className={inputClass}
                placeholder="e.g. chest pain, headache, fever"
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && search()}
              />
            </Field>
            <div className="flex items-end">
              <button
                onClick={search}
                disabled={!selectedDept || searching}
                className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {searching ? "Searching..." : "Find Best Doctor"}
              </button>
            </div>
          </div>
        </Panel>

        {recommendations.length > 0 &&
          (() => {
            const top = recommendations[0]!;
            return (
              <>
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                  <StatCard label="Top Recommendation" value={top.doctorName} tone="success" />
                  <StatCard label="Best Score" value={`${top.score.toFixed(1)}%`} tone="primary" />
                  <StatCard
                    label="Estimated Wait"
                    value={`${top.estimatedWaitMinutes} min`}
                    tone={top.estimatedWaitMinutes <= 15 ? "success" : "warning"}
                  />
                  <StatCard label="Doctors Ranked" value={recommendations.length} tone="primary" />
                </div>

                <Panel title="Ranked Recommendations">
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-border text-left text-muted-foreground">
                          <th className="pb-3 font-medium">Rank</th>
                          <th className="pb-3 font-medium">Doctor</th>
                          <th className="pb-3 font-medium">Score</th>
                          <th className="pb-3 font-medium">Specialization</th>
                          <th className="pb-3 font-medium">Queue Load</th>
                          <th className="pb-3 font-medium">Availability</th>
                          <th className="pb-3 font-medium">Wait Time</th>
                          <th className="pb-3 font-medium">Reason</th>
                        </tr>
                      </thead>
                      <tbody>
                        {recommendations.map((rec) => (
                          <tr
                            key={rec.doctorId}
                            className="border-b border-border/50 hover:bg-accent/50"
                          >
                            <td className="py-3">
                              <span
                                className={`inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                                  rec.rank === 1
                                    ? "bg-success text-success-foreground"
                                    : rec.rank === 2
                                      ? "bg-primary text-primary-foreground"
                                      : "bg-muted text-muted-foreground"
                                }`}
                              >
                                {rec.rank}
                              </span>
                            </td>
                            <td className="py-3 font-medium">{rec.doctorName}</td>
                            <td className="py-3">
                              <div className="flex items-center gap-2">
                                <div className="h-2 w-24 rounded-full bg-muted">
                                  <div
                                    className="h-full rounded-full bg-primary"
                                    style={{ width: `${rec.score}%` }}
                                  />
                                </div>
                                <span className="text-xs">{rec.score.toFixed(1)}</span>
                              </div>
                            </td>
                            <td className="py-3">
                              <StatusBadge
                                status={
                                  rec.specializationScore >= 80
                                    ? "HIGH"
                                    : rec.specializationScore >= 50
                                      ? "MEDIUM"
                                      : "LOW"
                                }
                              />
                            </td>
                            <td className="py-3">
                              <StatusBadge
                                status={
                                  rec.queueLoadScore >= 70
                                    ? "COMPLETED"
                                    : rec.queueLoadScore >= 40
                                      ? "WAITING"
                                      : "CANCELLED"
                                }
                              />
                            </td>
                            <td className="py-3">
                              <StatusBadge
                                status={
                                  rec.availabilityScore >= 90
                                    ? "ACTIVE"
                                    : rec.availabilityScore >= 50
                                      ? "WAITING"
                                      : "INACTIVE"
                                }
                              />
                            </td>
                            <td className="py-3">{rec.estimatedWaitMinutes} min</td>
                            <td className="max-w-xs truncate text-xs text-muted-foreground">
                              {rec.reason}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Panel>
              </>
            );
          })()}

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
