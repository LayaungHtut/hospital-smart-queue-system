import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState, useCallback } from "react";
import {
  Activity,
  CheckCircle2,
  Play,
  Pause,
  RotateCcw,
  User,
  Volume2,
  Sparkles,
  FileText,
  Pill,
  HeartPulse,
  Copy,
  Check,
  Wand2,
  ChevronDown,
  ChevronUp,
  Stethoscope,
} from "lucide-react";
import {
  doctorCallNext,
  doctorCompleteConsultation,
  doctorPauseConsultation,
  doctorResumeConsultation,
  doctorStartConsultation,
  getDoctorDashboard,
  getDoctorProfile,
  toggleDoctorAvailability,
  generateAiSoapNote,
  type SoapNoteResponse,
} from "@/services/api";
import { DoctorLayout } from "@/components/portal/shells";
import { QueueTokenBadge, StatCard, StatusBadge } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonStatCard, SkeletonPanel } from "@/components/ui/loading";
import type { DoctorDashboard } from "@/types";

export const Route = createFileRoute("/doctor/")({
  head: () => ({
    meta: [
      { title: "Doctor Dashboard — Hospital Smart Queue" },
      {
        name: "description",
        content: "Doctor consultation suite, active patient queue, and call controls.",
      },
      { property: "og:title", content: "Doctor Dashboard — Hospital Smart Queue" },
      { property: "og:description", content: "Doctor consultation suite and queue dashboard." },
    ],
  }),
  component: DoctorDashboardPage,
});

