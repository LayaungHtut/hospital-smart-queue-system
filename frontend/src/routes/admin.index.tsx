import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { TooltipProps } from "recharts";
import { getAdminDashboard } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import { Panel, StatCard } from "@/components/portal/ui-kit";
import { SkeletonStatCard, SkeletonPanel } from "@/components/ui/loading";
import type { AdminDashboard } from "@/types";

export const Route = createFileRoute("/admin/")({
  head: () => ({
    meta: [
      { title: "Admin Dashboard — Hospital Smart Queue" },
      {
        name: "description",
        content: "Hospital-wide queue analytics, waiting times and doctor coverage.",
      },
      { property: "og:title", content: "Admin Dashboard — Hospital Smart Queue" },
      { property: "og:description", content: "Hospital-wide queue analytics and doctor coverage." },
    ],
  }),
  component: AdminDashboardPage,
});

type DepartmentSlice = { name: string; percent: number; color: string };

function DepartmentTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null;
  const entry = payload[0];
  if (!entry) return null;
  const d = entry.payload as DepartmentSlice;
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 text-sm shadow-md">
      <div className="flex items-center gap-2">
        <span className="size-2.5 rounded-full" style={{ backgroundColor: d.color }} aria-hidden />
        <span className="font-medium text-popover-foreground">{d.name}</span>
      </div>
      <p className="mt-0.5 text-popover-foreground/80">{d.percent}% of today's queues</p>
    </div>
  );
}

function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeDept, setActiveDept] = useState<string | null>(null);

  useEffect(() => {
    getAdminDashboard()
      .then((d) => {
        setData(d);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  if (loading && !data) {
    return (
      <AdminLayout title="Dashboard">
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </div>
          <div className="grid gap-6 lg:grid-cols-3">
            <SkeletonPanel className="lg:col-span-2 h-72" />
            <SkeletonPanel className="h-52" />
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout title="Dashboard">
      <div className="space-y-6">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-secondary">
            Executive Overview
          </span>
          <h2 className="mt-0.5 text-xl font-bold text-foreground">Hospital-wide Analytics</h2>
          <p className="text-sm text-muted-foreground">
            Live patient volume, waiting times, and doctor coverage across all departments.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Total Patients Today"
            value={data?.totalPatientsToday ?? "—"}
            caption="Patients"
          />
          <StatCard
            label="Total Queues Today"
            value={data?.totalQueuesToday ?? "—"}
            caption="Queues"
            tone="success"
          />
          <StatCard
            label="Average Waiting Time"
            value={data ? `${data.averageWaitingMinutes} min` : "—"}
            tone="warning"
          />
          <StatCard
            label="Doctors On Duty"
            value={data?.doctorsOnDuty ?? "—"}
            caption="Doctors"
            tone="danger"
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <Panel title="Weekly Queue Volume" className="lg:col-span-2">
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data?.weekly ?? []}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--color-border)"
                    vertical={false}
                  />
                  <XAxis dataKey="date" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickLine={false} axisLine={false} fontSize={12} />
                  <Tooltip />
                  <Legend />
                  <Bar
                    dataKey="queues"
                    name="Queues"
                    fill="var(--color-primary)"
                    radius={[4, 4, 0, 0]}
                  />
                  <Bar
                    dataKey="completed"
                    name="Completed"
                    fill="var(--color-success)"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Panel>

          <Panel title="Queues by Department">
            <div className="h-52">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={data?.byDepartment ?? []}
                    dataKey="percent"
                    nameKey="name"
                    innerRadius={45}
                    outerRadius={75}
                    paddingAngle={2}
                    stroke="var(--color-card)"
                    strokeWidth={2}
                    onMouseEnter={(_, index) =>
                      setActiveDept(data?.byDepartment[index]?.name ?? null)
                    }
                    onMouseLeave={() => setActiveDept(null)}
                  >
                    {(data?.byDepartment ?? []).map((d) => (
                      <Cell
                        key={d.name}
                        fill={d.color}
                        opacity={activeDept === null || activeDept === d.name ? 1 : 0.35}
                        className="cursor-pointer transition-opacity"
                      />
                    ))}
                  </Pie>
                  <Tooltip content={<DepartmentTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <ul className="mt-4 space-y-2">
              {(data?.byDepartment ?? []).map((d) => (
                <li
                  key={d.name}
                  className="flex items-center gap-3 rounded-md px-1.5 py-1 text-sm transition-colors"
                  style={{
                    backgroundColor: activeDept === d.name ? "var(--color-accent)" : "transparent",
                  }}
                  onMouseEnter={() => setActiveDept(d.name)}
                  onMouseLeave={() => setActiveDept(null)}
                >
                  <span
                    className="size-3 rounded-sm"
                    style={{ backgroundColor: d.color }}
                    aria-hidden
                  />
                  <span className="flex-1 text-foreground">{d.name}</span>
                  <span className="font-medium text-foreground">{d.percent}%</span>
                </li>
              ))}
            </ul>
          </Panel>
        </div>
      </div>
    </AdminLayout>
  );
}
