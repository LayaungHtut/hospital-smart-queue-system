import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Plus, Edit2 } from "lucide-react";
import {
  createDoctor,
  updateDoctor,
  deleteDoctor,
  getDepartments,
  getDoctors,
} from "@/services/api";
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
import type { Department, DoctorDetail } from "@/types";

export const Route = createFileRoute("/admin/doctors")({
  head: () => ({
    meta: [
      { title: "Doctor Management — Admin Portal" },
      {
        name: "description",
        content: "Add, review and update doctors across hospital departments.",
      },
      { property: "og:title", content: "Doctor Management — Admin Portal" },
      { property: "og:description", content: "Add, review and update hospital doctors." },
    ],
  }),
  component: AdminDoctorsPage,
});

const emptyForm = {
  name: "",
  qualification: "MBBS, M.Med.Sc",
  specialization: "General Medicine",
  department: "",
  experienceYears: 5,
  estimatedWaitingMinutes: 15,
  phone: "",
};

function AdminDoctorsPage() {
  const [doctors, setDoctors] = useState<DoctorDetail[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState<DoctorDetail | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  useEffect(() => {
    getDepartments()
      .then(setDepartments)
      .catch((err) => setError(err.message));
    getDoctors()
      .then((data) => {
        setDoctors(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "Failed to load doctors.");
        setLoading(false);
      });
  }, []);

  function showToast(msg: string) {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3500);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const cleanPhone = form.phone.trim().replace(/[\s\-()]/g, "");
    if (cleanPhone && !/^09\d{7,9}$/.test(cleanPhone)) {
      setError("Doctor phone number must start with 09 and contain 9 to 11 digits.");
      return;
    }

    try {
      const created = await createDoctor({
        ...form,
        phone: cleanPhone,
        doctorCode: `D-${Math.floor(Math.random() * 900 + 100)}`,
        currentPatientId: 0,
        status: "ACTIVE",
        availabilityStatus: "CONSULTING",
      });
      setDoctors((prev) => [...prev, created]);
      setForm(emptyForm);
      setOpen(false);
      showToast("Doctor profile added successfully!");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to add doctor. Please ensure phone number is unique.",
      );
    }
  }

  async function handleEditSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!editingDoctor) return;
    setError(null);

    const cleanPhone = (editingDoctor.phone || "").trim().replace(/[\s\-()]/g, "");
    if (cleanPhone && !/^09\d{7,9}$/.test(cleanPhone)) {
      setError("Doctor phone number must start with 09 and contain 9 to 11 digits.");
      return;
    }

    const deptObj = departments.find((d) => d.name === editingDoctor.department);

    const res = await updateDoctor(editingDoctor.id, {
      name: editingDoctor.name,
      qualification: editingDoctor.qualification,
      specialization: editingDoctor.specialization,
      departmentId: deptObj?.id,
      experienceYears: editingDoctor.experienceYears,
      phone: cleanPhone,
      status: editingDoctor.status,
    });

    if (res.success) {
      setDoctors((prev) =>
        prev.map((d) => (d.id === editingDoctor.id ? { ...editingDoctor, phone: cleanPhone } : d)),
      );
      setEditingDoctor(null);
      showToast("Doctor profile updated successfully!");
    } else {
      setError(res.message);
    }
  }

  async function remove(id: string | number) {
    await deleteDoctor(id);
    setDoctors((prev) => prev.filter((d) => d.id !== id));
    showToast("Doctor deactivated successfully.");
  }

  const columns: Column<DoctorDetail>[] = [
    {
      header: "Doctor ID",
      cell: (r) => <span className="font-medium text-primary">{r.doctorCode}</span>,
    },
    { header: "Name", cell: (r) => r.name },
    { header: "Degree / Qualification", cell: (r) => r.qualification || "MBBS, M.Med.Sc" },
    { header: "Specialization", cell: (r) => r.specialization || "General" },
    { header: "Department", cell: (r) => r.department },
    { header: "Experience", cell: (r) => `${r.experienceYears} yrs` },
    { header: "Est. Waiting", cell: (r) => `${r.estimatedWaitingMinutes} min` },
    { header: "Phone", cell: (r) => r.phone },
    { header: "Status", cell: (r) => <StatusBadge status={r.status} /> },
    {
      header: "Action",
      cell: (r) => (
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setEditingDoctor({ ...r });
              setError(null);
            }}
            className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
          >
            <Edit2 className="size-3" /> Edit
          </button>
          <button
            onClick={() => remove(r.id)}
            className="text-xs font-medium text-danger hover:underline"
          >
            Deactivate
          </button>
        </div>
      ),
    },
  ];

  return (
    <AdminLayout title="Doctor Management">
      {toastMsg && (
        <div className="mb-4 rounded-lg bg-success/15 p-3 text-center text-xs font-bold text-success">
          ✓ {toastMsg}
        </div>
      )}

      <Panel
        title="Doctors"
        action={
          <button
            onClick={() => {
              setOpen((v) => !v);
              setError(null);
            }}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm sm:px-4 sm:text-sm"
          >
            <Plus className="size-4" /> <span className="hidden sm:inline">Add Doctor</span>
            <span className="sm:hidden">Add</span>
          </button>
        }
      >
        {open ? (
          <form
            onSubmit={submit}
            className="mb-6 grid gap-4 rounded-lg border border-border bg-card p-4 sm:grid-cols-2 shadow-sm"
          >
            <h4 className="sm:col-span-2 text-sm font-bold text-foreground">Add New Doctor</h4>
            {error && (
              <div className="sm:col-span-2 rounded-md bg-danger/10 p-2.5 text-xs font-semibold text-danger">
                ⚠ {error}
              </div>
            )}
            <Field label="Full Name">
              <input
                required
                className={inputClass}
                placeholder="e.g. Dr. Zaw Min"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field label="Degree / Qualification">
              <input
                required
                className={inputClass}
                placeholder="e.g. MBBS, M.Med.Sc (Int. Med), MRCP (UK)"
                value={form.qualification}
                onChange={(e) => setForm({ ...form, qualification: e.target.value })}
              />
            </Field>
            <Field label="Specialization">
              <input
                required
                className={inputClass}
                placeholder="e.g. Interventional Cardiology"
                value={form.specialization}
                onChange={(e) => setForm({ ...form, specialization: e.target.value })}
              />
            </Field>
            <Field label="Department">
              <select
                required
                className={inputClass}
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
              >
                <option value="">-- Select Department --</option>
                {departments.map((d) => (
                  <option key={d.id}>{d.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Experience (years)">
              <input
                type="number"
                min={0}
                className={inputClass}
                value={form.experienceYears}
                onChange={(e) => setForm({ ...form, experienceYears: Number(e.target.value) })}
              />
            </Field>
            <Field label="Phone Number (Myanmar)">
              <input
                required
                className={inputClass}
                placeholder="e.g. 09123456789"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </Field>
            <div className="sm:col-span-2 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded-lg border border-border px-4 py-2 text-xs font-medium hover:bg-muted"
              >
                Cancel
              </button>
              <button className="rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90">
                Save Doctor
              </button>
            </div>
          </form>
        ) : null}

        {/* Edit Doctor Modal */}
        {editingDoctor && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <div className="w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-2xl animate-in fade-in zoom-in-95">
              <h3 className="text-base font-bold text-foreground">
                Update Doctor: {editingDoctor.name} ({editingDoctor.doctorCode})
              </h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Modify doctor qualifications, specialization, experience, and contact details.
              </p>

              {error && (
                <div className="mt-3 rounded-md bg-danger/10 p-2.5 text-xs font-semibold text-danger">
                  ⚠ {error}
                </div>
              )}

              <form onSubmit={handleEditSubmit} className="mt-4 grid gap-4 sm:grid-cols-2">
                <Field label="Full Name">
                  <input
                    required
                    className={inputClass}
                    value={editingDoctor.name}
                    onChange={(e) => setEditingDoctor({ ...editingDoctor, name: e.target.value })}
                  />
                </Field>
                <Field label="Degree / Qualification">
                  <input
                    required
                    className={inputClass}
                    value={editingDoctor.qualification || ""}
                    onChange={(e) =>
                      setEditingDoctor({ ...editingDoctor, qualification: e.target.value })
                    }
                  />
                </Field>
                <Field label="Specialization">
                  <input
                    required
                    className={inputClass}
                    value={editingDoctor.specialization || ""}
                    onChange={(e) =>
                      setEditingDoctor({ ...editingDoctor, specialization: e.target.value })
                    }
                  />
                </Field>
                <Field label="Department">
                  <select
                    required
                    className={inputClass}
                    value={editingDoctor.department}
                    onChange={(e) =>
                      setEditingDoctor({ ...editingDoctor, department: e.target.value })
                    }
                  >
                    {departments.map((d) => (
                      <option key={d.id}>{d.name}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Experience (years)">
                  <input
                    type="number"
                    min={0}
                    className={inputClass}
                    value={editingDoctor.experienceYears}
                    onChange={(e) =>
                      setEditingDoctor({
                        ...editingDoctor,
                        experienceYears: Number(e.target.value),
                      })
                    }
                  />
                </Field>
                <Field label="Phone Number (Myanmar)">
                  <input
                    required
                    className={inputClass}
                    value={editingDoctor.phone}
                    onChange={(e) => setEditingDoctor({ ...editingDoctor, phone: e.target.value })}
                  />
                </Field>
                <Field label="Status">
                  <select
                    className={inputClass}
                    value={editingDoctor.status}
                    onChange={(e) =>
                      setEditingDoctor({
                        ...editingDoctor,
                        status: e.target.value as "ACTIVE" | "INACTIVE",
                      })
                    }
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </Field>

                <div className="sm:col-span-2 flex justify-end gap-2 border-t border-border pt-4">
                  <button
                    type="button"
                    onClick={() => setEditingDoctor(null)}
                    className="rounded-lg border border-border px-4 py-2 text-xs font-medium hover:bg-muted"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="rounded-lg bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm"
                  >
                    Save Changes
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {loading ? (
          <SkeletonTable rows={5} columns={9} />
        ) : (
          <DataTable
            rows={doctors}
            columns={columns}
            loading={loading}
            searchable
            searchKeys={["name", "department", "doctorCode", "specialization"]}
            pageSize={8}
          />
        )}
      </Panel>
    </AdminLayout>
  );
}
