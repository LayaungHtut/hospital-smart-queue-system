import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState, useCallback } from "react";
import { Activity, CheckCircle2, Pause, Play, RotateCcw, Sparkles, Timer, Volume2 } from "lucide-react";
import {
  doctorCallNext,
  doctorCompleteConsultation,
  doctorPauseConsultation,
  doctorResumeConsultation,
  doctorStartConsultation,
  getDoctorDashboard,
  getDoctorProfile,
} from "@/services/api";
import { DoctorLayout } from "@/components/portal/shells";
import {
  DataTable,
  Panel,
  QueueTokenBadge,
  StatusBadge,
  type Column,
} from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonTable, SkeletonPanel } from "@/components/ui/loading";
import type { DoctorDashboard, QueueEntry } from "@/types";

export const Route = createFileRoute("/doctor/queue")({
  head: () => ({
    meta: [
      { title: "Consultation Queue — Doctor Portal" },
      { name: "description", content: "Interactive consultation room and live queue caller." },
      { property: "og:title", content: "Consultation Queue — Doctor Portal" },
      { property: "og:description", content: "Interactive consultation room." },
    ],
  }),
  component: DoctorQueueRoomPage,
});

function DoctorQueueRoomPage() {
  const { session } = useAuth();
  const [data, setData] = useState<DoctorDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [doctorId, setDoctorId] = useState<string | number>(
    session?.doctorId ?? session?.userId ?? "D001",
  );
  const [now, setNow] = useState(() => Date.now());

  // Tick every second so the "call expires in" countdown stays live.
  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  // Sync doctorId from session
  useEffect(() => {
    const currentDocId = session?.doctorId ?? session?.userId;
    if (currentDocId && currentDocId !== doctorId) {
      setDoctorId(currentDocId);
    } else if (!session?.doctorId && session?.name && session?.role === "DOCTOR") {
      getDoctorProfile(session.name)
        .then((profile) => {
          if (profile?.id) {
            setDoctorId(profile.id);
          }
        })
        .catch(console.error);
    }
  }, [session, doctorId]);

  const loadData = useCallback(async () => {
    try {
      const res = await getDoctorDashboard(doctorId);
      setData(res);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [doctorId]);

  useEffect(() => {
    loadData();
    const timer = setInterval(loadData, 8000);
    return () => clearInterval(timer);
  }, [loadData]);

  async function handleCallNext() {
    setActionLoading(true);
    try {
      const res = await doctorCallNext(doctorId);
      setMessage(res.message);
      await loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to call next patient");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleStart() {
    setActionLoading(true);
    try {
      const res = await doctorStartConsultation(doctorId);
      setMessage(res.message);
      await loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to start consultation");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleComplete() {
    setActionLoading(true);
    try {
      const res = await doctorCompleteConsultation(doctorId);
      setMessage(res.message);
      await loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to complete consultation");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  async function handlePause() {
    setActionLoading(true);
    try {
      const res = await doctorPauseConsultation(doctorId);
      setMessage(res.message);
      await loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to pause consultation");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleResume() {
    setActionLoading(true);
    try {
      const res = await doctorResumeConsultation(doctorId);
      setMessage(res.message);
      await loadData();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to resume consultation");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  const columns: Column<QueueEntry>[] = [
    {
      header: "Queue No.",
      cell: (r) => <span className="font-bold text-primary">{r.queueNumber}</span>,
    },
    { header: "Patient Name", cell: (r) => <span className="font-medium">{r.patientName}</span> },
    {
      header: "Type",
      cell: (r) => (
        <div className="flex items-center gap-1.5">
          <StatusBadge status={r.type ?? "NORMAL"} />
          {r.emergency && (
            <span className="rounded bg-danger/20 px-1.5 py-0.5 text-[10px] font-bold text-danger">
              EMERGENCY
            </span>
          )}
        </div>
      ),
    },
    { header: "Line Position", cell: (r) => `#${r.position}` },
    { header: "Estimated Wait", cell: (r) => `${r.estimatedWaitingMinutes ?? 15} min` },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
  ];

  const activePatient = data?.serving ?? data?.called;
  const isServing = !!data?.serving;
  const isCalled = !!data?.called && !data?.serving;

  const calledExpiryMinutes = data?.calledExpiryMinutes ?? 5;
  const calledDeadline =
    isCalled && data?.called?.calledAt
      ? new Date(data.called.calledAt).getTime() + calledExpiryMinutes * 60_000
      : null;
  const remainingMs = calledDeadline !== null ? calledDeadline - now : null;

  if (loading && !data) {
    return (
      <DoctorLayout title="Consultation Queue">
        <div className="space-y-6">
          <SkeletonCard className="rounded-2xl p-6" />
          <SkeletonPanel title />
        </div>
      </DoctorLayout>
    );
  }

  return (
    <DoctorLayout title="Consultation Queue">
      <div className="space-y-6">
        {/* Active Consultation Console */}
        <div className="rounded-2xl border border-primary/30 bg-card p-4 shadow-sm sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:border-b sm:border-border sm:pb-4">
            <div className="flex items-center gap-3">
              <div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm sm:size-10">
                <Activity className="size-5 sm:size-6" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-foreground sm:text-xl">
                  Consultation Console
                </h2>
                <p className="text-xs text-muted-foreground">
                  Call, serve, pause, and complete patient queues
                </p>
              </div>
            </div>
            <button
              onClick={handleCallNext}
              disabled={actionLoading || isServing || isCalled}
              className="flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-primary-foreground shadow hover:opacity-90 disabled:opacity-50 sm:px-5"
            >
              <Volume2 className="size-4" />
              Call Next Patient
            </button>
          </div>

          {message && (
            <div className="mt-4 flex items-center gap-2 rounded-lg bg-primary/10 px-4 py-2 text-sm font-medium text-primary">
              <Sparkles className="size-4 shrink-0" />
              {message}
            </div>
          )}

          <div className="mt-6">
            {activePatient ? (
              <div className="flex flex-col gap-4 rounded-xl border border-border bg-accent/30 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-6">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <QueueTokenBadge
                    token={activePatient.queueNumber}
                    station={isServing ? "In Consultation" : "Called · Awaiting Arrival"}
                    urgency={activePatient.emergency ? "critical" : "general"}
                  />
                  <div>
                    <span className="text-lg font-bold text-foreground sm:text-xl">
                      {activePatient.patientName}
                    </span>
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                      <StatusBadge status={activePatient.type ?? "NORMAL"} />
                      {activePatient.emergency && (
                        <span className="rounded bg-danger/20 px-2 py-0.5 font-bold text-danger">
                          EMERGENCY
                        </span>
                      )}
                      <span>Waiting: {activePatient.estimatedWaitingMinutes ?? 15} min</span>
                      {isCalled && remainingMs !== null && (
                        <span
                          className={`flex items-center gap-1 rounded px-2 py-0.5 font-bold ${
                            remainingMs <= 0
                              ? "bg-danger/20 text-danger"
                              : remainingMs <= 60_000
                                ? "bg-warning/20 text-warning"
                                : "bg-primary/10 text-primary"
                          }`}
                        >
                          <Timer className="size-3.5" />
                          {remainingMs <= 0
                            ? "Call expired — will auto-cancel shortly"
                            : `Auto-expires in ${formatCountdown(remainingMs)}`}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2 sm:gap-3">
                  {isCalled && (
                    <button
                      onClick={handleStart}
                      disabled={actionLoading}
                      className="flex items-center gap-2 rounded-xl bg-success px-4 py-2 text-xs font-bold text-success-foreground hover:opacity-90 shadow-sm sm:px-5 sm:py-2.5 sm:text-sm"
                    >
                      <Play className="size-4 fill-current" />
                      Start Consultation
                    </button>
                  )}

                  {isServing && (
                    <>
                      <button
                        onClick={handleComplete}
                        disabled={actionLoading}
                        className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:opacity-90 shadow-sm sm:px-5 sm:py-2.5 sm:text-sm"
                      >
                        <CheckCircle2 className="size-4" />
                        Complete
                      </button>
                      <button
                        onClick={handlePause}
                        disabled={actionLoading}
                        className="flex items-center gap-2 rounded-xl bg-secondary px-3 py-2 text-xs font-semibold text-secondary-foreground hover:bg-secondary/80 sm:px-4 sm:py-2.5 sm:text-sm"
                      >
                        <Pause className="size-4" />
                        Pause
                      </button>
                    </>
                  )}

                  {activePatient.status === "PAUSED" && (
                    <button
                      onClick={handleResume}
                      disabled={actionLoading}
                      className="flex items-center gap-2 rounded-xl bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 sm:px-4 sm:py-2.5 sm:text-sm"
                    >
                      <RotateCcw className="size-4" />
                      Resume
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <div className="py-8 text-center text-muted-foreground text-sm">
                No patient currently called. Click <strong>"Call Next Patient"</strong> to call the
                next patient in line.
              </div>
            )}
          </div>
        </div>

        {/* Full Waiting Queue */}
        <Panel title={`Waiting Queue (${data?.waiting.length ?? 0} Patients)`}>
          {loading ? (
            <SkeletonTable rows={5} columns={6} />
          ) : (
            <DataTable
              rows={data?.waiting ?? []}
              columns={columns}
              loading={loading}
              emptyMessage="No patients currently waiting in queue."
              pageSize={10}
            />
          )}
        </Panel>
      </div>
    </DoctorLayout>
  );
}

/** Formats a millisecond duration as "m:ss" for the call-expiry countdown. */
function formatCountdown(ms: number): string {
  const totalSeconds = Math.max(0, Math.round(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}
