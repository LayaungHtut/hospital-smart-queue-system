import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AlertTriangle, Info, ShieldAlert, Timer } from "lucide-react";
import { confirmEmergency, getEmergencyCases, rejectEmergency } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { DataTable, Panel, StatusBadge, TabNav, type Column } from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { EmergencyCase } from "@/types";

export const Route = createFileRoute("/staff/emergency")({
  head: () => ({
    meta: [
      { title: "Emergency Cases — Staff Portal" },
      { name: "description", content: "Confirm or reject emergency queue requests from patients." },
      { property: "og:title", content: "Emergency Cases — Staff Portal" },
      { property: "og:description", content: "Confirm or reject emergency queue requests." },
    ],
  }),
  component: EmergencyPage,
});

function EmergencyPage() {
  const [cases, setCases] = useState<EmergencyCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState("pending");

  useEffect(() => {
    getEmergencyCases()
      .then((data) => {
        setCases(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function handleConfirm(id: number) {
    const updated = await confirmEmergency(id);
    setCases((prev) => prev.map((c) => (c.id === id ? updated : c)));
  }

  async function handleReject(id: number) {
    await rejectEmergency(id);
    setCases((prev) => prev.filter((c) => c.id !== id));
  }

  const pending = cases.filter((c) => !c.resolved);
  const resolved = cases.filter((c) => c.resolved);
  const critical = pending.filter((c) => c.priority === "HIGH");
  const fastTrack = pending.filter((c) => c.priority !== "HIGH");

  const pendingColumns: Column<EmergencyCase>[] = [
    {
      header: "Patient ID",
      cell: (r) => <span className="font-medium text-primary">{r.patientCode}</span>,
    },
    { header: "Patient Name", cell: (r) => r.patientName },
    { header: "Symptoms", cell: (r) => r.symptoms },
    { header: "Requested Time", cell: (r) => r.requestedTime },
    { header: "Priority", cell: (r) => <StatusBadge status={r.priority} /> },
    {
      header: "Action",
      cell: (r) => (
        <div className="flex gap-2">
          <button
            onClick={() => handleConfirm(r.id)}
            className="rounded-md bg-success-soft px-3 py-1 text-xs font-medium text-success"
          >
            Confirm
          </button>
          <button
            onClick={() => handleReject(r.id)}
            className="rounded-md bg-danger-soft px-3 py-1 text-xs font-medium text-danger"
          >
            Reject
          </button>
        </div>
      ),
    },
  ];

  const resolvedColumns: Column<EmergencyCase>[] = [
    { header: "Patient ID", cell: (r) => r.patientCode },
    { header: "Patient Name", cell: (r) => r.patientName },
    { header: "Confirmed Time", cell: (r) => r.confirmedTime ?? "—" },
    { header: "Handled By", cell: (r) => r.handledBy ?? "—" },
    { header: "Priority", cell: (r) => <StatusBadge status={r.priority} /> },
  ];

  return (
    <StaffLayout title="Emergency Cases">
      <div className="space-y-6">
        {/* Critical metrics & rapid protocol banner */}
        <div className="grid gap-4 xl:grid-cols-12">
          <div className="relative overflow-hidden rounded-xl border border-danger/30 bg-danger-soft p-5 xl:col-span-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <ShieldAlert className="size-6 animate-pulse text-danger" />
                <div>
                  <span className="block text-[10px] font-bold uppercase tracking-wider text-danger">
                    Red Triage Protocol
                  </span>
                  <h2 className="text-base font-bold text-foreground">High Priority Cases</h2>
                </div>
              </div>
              {critical.length > 0 ? (
                <span className="animate-bounce rounded-full bg-danger px-2 py-0.5 text-[10px] font-bold text-danger-foreground">
                  ACT NOW
                </span>
              ) : null}
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <div className="rounded-lg border border-border bg-card p-3 shadow-sm">
                <span className="text-2xl font-extrabold text-danger">{critical.length}</span>
                <p className="text-xs font-semibold text-foreground">Critical Cases</p>
                <p className="text-[11px] font-medium text-danger">Immediate attention</p>
              </div>
              <div className="rounded-lg border border-border bg-card p-3 shadow-sm">
                <span className="text-2xl font-extrabold text-warning">{fastTrack.length}</span>
                <p className="text-xs font-semibold text-foreground">Fast-Track Pending</p>
                <p className="text-[11px] font-medium text-warning">Priority queue</p>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-border bg-card p-5 xl:col-span-8">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3">
              <div className="flex items-center gap-2">
                <AlertTriangle className="size-5 text-primary" />
                <span className="text-base font-bold text-foreground">Rapid Triage Directive</span>
              </div>
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Timer className="size-3.5" />
                Target Door-to-Doctor: <strong className="text-foreground">&lt; 10 mins</strong>
              </div>
            </div>
            <div className="grid gap-3 border-t border-border pt-3 sm:grid-cols-3">
              <div className="flex items-start gap-2 rounded-lg bg-muted/50 p-3">
                <AlertTriangle className="mt-0.5 size-4 shrink-0 text-danger" />
                <div>
                  <p className="text-xs font-bold text-foreground">Acute chest pain / cardiac</p>
                  <p className="text-[11px] text-muted-foreground">
                    Route straight to on-call doctor.
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-2 rounded-lg bg-muted/50 p-3">
                <AlertTriangle className="mt-0.5 size-4 shrink-0 text-warning" />
                <div>
                  <p className="text-xs font-bold text-foreground">Breathing difficulty</p>
                  <p className="text-[11px] text-muted-foreground">
                    Fast-track ahead of the queue.
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-2 rounded-lg bg-muted/50 p-3">
                <Info className="mt-0.5 size-4 shrink-0 text-info" />
                <div>
                  <p className="text-xs font-bold text-foreground">Everything else</p>
                  <p className="text-[11px] text-muted-foreground">
                    Confirm case, assign standard priority.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <Panel>
          <TabNav
            value={tab}
            onChange={setTab}
            tabs={[
              { value: "pending", label: `Pending (${pending.length})` },
              { value: "resolved", label: "Resolved" },
            ]}
          />
          {tab === "pending" ? (
            loading ? (
              <SkeletonTable rows={5} columns={6} />
            ) : (
              <DataTable
                rows={pending}
                columns={pendingColumns}
                loading={loading}
                emptyMessage="No pending emergency cases."
              />
            )
          ) : loading ? (
            <SkeletonTable rows={5} columns={5} />
          ) : (
            <DataTable
              rows={resolved}
              columns={resolvedColumns}
              loading={loading}
              emptyMessage="No confirmed emergency yet."
            />
          )}
        </Panel>

        <div className="grid gap-6 lg:grid-cols-2">
          <Panel>
            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
              <Info className="size-4 text-info" /> What to do?
            </h3>
            <ol className="list-decimal space-y-1.5 pl-5 text-sm text-muted-foreground">
              <li>Check the patient's symptoms.</li>
              <li>Confirm if it is a real emergency.</li>
              <li>If yes, set priority to Emergency.</li>
              <li>If no, continue as normal queue.</li>
            </ol>
          </Panel>
          <Panel title="Confirmed Emergency">
            {loading ? (
              <SkeletonTable rows={3} columns={5} />
            ) : (
              <DataTable
                rows={resolved}
                columns={resolvedColumns}
                loading={loading}
                emptyMessage="No confirmed emergency yet."
                pageSize={5}
              />
            )}
          </Panel>
        </div>
      </div>
    </StaffLayout>
  );
}
