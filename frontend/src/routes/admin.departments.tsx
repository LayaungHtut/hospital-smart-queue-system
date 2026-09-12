import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { createDepartment, deleteDepartment, getDepartments } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import {
  DataTable,
  Field,
  Panel,
  StatusBadge,
  inputClass,
  type Column,
} from "@/components/portal/ui-kit";
import { SkeletonTable } from "@/components/ui/loading";
import type { Department } from "@/types";

export const Route = createFileRoute("/admin/departments")({
  head: () => ({
    meta: [
      { title: "Department Management — Admin Portal" },
      {
        name: "description",
        content: "Create and maintain hospital departments and their locations.",
      },
      { property: "og:title", content: "Department Management — Admin Portal" },
      { property: "og:description", content: "Create and maintain hospital departments." },
    ],
  }),
  component: AdminDepartmentsPage,
});

function AdminDepartmentsPage() {
  const [rows, setRows] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ name: "", location: "" });

  useEffect(() => {
    getDepartments()
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const created = await createDepartment({
      name: form.name,
      location: form.location,
      departmentCode: `DP-${Math.floor(Math.random() * 900 + 100)}`,
      status: "ACTIVE",
    });
    setRows((prev) => [...prev, created]);
    setForm({ name: "", location: "" });
    setOpen(false);
  }

  async function remove(id: string | number) {
    await deleteDepartment(id);
    setRows((prev) => prev.filter((d) => d.id !== id));
  }

  const columns: Column<Department>[] = [
    {
      header: "Dept. ID",
      cell: (r) => <span className="font-medium text-primary">{r.departmentCode}</span>,
    },
    { header: "Department Name", cell: (r) => r.name },
    { header: "Location", cell: (r) => r.location },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    {
      header: "Action",
      cell: (r) => (
        <button
          onClick={() => remove(r.id)}
          className="text-xs font-medium text-danger hover:underline"
        >
          Delete
        </button>
      ),
    },
  ];

  return (
    <AdminLayout title="Department Management">
      <Panel
        title="Departments"
        action={
          <button
            onClick={() => setOpen((v) => !v)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 sm:px-4 sm:text-sm"
          >
            <Plus className="size-4" /> <span className="hidden sm:inline">Add Department</span><span className="sm:hidden">Add</span>
          </button>
        }
      >
        {open ? (
          <form
            onSubmit={submit}
            className="mb-6 grid gap-4 rounded-lg border border-border p-4 sm:grid-cols-2"
          >
            <Field label="Department Name">
              <input
                required
                className={inputClass}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field label="Location">
              <input
                required
                className={inputClass}
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
              />
            </Field>
            <div className="sm:col-span-2">
              <button className="rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90">
                Save Department
              </button>
            </div>
          </form>
        ) : null}

        {loading ? (
          <SkeletonTable rows={5} columns={5} />
        ) : (
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            searchable
            searchKeys={["name", "location"]}
          />
        )}
      </Panel>
    </AdminLayout>
  );
}