function DoctorDashboardPage() {
  const { session } = useAuth();
  const [data, setData] = useState<DoctorDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [doctorId, setDoctorId] = useState<string | number>(
    session?.doctorId ?? session?.userId ?? "D001",
  );

  // AI SOAP Note Copilot state
  const [soapOpen, setSoapOpen] = useState(true);
  const [soapVitals, setSoapVitals] = useState("BP 120/80 mmHg, HR 76 bpm, Temp 36.8°C, SpO2 99%");
  const [soapObservations, setSoapObservations] = useState("");
  const [soapSymptoms, setSoapSymptoms] = useState("");
  const [soapLoading, setSoapLoading] = useState(false);
  const [soapNote, setSoapNote] = useState<SoapNoteResponse | null>(null);
  const [soapCopied, setSoapCopied] = useState(false);

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

  const loadDashboard = useCallback(async () => {
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
    loadDashboard();
    const timer = setInterval(loadDashboard, 10000);
    return () => clearInterval(timer);
  }, [loadDashboard]);

  async function handleGenerateSoap() {
    if (!activePatient) return;
    setSoapLoading(true);
    try {
      const sym = soapSymptoms || activePatient.type || "General consultation evaluation";
      const res = await generateAiSoapNote({
        patientName: activePatient.patientName,
        symptoms: sym,
        vitals: soapVitals,
        observations: soapObservations,
        ...(data?.doctor.department ? { department: data.doctor.department } : {}),
        ...(data?.doctor.specialization ? { specialization: data.doctor.specialization } : {}),
      });
      setSoapNote(res);
      setMessage("AI Clinical SOAP Note generated successfully!");
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      console.error(err);
      setMessage("Failed to generate AI SOAP note.");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setSoapLoading(false);
    }
  }

  function handleCopySoap() {
    if (!soapNote) return;
    const text = `CLINICAL SOAP NOTE\nPatient: ${activePatient?.patientName} (ID: ${activePatient?.patientId})\nDepartment: ${data?.doctor.department}\n\n[SUBJECTIVE]\n${soapNote.subjective}\n\n[OBJECTIVE]\n${soapNote.objective}\n\n[ASSESSMENT]\n${soapNote.assessment}\nICD-10: ${soapNote.icd10Codes?.join(", ")}\n\n[PLAN]\nMedications:\n${soapNote.medications?.map((m) => `- ${m.name} ${m.dosage} (${m.frequency}, ${m.duration}): ${m.instructions}`).join("\n")}\n\nRecommended Tests:\n${soapNote.recommendedTests?.map((t) => `- ${t}`).join("\n")}\n\nLifestyle Advice: ${soapNote.lifestyleAdvice}\nFollow-up: ${soapNote.followUp}`;
    navigator.clipboard.writeText(text);
    setSoapCopied(true);
    setTimeout(() => setSoapCopied(false), 3000);
  }

  async function handleToggleAvailability() {
    if (!data) return;
    setActionLoading(true);
    try {
      const res = await toggleDoctorAvailability(doctorId, !data.doctor.available);
      setData((prev) =>
        prev
          ? {
              ...prev,
              doctor: { ...prev.doctor, available: res.available },
            }
          : prev,
      );
      setMessage(`Doctor availability set to ${res.available ? "AVAILABLE" : "UNAVAILABLE"}`);
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleCallNext() {
    setActionLoading(true);
    try {
      const res = await doctorCallNext(doctorId);
      setMessage(res.message);
      await loadDashboard();
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
      await loadDashboard();
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
      await loadDashboard();
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
      await loadDashboard();
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
      await loadDashboard();
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Failed to resume consultation");
      setTimeout(() => setMessage(null), 4000);
    } finally {
      setActionLoading(false);
    }
  }

  const activePatient = data?.serving ?? data?.called;
  const isServing = !!data?.serving;
  const isCalled = !!data?.called && !data?.serving;

  if (loading && !data) {
    return (
      <DoctorLayout title="Doctor Dashboard">
        <div className="space-y-6">
          <SkeletonCard className="rounded-2xl p-6" />
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </div>
          <SkeletonCard className="rounded-2xl p-6" />
          <div className="grid gap-6 lg:grid-cols-3">
            <SkeletonPanel className="lg:col-span-2" />
            <SkeletonPanel />
          </div>
        </div>
      </DoctorLayout>
    );
  }

  return (
    <DoctorLayout title="Doctor Dashboard">
      <div className="space-y-6">
        {/* Top Header Card with Doctor Status & Availability */}
        <div className="relative overflow-hidden rounded-2xl border border-border bg-card p-6 shadow-sm">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-start gap-3 sm:gap-4">
              <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary sm:size-14">
                <User className="size-6 sm:size-8" />
              </div>
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-lg font-bold text-foreground sm:text-2xl">
                    {data?.doctor.name ?? session?.name ?? "Dr. Physician"}
                  </h1>
                  <span className="rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-semibold text-primary sm:px-3 sm:text-xs">
                    {data?.doctor.doctorCode ?? "D101"}
                  </span>
                  <span className="rounded-full bg-secondary px-2 py-0.5 text-[10px] font-medium text-secondary-foreground sm:px-3 sm:text-xs">
                    {data?.doctor.department ?? "General Medicine"}
                  </span>
                </div>
                <p className="mt-1 text-xs text-muted-foreground sm:text-sm">
                  {data?.doctor.specialization ?? "Consultant Physician"} • Average Consultation
                  Time: {data?.doctor.averageConsultationMinutes ?? 15} min
                </p>
              </div>
            </div>

            {/* Status Indicator and Availability Switch */}
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <button
                onClick={handleToggleAvailability}
                disabled={actionLoading}
                className={`flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold transition-colors shadow-sm sm:px-4 sm:py-2.5 sm:text-sm ${
                  data?.doctor.available
                    ? "bg-success text-success-foreground hover:opacity-90"
                    : "bg-danger text-danger-foreground hover:opacity-90"
                }`}
              >
                <span className="size-2 rounded-full bg-white animate-pulse sm:size-2.5" />
                <span className="hidden sm:inline">
                  {data?.doctor.available
                    ? "Status: AVAILABLE (Online)"
                    : "Status: ON BREAK (Offline)"}
                </span>
                <span className="sm:hidden">
                  {data?.doctor.available ? "AVAILABLE" : "ON BREAK"}
                </span>
              </button>

              <button
                onClick={handleCallNext}
                disabled={actionLoading || isServing || isCalled || !data?.doctor.available}
                className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-bold text-primary-foreground shadow-md transition-transform hover:scale-[1.02] disabled:opacity-50 sm:px-5 sm:py-2.5 sm:text-sm"
              >
                <Volume2 className="size-4" />
                <span className="hidden sm:inline">Call Next Patient</span>
                <span className="sm:hidden">Call Next</span>
              </button>
            </div>
          </div>

          {message && (
            <div className="mt-4 flex items-center gap-2 rounded-lg bg-primary/10 px-4 py-2 text-sm font-medium text-primary">
              <Sparkles className="size-4 shrink-0" />
              {message}
            </div>
          )}
        </div>

        {/* 4 Stat Cards */}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Total Patients Today"
            value={data?.stats.totalPatientsToday ?? "—"}
            tone="primary"
          />
          <StatCard
            label="Patients Waiting"
            value={data?.stats.waitingCount ?? "—"}
            tone="warning"
          />
          <StatCard
            label="Completed Consultations"
            value={data?.stats.completedCount ?? "—"}
            tone="success"
          />
          <StatCard
            label="Avg. Consultation Time"
            value={`${data?.stats.averageConsultationMinutes ?? 15} min`}
            tone="primary"
          />
        </div>

        {/* Consultation Room Active Box */}
        <div className="rounded-2xl border border-primary/30 bg-card p-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-border pb-4">
            <div className="flex items-center gap-2.5">
              <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <Activity className="size-5" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-foreground">
                  Consultation Room Active Patient
                </h2>
                <p className="text-xs text-muted-foreground">
                  Manage currently called and in-consultation patients
                </p>
              </div>
            </div>
            {activePatient && (
              <span
                className={`rounded-full px-3 py-1 text-xs font-bold ${
                  isServing
                    ? "bg-success/20 text-success animate-pulse"
                    : "bg-warning/20 text-warning"
                }`}
              >
                {isServing ? "● IN CONSULTATION" : "● CALLED — WAITING FOR PATIENT"}
              </span>
            )}
          </div>

          <div className="mt-6">
            {activePatient ? (
              <div className="space-y-4">
                <div className="flex flex-col gap-4 rounded-xl border border-border bg-accent/40 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-6">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                    <QueueTokenBadge
                      token={activePatient.queueNumber}
                      station={isServing ? "In Consultation" : "Called · Awaiting Arrival"}
                      urgency={activePatient.emergency ? "critical" : "general"}
                    />
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-base font-bold text-foreground sm:text-lg">
                          {activePatient.patientName}
                        </span>
                        <span className="text-xs text-muted-foreground font-medium">
                          (Patient ID: {activePatient.patientId})
                        </span>
                      </div>
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="font-semibold text-muted-foreground">Priority:</span>
                        <StatusBadge status={activePatient.type ?? "NORMAL"} />
                        {activePatient.emergency && (
                          <span className="rounded bg-danger/20 px-2 py-0.5 font-bold text-danger">
                            EMERGENCY
                          </span>
                        )}
                        <span className="text-muted-foreground sm:ml-3">
                          Waiting: {activePatient.estimatedWaitingMinutes ?? 15} min
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Action Buttons for Active Patient */}
                  <div className="flex flex-wrap items-center gap-3">
                    {isCalled && (
                      <button
                        onClick={handleStart}
                        disabled={actionLoading}
                        className="flex items-center gap-2 rounded-xl bg-success px-5 py-2.5 text-sm font-bold text-success-foreground hover:opacity-90 shadow-sm"
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
                          className="flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground hover:opacity-90 shadow-sm"
                        >
                          <CheckCircle2 className="size-4" />
                          Complete Consultation
                        </button>
                        <button
                          onClick={handlePause}
                          disabled={actionLoading}
                          className="flex items-center gap-2 rounded-xl bg-secondary px-4 py-2.5 text-sm font-semibold text-secondary-foreground hover:bg-secondary/80"
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
                        className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
                      >
                        <RotateCcw className="size-4" />
                        Resume
                      </button>
                    )}
                  </div>
                </div>

                {/* AI Clinical Copilot & SOAP Note Generator */}
                <div className="mt-4 rounded-xl border border-primary/20 bg-card overflow-hidden shadow-sm">
                  <div
                    onClick={() => setSoapOpen(!soapOpen)}
                    className="flex cursor-pointer items-center justify-between bg-primary/5 px-5 py-3.5 transition-colors hover:bg-primary/10"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className="flex size-7 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                        <Wand2 className="size-4" />
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
                          AI Clinical Copilot & SOAP Note Assistant
                          <span className="rounded bg-primary/20 px-2 py-0.5 text-[10px] font-bold text-primary">
                            FREE AI
                          </span>
                        </h3>
                        <p className="text-xs text-muted-foreground">
                          Generate structured SOAP notes, ICD-10 coding, and prescription templates
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <span className="text-xs">{soapOpen ? "Hide" : "Show"} Copilot</span>
                      {soapOpen ? (
                        <ChevronUp className="size-4" />
                      ) : (
                        <ChevronDown className="size-4" />
                      )}
                    </div>
                  </div>

                  {soapOpen && (
                    <div className="space-y-4 p-4 sm:p-5">
                      <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
                        <div>
                          <label className="text-xs font-semibold text-foreground">
                            Symptoms / Complaints
                          </label>
                          <input
                            type="text"
                            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                            placeholder={activePatient.type || "e.g. Chest tightness for 2 days"}
                            value={soapSymptoms}
                            onChange={(e) => setSoapSymptoms(e.target.value)}
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-foreground">
                            Vital Signs
                          </label>
                          <input
                            type="text"
                            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                            value={soapVitals}
                            onChange={(e) => setSoapVitals(e.target.value)}
                          />
                        </div>
                        <div>
                          <label className="text-xs font-semibold text-foreground">
                            Doctor Observations / Findings
                          </label>
                          <input
                            type="text"
                            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
                            placeholder="e.g. Clear breath sounds, no edema"
                            value={soapObservations}
                            onChange={(e) => setSoapObservations(e.target.value)}
                          />
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <Stethoscope className="size-3.5 text-primary" />
                          <span className="hidden sm:inline">Patient:</span>{" "}
                          <strong>{activePatient.patientName}</strong>{" "}
                          <span className="hidden sm:inline">({data?.doctor.department})</span>
                        </div>
                        <div className="flex items-center gap-2">
                          {soapNote && (
                            <button
                              type="button"
                              onClick={handleCopySoap}
                              className="flex items-center gap-1.5 rounded-lg border border-border bg-secondary px-3 py-1.5 text-xs font-semibold text-secondary-foreground hover:bg-secondary/80"
                            >
                              {soapCopied ? (
                                <Check className="size-3.5 text-success" />
                              ) : (
                                <Copy className="size-3.5" />
                              )}
                              {soapCopied ? "Copied!" : "Copy Full Note"}
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={handleGenerateSoap}
                            disabled={soapLoading}
                            className="flex items-center gap-1.5 rounded-lg bg-primary px-4 py-1.5 text-xs font-bold text-primary-foreground shadow hover:opacity-90 disabled:opacity-50"
                          >
                            <Sparkles className="size-3.5" />
                            {soapLoading ? "Generating SOAP Note..." : "✨ Generate AI SOAP Note"}
                          </button>
                        </div>
                      </div>

                      {/* Formatted SOAP Result */}
                      {soapNote && (
                        <div className="mt-4 grid gap-3 rounded-xl border border-border bg-accent/30 p-3 sm:grid-cols-2 sm:p-4">
                          {/* Subjective */}
                          <div className="rounded-lg bg-card p-3 border border-border space-y-1.5">
                            <div className="flex items-center gap-1.5 text-xs font-bold text-primary">
                              <FileText className="size-3.5" />
                              [S] SUBJECTIVE
                            </div>
                            <p className="text-xs text-foreground leading-relaxed">
                              {soapNote.subjective}
                            </p>
                          </div>

                          {/* Objective */}
                          <div className="rounded-lg bg-card p-3 border border-border space-y-1.5">
                            <div className="flex items-center gap-1.5 text-xs font-bold text-primary">
                              <HeartPulse className="size-3.5" />
                              [O] OBJECTIVE
                            </div>
                            <p className="text-xs text-foreground leading-relaxed">
                              {soapNote.objective}
                            </p>
                          </div>

                          {/* Assessment */}
                          <div className="rounded-lg bg-card p-3 border border-border space-y-2">
                            <div className="flex items-center gap-1.5 text-xs font-bold text-primary">
                              <CheckCircle2 className="size-3.5" />
                              [A] ASSESSMENT & DIAGNOSIS
                            </div>
                            <p className="text-xs text-foreground leading-relaxed">
                              {soapNote.assessment}
                            </p>
                            <div className="flex flex-wrap gap-1.5 pt-1">
                              {soapNote.icd10Codes?.map((code, idx) => (
                                <span
                                  key={idx}
                                  className="rounded bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary border border-primary/20"
                                >
                                  {code}
                                </span>
                              ))}
                            </div>
                          </div>

                          {/* Plan */}
                          <div className="rounded-lg bg-card p-3 border border-border space-y-2">
                            <div className="flex items-center gap-1.5 text-xs font-bold text-primary">
                              <Pill className="size-3.5" />
                              [P] PLAN & PRESCRIPTIONS
                            </div>
                            <div className="space-y-1">
                              {soapNote.medications?.map((med, idx) => (
                                <div
                                  key={idx}
                                  className="text-xs text-foreground bg-accent/40 px-2 py-1 rounded"
                                >
                                  <strong>{med.name}</strong> {med.dosage} — {med.frequency} (
                                  {med.duration})
                                  <div className="text-[10px] text-muted-foreground">
                                    {med.instructions}
                                  </div>
                                </div>
                              ))}
                            </div>
                            {soapNote.recommendedTests?.length > 0 && (
                              <div className="pt-1 text-[11px] text-muted-foreground">
                                <strong>Tests:</strong> {soapNote.recommendedTests.join(", ")}
                              </div>
                            )}
                            <div className="text-[11px] text-muted-foreground">
                              <strong>Follow-up:</strong> {soapNote.followUp}
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-10 text-center">
                <div className="flex size-14 items-center justify-center rounded-full bg-secondary text-muted-foreground">
                  <Volume2 className="size-7" />
                </div>
                <h3 className="mt-3 text-base font-bold text-foreground">
                  No Active Patient in Room
                </h3>
                <p className="mt-1 max-w-sm text-xs text-muted-foreground">
                  {data?.waiting && data.waiting.length > 0
                    ? `${data.waiting.length} patients currently waiting in line. Click below to call the next patient.`
                    : "No patients currently in queue. You are all caught up."}
                </p>
                {data?.waiting && data.waiting.length > 0 && data.doctor.available && (
                  <button
                    onClick={handleCallNext}
                    disabled={actionLoading}
                    className="mt-4 flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground shadow hover:opacity-90"
                  >
                    <Volume2 className="size-4" />
                    Call Next Patient ({data.waiting[0]?.queueNumber ?? ""})
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </DoctorLayout>
  );
}
