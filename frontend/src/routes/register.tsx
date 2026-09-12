import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { Clock, ShieldCheck, Eye, EyeOff, Sparkles, UserRound } from "lucide-react";
import { registerPatient } from "@/services/api";
import { Field, Stepper, inputClass } from "@/components/portal/ui-kit";

export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Patient Registration — Hospital Smart Queue" },
      { name: "description", content: "Create a patient account to join hospital queues online." },
      { property: "og:title", content: "Patient Registration — Hospital Smart Queue" },
      {
        property: "og:description",
        content: "Submit your patient registration for admin approval.",
      },
    ],
  }),
  component: RegisterPage,
});

const steps = ["Personal Info", "Request Submitted"];

function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    name: "",
    phone: "",
    password: "",
    dateOfBirth: "",
    gender: "",
    address: "",
  });
  const [showPassword, setShowPassword] = useState(false);

  const MYANMAR_REGIONS = [
    "yangon", "mandalay", "naypyidaw", "nay pyi taw", "bago", "ayeyarwady", "magway",
    "sagaing", "tanintharyi", "kachin", "kayah", "kayin", "chin", "mon", "rakhine", "shan",
    "ရန်ကုန်", "မန္တလေး", "နေပြည်တော်", "ပဲခူး", "ဧရာဝတီ", "မကွေး", "စစ်ကိုင်း"
  ];

  async function submitPersonal(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    // Validate phone number - must start with 09 and be 9-11 digits
    const cleanPhone = form.phone.trim().replace(/[\s\-()]/g, "");
    if (!cleanPhone.startsWith("09")) {
      setError("Phone number must start with 09 (Myanmar format).");
      return;
    }
    if (cleanPhone.length < 9 || cleanPhone.length > 11) {
      setError("Phone number must be 9 to 11 digits long (e.g. 09123456789).");
      return;
    }
    if (!/^\d+$/.test(cleanPhone)) {
      setError("Phone number must contain only digits.");
      return;
    }
    if (!/^09\d{7,9}$/.test(cleanPhone)) {
      setError("Invalid phone number format. Must start with 09 followed by 7-9 digits.");
      return;
    }

    // Validate Myanmar address if provided
    if (form.address.trim()) {
      const lower = form.address.toLowerCase();
      const hasRegion = MYANMAR_REGIONS.some((r) => lower.includes(r));
      if (!hasRegion) {
        setError("Address must be within a Myanmar Region/State (e.g. Yangon, Mandalay, Bago, Shan, etc.).");
        return;
      }
      const parts = form.address.split(/[,;\/\n]/);
      const hasStreetOrTown = lower.includes("st") || lower.includes("road") || lower.includes("lane") || lower.includes("tsp") || lower.includes("town") || lower.includes("လမ်း");
      if (parts.length < 3 && !hasStreetOrTown && form.address.trim().length < 15) {
        setError("Please enter a complete address including Region, Town/Township, and Street.");
        return;
      }
    }

    setLoading(true);
    try {
      await registerPatient({ ...form, phone: cleanPhone });
      setStep(1);
    } catch (err: any) {
      setError(err?.message ?? "Registration request failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-stretch bg-background">
      {/* Branded side panel */}
      <div className="relative hidden w-full max-w-md flex-col justify-between overflow-hidden bg-primary p-10 text-primary-foreground lg:flex">
        <div className="absolute -right-16 -top-16 size-64 rounded-full bg-primary-foreground/10 blur-3xl" aria-hidden />
        <div className="relative">
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary-foreground/15">
              <Sparkles className="size-4.5" />
            </div>
            <span className="text-sm font-bold uppercase tracking-wider">City General Hospital</span>
          </div>
          <div className="mt-10 flex size-14 items-center justify-center rounded-2xl bg-primary-foreground/15">
            <UserRound className="size-7" />
          </div>
          <h2 className="mt-6 text-2xl font-bold leading-tight">Join as a Patient</h2>
          <p className="mt-3 max-w-xs text-sm text-primary-foreground/80">
            Create an account to join live queues, run AI symptom triage, and track your wait from
            anywhere.
          </p>
        </div>
        <div className="relative flex items-center gap-2 rounded-xl bg-primary-foreground/10 px-4 py-3 text-xs font-medium text-primary-foreground/90">
          <ShieldCheck className="size-4 shrink-0" /> Reviewed and verified by hospital admin staff.
        </div>
      </div>

      {/* Form panel */}
      <div className="flex w-full flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-2xl">
          <h1 className="mb-8 text-center text-2xl font-bold tracking-tight text-foreground lg:text-left">
            Patient Registration
          </h1>
          <Stepper steps={steps} current={step} />

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
          {step === 0 ? (
            <form onSubmit={submitPersonal} className="space-y-5">
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label="Full Name">
                  <input
                    className={inputClass}
                    placeholder="Enter full name"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Phone Number (Myanmar)">
                  <input
                    className={inputClass}
                    placeholder="e.g. 09123456789"
                    value={form.phone}
                    onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    maxLength={11}
                    required
                  />
                  <span className="mt-1 block text-[11px] text-muted-foreground">
                    Must start with 09 with 9-11 digits total. Up to 3 family members can share one phone.
                  </span>
                </Field>
                <Field label="Password">
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      className={inputClass}
                      placeholder="Min 8 chars: upper, lower, number, special"
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                      minLength={8}
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </Field>
                <Field label="Date of Birth">
                  <input
                    type="date"
                    className={inputClass}
                    value={form.dateOfBirth}
                    onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Gender">
                  <select
                    className={inputClass}
                    value={form.gender}
                    onChange={(e) => setForm({ ...form, gender: e.target.value })}
                    required
                  >
                    <option value="">Select gender</option>
                    <option>Male</option>
                    <option>Female</option>
                    <option>Other</option>
                  </select>
                </Field>
                <Field label="Residential Address (Optional)">
                  <input
                    className={inputClass}
                    placeholder="e.g. No 12, Pyay Rd, Kamayut, Yangon"
                    value={form.address}
                    onChange={(e) => setForm({ ...form, address: e.target.value })}
                  />
                  <span className="mt-1 block text-[11px] text-muted-foreground">
                    If provided: include Street, Town/Township, and Myanmar Region.
                  </span>
                </Field>
              </div>

              <div className="rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
                <ShieldCheck className="mr-1 inline-block size-4 text-primary" />
                Your account details will be reviewed and verified by a hospital administrator
                before activation.
              </div>

              {error ? <p className="rounded-md bg-danger/10 p-2.5 text-sm text-danger">{error}</p> : null}

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-60"
              >
                {loading ? "Submitting Request..." : "Submit Registration Request"}
              </button>

              <p className="text-center text-sm text-muted-foreground">
                Already registered?{" "}
                <Link to="/login" className="font-medium text-primary hover:underline">
                  Sign in here
                </Link>
              </p>
            </form>
          ) : null}

          {step === 1 ? (
            <div className="py-8 text-center">
              <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Clock className="size-8" />
              </div>
              <h2 className="text-xl font-bold text-foreground">Request Submitted Successfully!</h2>
              <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">
                Your registration request for{" "}
                <span className="font-semibold text-foreground">{form.phone}</span> has been
                forwarded to the Hospital Administrator for approval.
              </p>
              <div className="mx-auto mt-4 max-w-md rounded-lg border border-border bg-muted/40 p-4 text-left text-xs text-muted-foreground">
                <p className="font-semibold text-foreground">What happens next?</p>
                <ul className="mt-1 list-disc space-y-1 pl-4">
                  <li>An administrator will review and verify your patient details.</li>
                  <li>Once approved, your account will be activated immediately.</li>
                  <li>You can then sign in directly using your phone number and password.</li>
                </ul>
              </div>
              <button
                onClick={() => navigate({ to: "/login" })}
                className="mt-6 rounded-lg bg-primary px-8 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
              >
                Go to Login Page
              </button>
            </div>
          ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
