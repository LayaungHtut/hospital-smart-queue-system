import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Activity,
  ArrowRight,
  BadgeCheck,
  BellOff,
  Bot,
  Building2,
  CalendarClock,
  Languages,
  MessageCircle,
  Phone,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Timer,
  UserRound,
  Users,
  Wifi,
} from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Hospital Smart Queue System" },
      {
        name: "description",
        content:
          "Smart hospital queue management for patients, doctors, front-desk staff and administrators — book queues, monitor doctors and manage departments.",
      },
      { property: "og:title", content: "Hospital Smart Queue System" },
      {
        property: "og:description",
        content: "Book queues, track waiting times, and manage hospital operations in real time.",
      },
    ],
  }),
  component: Landing,
});

const departments = [
  {
    name: "Cardiology",
    tag: "Specialty OPD",
    token: "A-042",
    waiting: 3,
    wait: "~14 mins",
    stroke: "bg-secondary",
  },
  {
    name: "Pediatrics",
    tag: "Family Care",
    token: "P-019",
    waiting: 5,
    wait: "~22 mins",
    stroke: "bg-success",
  },
  {
    name: "Orthopedics",
    tag: "Bone & Joint",
    token: "O-088",
    waiting: 1,
    wait: "~6 mins",
    stroke: "bg-primary",
  },
  {
    name: "General OPD",
    tag: "Fast Triage",
    token: "G-142",
    waiting: 7,
    wait: "~18 mins",
    stroke: "bg-warning",
  },
];

const portals = [
  {
    title: "Patient & Caregiver Portal",
    kicker: "Self-Service Module",
    description:
      "Join a live queue, run AI symptom triage, watch your wait countdown, and manage appointments from your phone.",
    to: "/login",
    icon: UserRound,
    cta: "Enter Patient Portal",
    span: "lg:col-span-7",
    tone: "light" as const,
  },
  {
    title: "Doctor & Specialist Desk",
    kicker: "Clinical Console",
    description:
      "Call the next patient, manage consultation state, and get AI-assisted triage notes without breaking pace.",
    to: "/doctor/login",
    icon: Stethoscope,
    cta: "Launch Clinical Desk",
    span: "lg:col-span-5",
    tone: "dark" as const,
  },
  {
    title: "Staff & Triage Operations",
    kicker: "Front-Desk Floor",
    description:
      "Issue tokens, flag priority patients, fast-track emergencies, and balance load across doctors.",
    to: "/staff/login",
    icon: Users,
    cta: "Open Operations Desk",
    span: "lg:col-span-6",
    tone: "light" as const,
  },
  {
    title: "Hospital Admin & Analytics",
    kicker: "Facility Level",
    description:
      "Monitor bottlenecks, forecast peak-hour staffing, review doctor performance, and export compliance reports.",
    to: "/admin/login",
    icon: ShieldCheck,
    cta: "Admin Analytics",
    span: "lg:col-span-6",
    tone: "light" as const,
  },
];

const stats = [
  {
    icon: Timer,
    value: "94%",
    label: "Wait Time Reduction",
    detail: "Dynamic triage and live queueing cut average idle time from ~95 to ~18 minutes.",
    tone: "text-secondary",
  },
  {
    icon: Bot,
    value: "24/7",
    label: "AI Symptom Triage",
    detail:
      "Instant department recommendation and emergency flagging before a patient even joins the line.",
    tone: "text-success",
  },
  {
    icon: Wifi,
    value: "100%",
    label: "Live Sync",
    detail: "Every counter, doctor console and TV display updates in real time as the queue moves.",
    tone: "text-primary",
  },
];

