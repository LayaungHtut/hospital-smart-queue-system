import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  BadgeCheck,
  CalendarDays,
  CheckCircle2,
  Clock,
  ListOrdered,
  Sparkles,
  Users,
} from "lucide-react";
import { getPatientAppointments, getPatientNotifications, getPatientQueue } from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import {
  Alert,
  Panel,
  QueueTokenBadge,
  StatCard,
  StatusBadge,
  type AlertTone,
} from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonStatCard, SkeletonPanel } from "@/components/ui/loading";
import type { Appointment, NotificationItem, QueueEntry } from "@/types";
import { ChatbotWidget } from "@/components/portal/ChatbotWidget";

const toneForCategory: Record<NotificationItem["category"], AlertTone> = {
  EMERGENCY: "error",
  QUEUE: "info",
  DOCTOR: "success",
  SCHEDULE: "warning",
};

const NOTIFICATION_POLL_MS = 15_000;

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "My Dashboard — Hospital Smart Queue" },
      { name: "description", content: "See your active queue, position and next appointment." },
      { property: "og:title", content: "My Dashboard — Hospital Smart Queue" },
      { property: "og:description", content: "Your active queue, position and appointments." },
    ],
  }),
  component: PatientDashboard,
});

function PatientDashboard() {
  const { session } = useAuth();
  const [queues, setQueues] = useState<QueueEntry[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [dismissedIds, setDismissedIds] = useState<Set<number>>(new Set());

  const patientId = session?.userId;
  const patientName = session?.name || "Patient";

  useEffect(() => {
    let active = true;
    Promise.all([getPatientQueue(patientId), getPatientAppointments(patientId)])
      .then(([q, a]) => {
        if (!active) return;
        setQueues(q);
        setAppointments(a);
        setLoading(false);
      })
      .catch((err) => {
        if (!active) return;
        console.error(err);
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [patientId]);

  // Poll for new notifications (queue called, upcoming-turn reminder, etc.) so
  // patients see them right here on the dashboard, not only on /notifications.
  useEffect(() => {
    let active = true;
    async function poll() {
      try {
        const data = await getPatientNotifications(patientId);
        if (active) setNotifications(data);
      } catch (err) {
        console.error(err);
      }
    }
    poll();
    const interval = setInterval(poll, NOTIFICATION_POLL_MS);
    return () => {
      active = false;
      clearInterval(interval);
    };
  }, [patientId]);

  const current = queues.find((q) => q.status === "WAITING");
  const unreadNotifications = notifications.filter((n) => !n.read && !dismissedIds.has(n.id));

  function dismissNotification(id: number) {
    setDismissedIds((prev) => new Set(prev).add(id));
  }

  return (
    <PatientLayout title="Dashboard">
      {/* pb-24 keeps the last card/panel from staying trapped under the fixed
          chat bubble (bottom-right) when the page is too short to scroll. */}
      <div className="space-y-6 pb-24">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3.5">
            <div className="relative flex size-14 shrink-0 items-center justify-center rounded-full bg-primary text-lg font-bold text-primary-foreground shadow-sm">
              {patientName.charAt(0).toUpperCase()}
              <span className="absolute -bottom-0.5 -right-0.5 flex size-4.5 items-center justify-center rounded-full border-2 border-background bg-success text-success-foreground">
                <BadgeCheck className="size-2.5" />
              </span>
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">Welcome back, {patientName}</h2>
              <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                {patientId ? (
                  <span className="rounded bg-muted px-1.5 py-0.5 font-semibold text-primary">
                    ID: {patientId}
                  </span>
                ) : null}
                <span>Here is your queue overview for today.</span>
              </div>
            </div>
          </div>
        </div>

        {unreadNotifications.length > 0 ? (
          <div className="space-y-2">
            {unreadNotifications.slice(0, 3).map((n) => (
              <Alert
                key={n.id}
                tone={toneForCategory[n.category]}
                onDismiss={() => dismissNotification(n.id)}
                className="animate-in fade-in slide-in-from-top-2"
              >
                <p className="font-semibold">{n.title}</p>
                <p className="opacity-90">{n.message}</p>
              </Alert>
            ))}
            {unreadNotifications.length > 3 ? (
              <Link
                to="/notifications"
                className="block text-right text-xs font-medium text-primary hover:underline"
              >
                +{unreadNotifications.length - 3} more — view all notifications
              </Link>
            ) : null}
          </div>
        ) : null}

        <ChatbotWidget />

        {loading ? (
          <>
            <SkeletonPanel title action />
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
              <SkeletonStatCard />
            </div>
            <div className="grid gap-6 lg:grid-cols-3">
              <SkeletonPanel className="lg:col-span-2" />
              <SkeletonPanel />
            </div>
          </>
        ) : (
          <>
            {/* AI Symptom & Triage Quick Start Banner */}
            <div className="relative overflow-hidden rounded-xl border border-primary/30 bg-linear-to-r from-primary/15 via-primary/5 to-transparent p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-3.5">
                  <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm">
                    <Sparkles className="size-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-foreground">
                        AI Symptom Checker & Triage
                      </h3>
                      <span className="rounded-full bg-primary/20 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-primary">
                        AI Powered
                      </span>
                    </div>
                    <p className="mt-0.5 max-w-xl text-xs text-muted-foreground sm:text-sm">
                      Unsure which specialist to visit? Describe your symptoms to our medical AI for
                      instant department recommendation, emergency triage, and real-time wait
                      estimation.
                    </p>
                  </div>
                </div>

                <Link
                  to="/queue/new"
                  className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90"
                >
                  <Sparkles className="size-4" /> Start AI Triage
                </Link>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard
                label="Active Queue"
                value={current?.queueNumber ?? "—"}
                caption={current?.departmentName ?? "No active queue"}
                icon={<ListOrdered className="size-5 text-muted-foreground" />}
                footer={
                  current ? (
                    <span className="flex items-center gap-1.5 text-xs font-medium text-success">
                      <CheckCircle2 className="size-3.5" /> Ticket valid for today
                    </span>
                  ) : undefined
                }
              />
              <StatCard
                label="Your Position"
                value={current ? `${current.position}th` : "—"}
                caption={
                  current ? `${Math.max(current.position - 1, 0)} patients ahead of you` : "In line"
                }
                tone="success"
                icon={<Users className="size-5 text-muted-foreground" />}
                footer={
                  current ? (
                    <div className="flex items-center gap-1">
                      {Array.from({ length: 5 }).map((_, i) => (
                        <span
                          key={i}
                          className={`h-2 flex-1 rounded-full ${
                            i < current.position - 1
                              ? "bg-success"
                              : i === current.position - 1
                                ? "animate-pulse bg-secondary"
                                : "bg-muted"
                          }`}
                        />
                      ))}
                    </div>
                  ) : undefined
                }
              />
              <StatCard
                label="Estimated Waiting"
                value={current ? `${current.estimatedWaitingMinutes} min` : "—"}
                caption="Approximate"
                tone="warning"
                icon={<Clock className="size-5 text-muted-foreground" />}
                footer={
                  current ? (
                    <span className="flex items-center gap-1.5 text-xs font-medium text-warning">
                      <Sparkles className="size-3.5" /> Pacing on schedule
                    </span>
                  ) : undefined
                }
              />
              <StatCard
                label="Upcoming Appointments"
                value={appointments.length}
                caption={appointments.length > 0 ? "Scheduled" : "Nothing booked yet"}
                icon={<CalendarDays className="size-5 text-muted-foreground" />}
                footer={
                  appointments[0] ? (
                    <span className="truncate text-xs font-medium text-muted-foreground">
                      Next: {appointments[0].doctorName ?? appointments[0].departmentName}
                    </span>
                  ) : undefined
                }
              />
            </div>

            <div className="grid gap-6 lg:grid-cols-3">
              <Panel
                title="Current Queue"
                className="lg:col-span-2"
                action={
                  <Link
                    to="/queue/new"
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    New queue
                  </Link>
                }
              >
                {current ? (
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
                    <QueueTokenBadge
                      className="min-w-0 sm:max-w-55"
                      token={current.queueNumber}
                      station={current.departmentName}
                      urgency={current.type === "EMERGENCY" ? "critical" : "general"}
                    />
                    <div className="grid min-w-0 flex-1 grid-cols-2 gap-4">
                      <Info label="Doctor" value={current.doctorName} />
                      <Info label="Position" value={`${current.position}th`} highlight />
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    You have no active queue. Start a new one to get a queue number.
                  </p>
                )}
              </Panel>

              <Panel title="Recent Activity">
                {queues.length > 0 ? (
                  <ul className="space-y-3">
                    {queues.map((q) => (
                      <li key={q.id} className="flex items-center justify-between gap-3 text-sm">
                        <div>
                          <p className="font-medium text-foreground">{q.queueNumber}</p>
                          <p className="text-muted-foreground">{q.departmentName}</p>
                        </div>
                        <StatusBadge status={q.status} />
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-muted-foreground">No recent activity.</p>
                )}
              </Panel>
            </div>
          </>
        )}
      </div>
    </PatientLayout>
  );
}

function Info({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={
          highlight ? "text-lg font-bold text-primary" : "text-base font-semibold text-foreground"
        }
      >
        {value}
      </p>
    </div>
  );
}
