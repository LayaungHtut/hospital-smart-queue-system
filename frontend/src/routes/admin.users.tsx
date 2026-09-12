import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Plus, UserCheck, Users, Check, X } from "lucide-react";
import {
  createUser,
  deleteUser,
  getUsers,
  getRegistrationRequests,
  approveRegistrationRequest,
  rejectRegistrationRequest,
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
import type { User, RegistrationRequest } from "@/types";

export const Route = createFileRoute("/admin/users")({
  head: () => ({
    meta: [
      { title: "User Management — Admin Portal" },
      {
        name: "description",
        content: "Manage staff, doctors, admins, and patient registration requests.",
      },
      { property: "og:title", content: "User Management — Admin Portal" },
      { property: "og:description", content: "Manage users and approve patient registrations." },
    ],
  }),
  component: AdminUsersPage,
});

function AdminUsersPage() {
  const [activeTab, setActiveTab] = useState<"users" | "requests">("requests");
  const [rows, setRows] = useState<User[]>([]);
  const [requests, setRequests] = useState<RegistrationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [role, setRole] = useState<"Staff" | "Doctor" | "Admin">("Staff");
  const [form, setForm] = useState({
    name: "",
    username: "",
    phone: "",
    email: "",
    department: "General Medicine",
    qualification: "MBBS, M.Med.Sc",
    experienceYears: 5,
  });
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getUsers(), getRegistrationRequests()])
      .then(([userData, reqData]) => {
        setRows(userData);
        setRequests(reqData);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || "Failed to load users.");
        setLoading(false);
      });
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const cleanPhone = form.phone.trim().replace(/[\s\-()]/g, "");
    if (cleanPhone && !/^09\d{7,9}$/.test(cleanPhone)) {
      setError("Phone number must start with 09 and contain 9 to 11 digits (e.g. 09123456789).");
      return;
    }
    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      setError("Please enter a valid email address.");
      return;
    }

    try {
      const created = await createUser({
        role: role,
        name: form.name.trim() || (role === "Admin" ? form.username.trim() : "New User"),
        userCode: role === "Admin" ? form.username.trim() : undefined,
        phone: cleanPhone,
        email: form.email.trim(),
        contact: cleanPhone || form.email.trim(),
        department: form.department,
        qualification: form.qualification,
        experienceYears: form.experienceYears,
      });
      setRows((prev) => [...prev, created]);
      setForm({
        name: "",
        username: "",
        phone: "",
        email: "",
        department: "General Medicine",
        qualification: "MBBS, M.Med.Sc",
        experienceYears: 5,
      });
      setOpen(false);
      setMessage(`User created successfully in database table '${role === "Doctor" ? "doctor" : role === "Admin" ? "admin_user" : "staff"}'!`);
      setTimeout(() => setMessage(null), 4000);
    } catch (err: any) {
      setError(err?.message || "Failed to create user. Ensure username, email, and phone are unique.");
    }
  }

  async function remove(id: string | number) {
    await deleteUser(id);
    setRows((prev) => prev.filter((u) => u.id !== id));
  }

  async function handleApprove(reqId: number) {
    setActionLoading(reqId);
    setMessage(null);
    try {
      const res = await approveRegistrationRequest(reqId);
      setRequests((prev) => prev.map((r) => (r.id === reqId ? { ...r, status: "APPROVED" } : r)));
      setMessage(res.message || "Patient registration approved and account activated!");
    } catch (err: any) {
      setMessage(err?.message || "Failed to approve registration.");
    } finally {
      setActionLoading(null);
    }
  }

  async function handleReject(reqId: number) {
    setActionLoading(reqId);
    setMessage(null);
    try {
      await rejectRegistrationRequest(reqId, "Declined by administrator");
      setRequests((prev) => prev.map((r) => (r.id === reqId ? { ...r, status: "REJECTED" } : r)));
      setMessage("Registration request rejected.");
    } catch (err: any) {
      setMessage(err?.message || "Failed to reject registration.");
    } finally {
      setActionLoading(null);
    }
  }

  const pendingCount = requests.filter((r) => r.status === "PENDING").length;

  const userColumns: Column<User>[] = [
    {
      header: "User ID",
      cell: (r) => <span className="font-medium text-primary">{r.userCode}</span>,
    },
    { header: "Name", cell: (r) => r.name },
    { header: "Role", cell: (r) => r.role },
    { header: "Contact", cell: (r) => r.contact },
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

  const requestColumns: Column<RegistrationRequest>[] = [
    {
      header: "Patient Name",
      cell: (r) => <span className="font-semibold text-foreground">{r.name}</span>,
    },
    { header: "Phone Number", cell: (r) => r.phone },
    { header: "Gender", cell: (r) => r.gender || "—" },
    { header: "Date of Birth", cell: (r) => r.dateOfBirth || "—" },
    {
      header: "Status",
      cell: (r) => (
        <span
          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
            r.status === "APPROVED"
              ? "bg-success/15 text-success"
              : r.status === "REJECTED"
                ? "bg-danger/15 text-danger"
                : "bg-amber-500/15 text-amber-600 dark:text-amber-400"
          }`}
        >
          {r.status}
        </span>
      ),
    },
    {
      header: "Actions",
      cell: (r) =>
        r.status === "PENDING" ? (
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleApprove(r.id)}
              disabled={actionLoading === r.id}
              className="inline-flex items-center gap-1 rounded bg-success px-2.5 py-1 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-50"
            >
              <Check className="size-3.5" /> Approve
            </button>
            <button
              onClick={() => handleReject(r.id)}
              disabled={actionLoading === r.id}
              className="inline-flex items-center gap-1 rounded border border-danger/40 bg-card px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger/10 disabled:opacity-50"
            >
              <X className="size-3.5" /> Reject
            </button>
          </div>
        ) : (
          <span className="text-xs text-muted-foreground">
            {r.status === "APPROVED" ? "Account Active" : "Declined"}
          </span>
        ),
    },
  ];

  return (
    <AdminLayout title="User & Registration Management">
      {message ? (
        <div className="mb-4 flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 p-3 text-sm text-primary">
          <span>{message}</span>
          <button onClick={() => setMessage(null)} className="text-xs font-bold hover:underline">
            Dismiss
          </button>
        </div>
      ) : null}

      {/* Tabs Switcher */}
      <div className="mb-6 flex overflow-x-auto border-b border-border scrollbar-none">
        <button
          onClick={() => setActiveTab("requests")}
          className={`flex shrink-0 items-center gap-2 border-b-2 px-3 py-3 text-xs font-semibold transition sm:px-5 sm:text-sm ${
            activeTab === "requests"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <UserCheck className="size-4" />
          <span className="hidden sm:inline">Patient Registration Requests</span>
          <span className="sm:hidden">Requests</span>
          {pendingCount > 0 ? (
            <span className="rounded-full bg-primary px-2 py-0.5 text-[10px] text-primary-foreground sm:text-xs">
              {pendingCount}
            </span>
          ) : null}
        </button>
        <button
          onClick={() => setActiveTab("users")}
          className={`flex shrink-0 items-center gap-2 border-b-2 px-3 py-3 text-xs font-semibold transition sm:px-5 sm:text-sm ${
            activeTab === "users"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Users className="size-4" />
          <span className="hidden sm:inline">Hospital Staff & System Users</span>
          <span className="sm:hidden">Users</span>
        </button>
      </div>

      {activeTab === "requests" ? (
        <Panel
          title="Patient Registration Approval Requests"
        >
          {loading ? (
            <SkeletonTable rows={5} columns={5} />
          ) : (
            <DataTable
              rows={requests}
              columns={requestColumns}
              loading={loading}
              searchable
              searchKeys={["name", "phone", "status"]}
            />
          )}
        </Panel>
      ) : (
        <Panel
          title="Hospital Staff & Doctors"
          action={
            <button
              onClick={() => setOpen((v) => !v)}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
              <Plus className="size-4" /> Add User
            </button>
          }
        >
          {message && (
            <div className="mb-4 rounded-lg bg-success/15 p-3 text-sm font-semibold text-success">
              ✓ {message}
            </div>
          )}

          {open ? (
            <form
              onSubmit={submit}
              className="mb-6 rounded-lg border border-border bg-card/60 p-4 space-y-4 shadow-sm"
            >
              {error && (
                <div className="rounded-md bg-danger/10 p-2.5 text-xs font-semibold text-danger">
                  ⚠ {error}
                </div>
              )}

              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Select User Role:</span>
                  <div className="flex rounded-lg border border-border bg-background p-1 text-xs font-semibold">
                    {(["Staff", "Doctor", "Admin"] as const).map((r) => (
                      <button
                        key={r}
                        type="button"
                        onClick={() => {
                          setRole(r);
                          setError(null);
                        }}
                        className={`rounded px-3 py-1 transition ${
                          role === r ? "bg-primary text-primary-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
                        }`}
                      >
                        {r}
                      </button>
                    ))}
                  </div>
                </div>
                <span className="rounded bg-primary/10 px-2 py-0.5 text-[11px] font-mono text-primary">
                  Database Table: {role === "Doctor" ? "doctor" : role === "Admin" ? "admin_user" : "staff"}
                </span>
              </div>

              {role === "Doctor" ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <Field label="Doctor Full Name">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. Dr. Hlaing Myint"
                      value={form.name}
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                    />
                  </Field>
                  <Field label="Department">
                    <select
                      className={inputClass}
                      value={form.department}
                      onChange={(e) => setForm({ ...form, department: e.target.value })}
                    >
                      <option>Cardiology</option>
                      <option>Neurology</option>
                      <option>Orthopedics</option>
                      <option>General Medicine</option>
                      <option>Pediatrics</option>
                      <option>Dermatology</option>
                    </select>
                  </Field>
                  <Field label="Degree / Qualification">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. MBBS, M.Med.Sc (Int. Med), MRCP"
                      value={form.qualification}
                      onChange={(e) => setForm({ ...form, qualification: e.target.value })}
                    />
                  </Field>
                  <Field label="Years of Experience">
                    <input
                      type="number"
                      min={0}
                      className={inputClass}
                      value={form.experienceYears}
                      onChange={(e) => setForm({ ...form, experienceYears: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label="Phone Number (09...)">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. 09123456789"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    />
                  </Field>
                  <Field label="Official Email">
                    <input
                      type="email"
                      className={inputClass}
                      placeholder="doctor@hospital.com"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                    />
                  </Field>
                </div>
              ) : role === "Admin" ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Username (Login identifier)">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. sysadmin"
                      value={form.username}
                      onChange={(e) => setForm({ ...form, username: e.target.value })}
                    />
                  </Field>
                  <Field label="Full Name">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. Admin Zaw Win"
                      value={form.name}
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                    />
                  </Field>
                  <Field label="Admin Email">
                    <input
                      type="email"
                      required
                      className={inputClass}
                      placeholder="admin@hospital.com"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                    />
                  </Field>
                  <Field label="Phone Number (Optional)">
                    <input
                      className={inputClass}
                      placeholder="e.g. 09123456789"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    />
                  </Field>
                </div>
              ) : (
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Staff Full Name">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. Daw Khin Khin"
                      value={form.name}
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                    />
                  </Field>
                  <Field label="Role / Department">
                    <input
                      className={inputClass}
                      placeholder="e.g. Front Desk / Receptionist"
                      value={form.department}
                      onChange={(e) => setForm({ ...form, department: e.target.value })}
                    />
                  </Field>
                  <Field label="Contact Phone (09...)">
                    <input
                      required
                      className={inputClass}
                      placeholder="e.g. 09123456789"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    />
                  </Field>
                  <Field label="Email Address">
                    <input
                      type="email"
                      className={inputClass}
                      placeholder="staff@hospital.com"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                    />
                  </Field>
                </div>
              )}

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <button
                  type="button"
                  onClick={() => setOpen(false)}
                  className="rounded-lg border border-border px-4 py-2 text-xs font-medium hover:bg-muted"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm"
                >
                  Create {role} Account
                </button>
              </div>
            </form>
          ) : null}

          {loading ? (
            <SkeletonTable rows={5} columns={6} />
          ) : (
            <DataTable
              rows={rows}
              columns={userColumns}
              loading={loading}
              searchable
              searchKeys={["name", "role", "contact"]}
            />
          )}
        </Panel>
      )}
    </AdminLayout>
  );
}
