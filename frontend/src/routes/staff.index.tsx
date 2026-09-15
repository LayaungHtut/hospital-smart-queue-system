import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { getStaffDashboard } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { Panel, StatCard } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonStatCard, SkeletonPanel } from "@/components/ui/loading";
import type { StaffDashboard } from "@/types";

export const Route = createFileRoute("/staff/")({
  head: () => ({
    meta: [
      { title: "Staff Dashboard — Hospital Smart Queue" },
      {
        name: "description",
        content: "Today's queue totals, emergencies and doctor availability.",
      },
      { property: "og:title", content: "Staff Dashboard — Hospital Smart Queue" },
      { property: "og:description", content: "Today's queue totals and emergency overview." },
    ],
  }),
  component: StaffDashboardPage,
});

const STATUS_COLORS: Record<string, string> = {
  Emergency: "#EF4444",
  Appointment: "#0EA5E9",
  Normal: "#10B981",
};

function resolveStatusColor(label: string, apiColor?: string): string {
  if (apiColor && !apiColor.startsWith("var(") && apiColor.startsWith("#")) {
    return apiColor;
  }
  return STATUS_COLORS[label] || "#0EA5E9";
}

function StaffDashboardPage() {
  const { session } = useAuth();
  const [data, setData] = useState<StaffDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStaffDashboard()
      .then((d) => {
        setData(d);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const todayStr = new Date().toLocaleDateString("en-US", {
    weekday: "long",
    year: "numeric",
    month: "short",
    month: "long",
    day: "numeric",
  });

  if (loading && !data) {
    return (
      <StaffLayout title="Dashboard">
        <div className="space-y-6">
          <SkeletonCard />
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </div>
          <div className="grid gap-6 lg:grid-cols-2">
            <SkeletonPanel className="h-48" />
            <SkeletonPanel />
          </div>
        </div>
      </StaffLayout>
    );
  }

  const dashboard = data ?? {
    totalQueuesToday: 0,
    emergencyPending: 0,
    activeDoctors: 0,
    checkedInAppointments: 0,
    breakdown: [],
    highlights: [],
  };

  const breakdownData = dashboard.breakdown.map((b) => ({
    ...b,
    color: resolveStatusColor(b.label, b.color),
  }));

  return (
    <StaffLayout title="Dashboard">
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3.5">
            <div className="flex size-12 shrink-0 items-center justify-center rounded-full bg-primary text-base font-bold text-primary-foreground shadow-sm">
              {(session?.name ?? "S").charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">
                Welcome back, {session?.name ?? "Staff"}
              </h2>
              <p className="text-sm text-muted-foreground">Here is today's overview.</p>
            </div>
          </div>
          <span className="rounded-lg border border-border bg-card px-4 py-2 text-sm text-foreground">
            {todayStr}
          </span>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Total Queues Today"
            value={dashboard.totalQueuesToday}
            caption="Patients"
            tone="success"
            footer={
              <Link to="/staff/queue-monitor" className="text-primary hover:underline">
                View details
              </Link>
            }
          />
          <StatCard
            label="Emergency Cases"
            value={dashboard.emergencyPending}
            caption="Pending"
            tone="danger"
            footer={
              <Link to="/staff/emergency" className="text-primary hover:underline">
                View details
              </Link>
            }
          />
          <StatCard
            label="Active Doctors"
            value={dashboard.activeDoctors}
            caption="Doctors"
            footer={
              <Link to="/staff/queue-monitor" className="text-primary hover:underline">
                View details
              </Link>
            }
          />
          <StatCard
            label="Check-in Appointments"
            value={dashboard.checkedInAppointments}
            caption="Today"
            tone="warning"
            footer={
              <Link to="/staff/appointments" className="text-primary hover:underline">
                View details
              </Link>
            }
            caption="Scheduled arrivals"
            tone="primary"
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <Panel title="Queue Overview">
            <div className="flex flex-col items-center gap-6 sm:flex-row">
              <div className="h-48 w-48 shrink-0">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={dashboard.breakdown}
                      data={breakdownData}
                      dataKey="value"
                      nameKey="label"
                      innerRadius={50}
                      outerRadius={80}
                      paddingAngle={2}
                      stroke="var(--color-card, #ffffff)"
                      strokeWidth={2}
                    >
                      {dashboard.breakdown.map((entry) => (
                      {breakdownData.map((entry) => (
                        <Cell key={entry.label} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <ul className="w-full space-y-3">
                {dashboard.breakdown.map((b) => (
                {breakdownData.map((b) => (
                  <li key={b.label} className="flex items-center gap-3 text-sm">
                    <span
                      className="size-3 rounded-sm"
                      className="size-3 rounded-sm shrink-0 shadow-xs"
                      style={{ backgroundColor: b.color }}
                      aria-hidden
                    />
                    <span className="flex-1 text-foreground">{b.label}</span>
                    <span className="font-medium text-foreground">
                      {b.value} ({b.percent}%)
                    </span>
                  </li>
                ))}
                <li className="flex items-center justify-between border-t border-border pt-3 text-sm font-semibold text-foreground">
                  <span>Total</span>
                  <span>{dashboard.totalQueuesToday}</span>
                </li>
              </ul>
            </div>
          </Panel>

          <Panel title="Today's Highlights">
            <ul className="space-y-4">
              {dashboard.highlights.map((h) => (
                <li key={h.label} className="flex items-center justify-between text-sm">
                  <span className="text-foreground">{h.label}</span>
                  <span className="font-semibold text-foreground">
                    {h.label === "Average Waiting Time" ? `${h.value} min` : h.value}
                  </span>
                </li>
              ))}
            </ul>
          </Panel>
        </div>
      </div>
    </StaffLayout>
  );
}
