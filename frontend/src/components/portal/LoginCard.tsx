import { Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Eye, EyeOff, ShieldCheck, Sparkles, Stethoscope, UserRound, Users } from "lucide-react";
import { loginUser, getDoctors } from "@/services/api";
import { useAuth } from "@/lib/auth";
import { Field, inputClass } from "./ui-kit";
import { cn } from "@/lib/utils";
import type { UserRole } from "@/types";

const roleBrand: Record<UserRole, { icon: typeof UserRound; blurb: string; highlight: string }> = {
  PATIENT: {
    icon: UserRound,
    blurb: "Join a live queue, run AI symptom triage, and track your wait in real time.",
    highlight: "Zero install — works directly on mobile web.",
  },
  DOCTOR: {
    icon: Stethoscope,
    blurb: "Call the next patient, manage consultation state, and get AI-assisted triage notes.",
    highlight: "Built for high-speed clinical turnaround.",
  },
  STAFF: {
    icon: Users,
    blurb: "Issue tokens, flag priority patients, and fast-track emergencies from the front desk.",
    highlight: "Includes emergency reassignment tools.",
  },
  ADMIN: {
    icon: ShieldCheck,
    blurb: "Monitor bottlenecks, manage doctors and departments, and export compliance reports.",
    highlight: "Facility-wide analytics and configuration.",
  },
};

export function LoginCard({
  role,
  title,
  subtitle,
  redirectTo,
  footer,
}: {
  role: UserRole;
  title: string;
  subtitle: string;
  redirectTo: string;
  footer?: React.ReactNode;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hospitalName, setHospitalName] = useState("CareQueue General Hospital");
  const [logoUrl, setLogoUrl] = useState("");
  const { session, signIn } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const savedName = localStorage.getItem("system_hospital_name");
    const savedLogo = localStorage.getItem("system_logo_url");
    if (savedName) setHospitalName(savedName);
    if (savedLogo) setLogoUrl(savedLogo);
  }, []);

  // If already signed in with this role, redirect to dashboard
  useEffect(() => {
    if (session && session.role === role) {
      navigate({ to: redirectTo, replace: true });
    }
  }, [session, role, redirectTo, navigate]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const authSession = await loginUser({ username, password, role });

      if (role === "DOCTOR") {
        authSession.doctorId = authSession.doctorId ?? authSession.userId;
      }

      signIn(authSession);
      navigate({ to: redirectTo, replace: true });
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Unable to sign in. Please check your credentials.",
      );
    } finally {
      setLoading(false);
    }
  }

  const brand = roleBrand[role];
  const BrandIcon = brand.icon;

  return (
    <div
      className={cn(
        "flex min-h-screen items-stretch bg-background",
        role === "STAFF" && "theme-staff",
      )}
    >
      {/* Branded side panel */}
      <div className="relative hidden w-full max-w-md flex-col justify-between overflow-hidden bg-primary p-10 text-primary-foreground lg:flex">
        <div
          className="absolute -right-16 -top-16 size-64 rounded-full bg-primary-foreground/10 blur-3xl"
          aria-hidden
        />
        <div className="relative">
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary-foreground/15">
              <Sparkles className="size-4.5" />
            </div>
            <span className="text-sm font-bold uppercase tracking-wider">{hospitalName}</span>
          </div>
          <div className="mt-10 flex size-14 items-center justify-center rounded-2xl bg-primary-foreground/15">
            <BrandIcon className="size-7" />
          </div>
          <h2 className="mt-6 text-2xl font-bold leading-tight">{title}</h2>
          <p className="mt-3 max-w-xs text-sm text-primary-foreground/80">{brand.blurb}</p>
        </div>
        <div className="relative flex items-center gap-2 rounded-xl bg-primary-foreground/10 px-4 py-3 text-xs font-medium text-primary-foreground/90">
          <ShieldCheck className="size-4 shrink-0" /> {brand.highlight}
        </div>
      </div>

      {/* Form panel */}
      <div className="flex w-full flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-md">
          <div className="mb-6 text-center lg:text-left">
            {logoUrl ? (
              <img
                src={logoUrl}
                alt="Logo"
                className="mx-auto mb-3 size-14 rounded-xl border border-border bg-card object-contain p-1 shadow-sm lg:mx-0"
              />
            ) : (
              <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-xl bg-primary text-xl font-bold text-primary-foreground shadow-sm lg:mx-0 lg:hidden">
                +
              </div>
            )}
            <p className="mb-1 text-xs font-bold uppercase tracking-wider text-primary lg:hidden">
              {hospitalName}
            </p>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{title}</h1>
            <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>
          </div>

          <form
            onSubmit={handleSubmit}
            className="space-y-4 rounded-xl border border-border bg-card p-6 shadow-sm"
          >
            <Field label={role === "PATIENT" ? "Phone Number" : "Username or Email"}>
              <input
                className={inputClass}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder={role === "PATIENT" ? "Enter phone number" : "Enter username"}
                required
              />
            </Field>
            <Field label="Password">
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  className={inputClass}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
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

            {error ? <p className="text-sm text-danger">{error}</p> : null}

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-60"
            >
              {loading ? "Signing in..." : "Login"}
            </button>

            {footer}
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground lg:text-left">
            <Link to="/" className="hover:text-foreground">
              ← Back to portal selection
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