function Landing() {
  return (
    <div className="min-h-screen bg-background">
      {/* Utility bar */}
      <div className="border-b border-border bg-card/80">
        <div className="mx-auto flex max-w-360 flex-wrap items-center justify-between gap-2 px-4 py-2 text-xs text-muted-foreground sm:px-6">
          <div className="flex items-center gap-2 font-medium text-foreground">
            <Building2 className="size-3.5 text-primary" /> City General Hospital
            <span className="hidden h-3 w-px bg-border sm:block" />
            <span className="hidden items-center gap-1.5 text-success sm:flex">
              <span className="size-1.5 animate-pulse rounded-full bg-success" /> Live Queue Sync
              Active
            </span>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-danger-soft px-2.5 py-1 font-semibold text-danger">
            <Phone className="size-3.5" /> Emergency Hotline: 199
          </div>
        </div>
      </div>

      {/* Hero */}
      <section className="mx-auto max-w-360 px-4 pb-10 pt-14 sm:px-6 sm:pt-20">
        <div className="grid gap-10 lg:grid-cols-12 lg:items-center">
          <div className="lg:col-span-7">
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              <Sparkles className="size-3.5 text-primary" /> CareFlow Queue OS
            </span>
            <h1 className="mt-5 text-3xl font-bold leading-tight tracking-tight text-foreground sm:text-4xl lg:text-5xl">
              Hassle-free, human-centered hospital care.
            </h1>
            <p className="mt-4 max-w-xl text-sm text-muted-foreground sm:text-base">
              One unified platform for patients, doctors, staff, and administrators — replacing
              crowded corridors and shouted names with calm, real-time queue orchestration.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/login"
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground shadow-sm transition hover:opacity-90"
              >
                Get Started <ArrowRight className="size-4" />
              </Link>
              <a
                href="tel:199"
                className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-5 py-3 text-sm font-semibold text-foreground transition hover:bg-muted"
              >
                <Phone className="size-4" /> Call Emergency Hotline
              </a>
            </div>
          </div>

          {/* Now-consulting snapshot card */}
          <div className="lg:col-span-5">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-md">
              <div className="flex items-center justify-between rounded-xl bg-muted/60 p-4">
                <div className="flex items-center gap-3">
                  <div className="flex size-12 flex-col items-center justify-center rounded-xl border border-border bg-card shadow-sm">
                    <span className="text-[10px] uppercase text-muted-foreground">Room</span>
                    <span className="text-lg font-bold leading-none text-primary">302</span>
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-secondary">
                        Now Consulting
                      </span>
                      <span className="size-1.5 rounded-full bg-secondary" />
                    </div>
                    <p className="text-sm font-bold text-foreground">Dr. Thet Naing Win</p>
                    <p className="text-xs text-muted-foreground">
                      Calling <span className="font-bold text-primary">A-042</span> · 8 mins elapsed
                    </p>
                  </div>
                </div>
                <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-bold uppercase text-primary">
                  Counter 04
                </span>
              </div>
              <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <BellOff className="size-3.5 text-primary" /> Silent visual callers, no megaphones
                </span>
                <span className="flex items-center gap-1.5">
                  <MessageCircle className="size-3.5 text-success" /> SMS alerts
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Live department ticker */}
      <section className="border-y border-border bg-muted/40 py-8">
        <div className="mx-auto max-w-360 px-4 sm:px-6">
          <div className="mb-4 flex items-center justify-between">
            <span className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-foreground">
              <Activity className="size-4 text-primary" /> Live Department Consultations
            </span>
            <span className="hidden items-center gap-1.5 text-xs text-muted-foreground sm:flex">
              <span className="size-1.5 animate-pulse rounded-full bg-success" /> Updated just now
            </span>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {departments.map((d) => (
              <div
                key={d.name}
                className="relative overflow-hidden rounded-xl border border-border bg-card p-4 shadow-sm"
              >
                <span className={`absolute inset-y-0 left-0 w-1 ${d.stroke}`} aria-hidden />
                <div className="flex items-start justify-between pl-2">
                  <div>
                    <span className="text-[10px] font-bold uppercase text-muted-foreground">
                      {d.tag}
                    </span>
                    <h3 className="text-base font-bold text-foreground">{d.name}</h3>
                  </div>
                  <span className="rounded-full bg-warning-soft px-2 py-0.5 text-[10px] font-bold text-warning">
                    {d.waiting} Waiting
                  </span>
                </div>
                <div className="mt-3 flex items-center justify-between border-t border-border pl-2 pt-3">
                  <div>
                    <span className="block text-[10px] uppercase text-muted-foreground">
                      Now Serving
                    </span>
                    <span className="text-lg font-extrabold text-primary">{d.token}</span>
                  </div>
                  <div className="text-right">
                    <span className="block text-[10px] uppercase text-muted-foreground">
                      Est. Wait
                    </span>
                    <span className="text-sm font-bold text-foreground">{d.wait}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Portal entry hub */}
      <section className="mx-auto max-w-360 px-4 py-16 sm:px-6">
        <div className="mb-8 flex flex-col justify-between gap-3 md:flex-row md:items-end">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-secondary">
              Unified Care Ecosystem
            </span>
            <h2 className="mt-1 text-2xl font-bold text-foreground sm:text-3xl">
              Select your workspace or patient gateway
            </h2>
          </div>
          <p className="max-w-md text-sm text-muted-foreground">
            Dedicated role-tailored environments for patients, doctors, front-desk staff, and
            hospital administrators.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
          {portals.map((portal) => {
            const Icon = portal.icon;
            const dark = portal.tone === "dark";
            return (
              <Link
                key={portal.to}
                to={portal.to}
                className={`group relative flex flex-col justify-between overflow-hidden rounded-2xl border p-7 shadow-md transition-all hover:shadow-xl ${portal.span} ${
                  dark
                    ? "border-transparent bg-primary text-primary-foreground"
                    : "border-border bg-card text-foreground"
                }`}
              >
                <div>
                  <div className="mb-4 flex items-center justify-between">
                    <span
                      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase ${
                        dark ? "bg-primary-foreground/15" : "bg-accent text-accent-foreground"
                      }`}
                    >
                      <Icon className="size-3.5" /> {portal.kicker}
                    </span>
                  </div>
                  <h3 className={`text-xl font-bold ${dark ? "" : "group-hover:text-primary"}`}>
                    {portal.title}
                  </h3>
                  <p
                    className={`mt-2 max-w-lg text-sm ${dark ? "text-primary-foreground/80" : "text-muted-foreground"}`}
                  >
                    {portal.description}
                  </p>
                </div>
                <div
                  className={`mt-8 flex items-center justify-between border-t pt-4 text-sm font-semibold ${
                    dark ? "border-primary-foreground/20" : "border-border"
                  }`}
                >
                  <span
                    className={
                      dark
                        ? "text-primary-foreground/70 font-normal"
                        : "text-muted-foreground font-normal"
                    }
                  >
                    Continue as this role
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    {portal.cta}{" "}
                    <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" />
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      </section>

      {/* Trust / stats */}
      <section className="border-y border-border bg-muted/40 py-16">
        <div className="mx-auto max-w-360 px-4 sm:px-6">
          <div className="mx-auto mb-10 max-w-2xl text-center">
            <span className="text-xs font-bold uppercase tracking-widest text-primary">
              Built for Real Clinical Realities
            </span>
            <h2 className="mt-1 text-2xl font-bold text-foreground sm:text-3xl">
              Why hospitals trust this queue system
            </h2>
          </div>
          <div className="grid gap-6 md:grid-cols-3">
            {stats.map((s) => {
              const Icon = s.icon;
              return (
                <div
                  key={s.label}
                  className="rounded-2xl border border-border bg-card p-6 shadow-sm"
                >
                  <div className="mb-4 flex size-11 items-center justify-center rounded-xl bg-accent text-accent-foreground">
                    <Icon className="size-5" />
                  </div>
                  <div className={`text-3xl font-extrabold tracking-tight ${s.tone}`}>
                    {s.value}
                  </div>
                  <h3 className="mt-1 text-base font-bold text-foreground">{s.label}</h3>
                  <p className="mt-2 text-sm text-muted-foreground">{s.detail}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA banner */}
      <section className="mx-auto max-w-360 px-4 py-16 sm:px-6">
        <div className="relative flex flex-col items-center justify-between gap-6 overflow-hidden rounded-2xl bg-primary p-8 text-primary-foreground shadow-lg md:flex-row md:p-12">
          <div className="max-w-xl">
            <span className="text-xs font-bold uppercase tracking-widest text-primary-foreground/70">
              Onboarding &amp; Demo Support
            </span>
            <h3 className="mt-1 text-2xl font-bold">Ready to bring calm to your waiting room?</h3>
            <p className="mt-2 text-sm text-primary-foreground/80">
              Sign in to your role's portal above, or reach the triage hotline for hospital-wide
              support.
            </p>
          </div>
          <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
            <Link
              to="/register"
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-card px-6 py-3 text-sm font-bold text-primary shadow-md transition hover:bg-muted sm:w-auto"
            >
              <CalendarClock className="size-4" /> Create Patient Account
            </Link>
            <a
              href="tel:199"
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-primary-foreground/30 bg-primary-foreground/10 px-6 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary-foreground/20 sm:w-auto"
            >
              <Phone className="size-4" /> Call Triage Hotline
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border bg-muted/40 py-6">
        <div className="mx-auto flex max-w-360 flex-col items-center justify-between gap-3 px-4 text-xs text-muted-foreground sm:flex-row sm:px-6">
          <div className="flex items-center gap-2">
            <BadgeCheck className="size-4 text-primary" />
            <span className="font-semibold text-foreground">
              City General Hospital — CareFlow Queue OS
            </span>
          </div>
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5">
              <Languages className="size-3.5" /> EN / MM
            </span>
            <span>© {new Date().getFullYear()} All rights reserved.</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
