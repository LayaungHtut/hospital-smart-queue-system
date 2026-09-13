import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  Sparkles,
  MessageSquare,
  FlaskConical,
  Activity,
} from "lucide-react";
import {
  addPatientToQueue,
  getAIRecommendation,
  getDepartments,
  getDoctorsByDepartment,
  getSymptoms,
  predictWaitTime,
  predictWaitTimeForDepartment,
  getInteractiveTriageQuestions,
  finalizeInteractiveTriage,
  type TriageQuestion,
  type FinalizeTriageResponse,
} from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import { Field, Panel, QueueTokenBadge, Stepper, inputClass } from "@/components/portal/ui-kit";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";
import type { Department, DoctorDetail, QueueEntry, RecommendationResult, Symptom } from "@/types";

export const Route = createFileRoute("/queue/new")({
  head: () => ({
    meta: [
      { title: "AI Symptom Checker & Queue — Hospital Smart Queue" },
      {
        name: "description",
        content: "Describe symptoms to get an AI department recommendation and join the queue.",
      },
      { property: "og:title", content: "AI Symptom Checker & Queue" },
      {
        property: "og:description",
        content: "Smart AI triage and hospital queue registration.",
      },
    ],
  }),
  component: NewQueuePage,
});

const steps = ["AI Symptom Checker", "Department", "Doctor", "Confirm"];

const QUICK_SYMPTOMS = [
  "Severe chest pain and shortness of breath",
  "High fever and persistent cough",
  "Sudden severe headache and dizziness",
  "Stomach ache and nausea",
  "Skin rash and allergic itching",
  "Joint pain and difficulty walking",
  "Child fever with weakness",
];

function NewQueuePage() {
  const { session } = useAuth();
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [symptomsList, setSymptomsList] = useState<Symptom[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<DoctorDetail[]>([]);

  // Symptom input & AI state
  const [symptomText, setSymptomText] = useState("");
  const [selectedSymptomChips, setSelectedSymptomChips] = useState<string[]>([]);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<RecommendationResult | null>(null);

  // Interactive Conversational Triage state
  const [triageMode, setTriageMode] = useState<"interactive" | "quick">("interactive");
  const [interactiveStep, setInteractiveStep] = useState<"input" | "questions" | "result">("input");
  const [interactiveQuestions, setInteractiveQuestions] = useState<TriageQuestion[]>([]);
  const [interactiveAnswers, setInteractiveAnswers] = useState<Record<string, string>>({});
  const [interactiveResult, setInteractiveResult] = useState<FinalizeTriageResponse | null>(null);
  const [interactiveLoading, setInteractiveLoading] = useState(false);

  // Queue parameters
  const [departmentId, setDepartmentId] = useState<number | null>(null);
  const [doctorId, setDoctorId] = useState<string | number | null>(null);
  const [isEmergency, setIsEmergency] = useState(false);
  const [created, setCreated] = useState<QueueEntry | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [waitTimePredictions, setWaitTimePredictions] = useState<Record<string, number>>({});

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    getSymptoms().then(setSymptomsList).catch(console.error);
    getDepartments()
      .then(setDepartments)
      .catch((err) => setErrorMessage(err.message));
  }, []);

  useEffect(() => {
    if (departmentId == null) return;
    setDoctors([]);
    getDoctorsByDepartment(departmentId)
      .then((list) => {
        setDoctors(list);
        setDoctorId(list[0]?.id ?? null);
      })
      .catch((err) => setErrorMessage(err.message));

    // Fetch ML-based wait time predictions for all doctors in this department
    if (session?.userId) {
      const dept = departments.find((d) => d.id === departmentId);
      if (dept?.departmentCode) {
        predictWaitTimeForDepartment(dept.departmentCode, session.userId)
          .then((predictions) => {
            setWaitTimePredictions(predictions);
          })
          .catch(console.error);
      }
    }
  }, [departmentId, departments, session?.userId]);

  async function handleAnalyzeAI(customText?: string) {
    const textToAnalyze = (customText ?? symptomText).trim();
    if (!textToAnalyze) return;

    setErrorMessage(null);
    setAiLoading(true);
    try {
      const res = await getAIRecommendation({ symptoms: textToAnalyze });
      setAiResult(res);
      if (res.department?.id) {
        setDepartmentId(res.department.id);
      }
      setIsEmergency(res.emergency);
    } catch (err: unknown) {
      setErrorMessage(
        err instanceof Error
          ? err.message
          : "Failed to analyze symptoms. Please select department manually.",
      );
    } finally {
      setAiLoading(false);
    }
  }

  async function handleStartInteractiveTriage(customText?: string) {
    const text = (customText ?? symptomText).trim();
    if (!text) return;
    setErrorMessage(null);
    setInteractiveLoading(true);
    try {
      const res = await getInteractiveTriageQuestions(text);
      setInteractiveQuestions(res.questions || []);
      setInteractiveAnswers({});
      setInteractiveStep("questions");
    } catch (err) {
      console.error(err);
      handleAnalyzeAI(text);
    } finally {
      setInteractiveLoading(false);
    }
  }

  function handleSelectAnswer(questionId: string, answer: string) {
    setInteractiveAnswers((prev) => ({ ...prev, [questionId]: answer }));
  }

  async function handleFinalizeInteractive() {
    setInteractiveLoading(true);
    setErrorMessage(null);
    try {
      const res = await finalizeInteractiveTriage({
        symptoms: symptomText,
        answers: interactiveAnswers,
      });
      setInteractiveResult(res);
      const targetDeptId = res.department?.departmentId ?? res.department?.id;
      if (targetDeptId) {
        setDepartmentId(Number(targetDeptId));
      }
      setIsEmergency(res.emergency);
      setInteractiveStep("result");
    } catch (err) {
      console.error(err);
      handleAnalyzeAI();
    } finally {
      setInteractiveLoading(false);
    }
  }

  function toggleChip(chip: string) {
    setSelectedSymptomChips((prev) => {
      const next = prev.includes(chip) ? prev.filter((c) => c !== chip) : [...prev, chip];
      setSymptomText(next.join(", "));
      return next;
    });
  }

  async function confirm() {
    if (!departmentId || !doctorId) return;
    setSubmitting(true);
    setErrorMessage(null);
    try {
      const selectedSymptomIds = symptomsList
        .filter((s) => selectedSymptomChips.includes(s.name))
        .map((s) => s.id);
      const entry = await addPatientToQueue({
        patientId: session?.userId ?? "P001",
        symptomIds: selectedSymptomIds.length > 0 ? selectedSymptomIds : [1],
        departmentId: Number(departmentId),
        doctorId: doctorId,
        emergency: isEmergency,
      });
      setCreated(entry);
    } catch (err: unknown) {
      setErrorMessage(
        (err instanceof Error ? err.message : null) ||
          "Could not register queue. You may already have an active queue or the clinic is closed.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (created) {
    return (
      <PatientLayout title="New Queue">
        <Panel className="mx-auto max-w-3xl">
          <h2 className="text-base font-semibold text-foreground">Queue Confirmed</h2>
          <div className="mt-5">
            <Stepper steps={steps} current={3} />
          </div>
          <div className="text-center">
            <CheckCircle2 className="mx-auto size-14 text-success" />
            <p className="mt-3 font-semibold text-foreground">
              Your queue ticket has been issued successfully!
            </p>
          </div>
          <div className="mt-6 flex justify-center">
            <QueueTokenBadge
              token={created.queueNumber}
              station={created.departmentName}
              urgency={isEmergency ? "critical" : "general"}
            />
          </div>
          <div className="mt-6 rounded-xl border border-border p-5">
            <div className="grid gap-4 text-center sm:grid-cols-3">
              <Summary label="Department" value={created.departmentName} />
              <Summary label="Doctor" value={created.doctorName} />
              <Summary label="Your Position" value={`${created.position}th`} highlight />
            </div>
            {isEmergency ? (
              <div className="mt-4 rounded-lg bg-danger/10 p-2 text-center text-xs font-semibold text-danger">
                ⚡ EMERGENCY PRIORITY QUEUE ASSIGNED
              </div>
            ) : null}
            <div className="mt-5 border-t border-border pt-4 text-center">
              <p className="text-xs text-muted-foreground">Estimated Waiting Time</p>
              <p className="text-2xl font-bold text-primary">
                {created.estimatedWaitingMinutes} min
              </p>
            </div>
          </div>
          <p className="mt-5 text-center text-sm text-muted-foreground">
            Please be in the hospital waiting area. You will receive an alert when your turn is
            called.
          </p>
          <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
            <button
              onClick={() => navigate({ to: "/dashboard" })}
              className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
              Go to Dashboard
            </button>
            <Link
              to="/queue"
              className="rounded-lg border border-border bg-card px-6 py-2.5 text-center text-sm font-medium hover:bg-muted"
            >
              View My Queue Status
            </Link>
          </div>
        </Panel>
      </PatientLayout>
    );
  }

  const canContinue =
    (step === 0 && (departmentId != null || symptomText.trim().length > 0)) ||
    (step === 1 && departmentId != null) ||
    (step === 2 && doctorId != null) ||
    step === 3;

  return (
    <PatientLayout title="AI Symptom Checker & Queue Registration">
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h2 className="text-xl font-bold text-foreground">AI Smart Queue Assistant</h2>
          <p className="text-sm text-muted-foreground">
            Describe your condition or pick symptoms to receive instant AI department triage and
            live waiting estimates.
          </p>
        </div>

        <Stepper steps={steps} current={step} />

        <Panel>
          {errorMessage && (
            <div className="mb-5 flex items-center gap-3 rounded-lg border border-danger/40 bg-danger/10 p-3.5 text-sm text-danger">
              <AlertTriangle className="size-5 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* STEP 0: AI SYMPTOM CHECKER */}
          {step === 0 ? (
            <div className="space-y-6">
              <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-sm">
                      <Sparkles className="size-5" />
                    </div>
                    <div>
                      <h3 className="text-base font-semibold text-foreground flex items-center gap-2">
                        AI Clinical Department Triage Nurse
                        <span className="rounded bg-primary/20 px-2 py-0.5 text-[10px] font-bold text-primary">
                          FREE AI
                        </span>
                      </h3>
                      <p className="text-xs text-muted-foreground sm:text-sm">
                        Select multiple symptoms or describe your condition for clinical triage and
                        live queue estimates.
                      </p>
                    </div>
                  </div>

                  {/* Mode switcher */}
                  <div className="flex rounded-lg border border-border bg-card p-1 text-xs">
                    <button
                      type="button"
                      onClick={() => setTriageMode("interactive")}
                      className={cn(
                        "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium transition",
                        triageMode === "interactive"
                          ? "bg-primary text-primary-foreground shadow-sm"
                          : "text-muted-foreground hover:text-foreground",
                      )}
                    >
                      <MessageSquare className="size-3" /> Multi-Turn AI
                    </button>
                    <button
                      type="button"
                      onClick={() => setTriageMode("quick")}
                      className={cn(
                        "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium transition",
                        triageMode === "quick"
                          ? "bg-primary text-primary-foreground shadow-sm"
                          : "text-muted-foreground hover:text-foreground",
                      )}
                    >
                      ⚡ Quick 1-Click
                    </button>
                  </div>
                </div>

                {/* Initial Input */}
                {interactiveStep === "input" && (
                  <div className="mt-4 space-y-3">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Describe your symptoms in your own words:
                    </label>
                    <textarea
                      rows={3}
                      className="w-full rounded-lg border border-border bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                      placeholder="e.g. I have severe chest pressure radiating to my left shoulder, sweating, and difficulty breathing..."
                      value={symptomText}
                      onChange={(e) => setSymptomText(e.target.value)}
                    />

                    {/* Common Multi-select Symptoms */}
                    {symptomsList.length > 0 && (
                      <div>
                        <span className="mb-2 block text-xs font-semibold text-foreground">
                          Common Symptoms (Select multiple):
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {symptomsList.map((s) => {
                            const isSelected = selectedSymptomChips.includes(s.name);
                            return (
                              <button
                                key={s.id}
                                type="button"
                                onClick={() => toggleChip(s.name)}
                                className={cn(
                                  "rounded-full border px-3 py-1 text-xs font-medium transition",
                                  isSelected
                                    ? "border-primary bg-primary text-primary-foreground shadow-sm"
                                    : "border-border bg-card text-muted-foreground hover:border-primary hover:text-foreground",
                                )}
                              >
                                {isSelected ? "✓ " : "+ "}
                                {s.name}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Quick suggestion chips */}
                    <div>
                      <span className="mb-2 block text-xs text-muted-foreground">
                        Or choose typical scenario combinations:
                      </span>
                      <div className="flex flex-wrap gap-2">
                        {QUICK_SYMPTOMS.map((q) => (
                          <button
                            key={q}
                            type="button"
                            onClick={() => {
                              setSymptomText(q);
                              if (triageMode === "interactive") {
                                handleStartInteractiveTriage(q);
                              } else {
                                handleAnalyzeAI(q);
                              }
                            }}
                            className="rounded-full border border-border bg-card px-3 py-1 text-xs font-medium text-muted-foreground transition hover:border-primary hover:bg-primary/10 hover:text-primary"
                          >
                            + {q}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-3 pt-2">
                      {triageMode === "interactive" ? (
                        <button
                          type="button"
                          disabled={interactiveLoading || !symptomText.trim()}
                          onClick={() => handleStartInteractiveTriage()}
                          className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90 disabled:opacity-50"
                        >
                          {interactiveLoading ? (
                            <>
                              <Bot className="size-4 animate-spin" /> Preparing Triage Questions...
                            </>
                          ) : (
                            <>
                              <MessageSquare className="size-4" /> Start Interactive AI Triage →
                            </>
                          )}
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled={aiLoading || !symptomText.trim()}
                          onClick={() => handleAnalyzeAI()}
                          className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground shadow transition hover:opacity-90 disabled:opacity-50"
                        >
                          {aiLoading ? (
                            <>
                              <Bot className="size-4 animate-spin" /> Analyzing with Medical AI...
                            </>
                          ) : (
                            <>
                              <Sparkles className="size-4" /> Get Quick AI Recommendation
                            </>
                          )}
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={() => setStep(1)}
                        className="text-xs font-medium text-muted-foreground hover:text-foreground hover:underline sm:text-sm"
                      >
                        Or select department manually →
                      </button>
                    </div>
                  </div>
                )}

                {/* Step: Interactive Questions */}
                {interactiveStep === "questions" && (
                  <div className="mt-4 space-y-4 rounded-xl border border-primary/20 bg-card p-4 sm:p-5">
                    <div className="flex items-center justify-between border-b border-border pb-3">
                      <div className="flex items-center gap-2">
                        <MessageSquare className="size-4 text-primary" />
                        <h4 className="text-sm font-bold text-foreground">
                          Clinical Clarifying Questions
                        </h4>
                      </div>
                      <button
                        type="button"
                        onClick={() => setInteractiveStep("input")}
                        className="text-xs text-muted-foreground hover:text-foreground hover:underline"
                      >
                        ← Edit Symptoms
                      </button>
                    </div>

                    <p className="text-xs text-muted-foreground">
                      Please answer these brief follow-up questions to help our AI assign the exact
                      clinical acuity level:
                    </p>

                    <div className="space-y-4">
                      {interactiveQuestions.map((q, idx) => (
                        <div
                          key={q.id || idx}
                          className="rounded-lg bg-accent/30 p-3.5 border border-border"
                        >
                          <label className="text-xs font-bold text-foreground flex items-center gap-2">
                            <span className="flex size-5 items-center justify-center rounded-full bg-primary text-[11px] font-bold text-primary-foreground">
                              {idx + 1}
                            </span>
                            {q.question}
                          </label>
                          <div className="mt-2.5 flex flex-wrap gap-2">
                            {q.options?.map((opt) => {
                              const isSelected = interactiveAnswers[q.id] === opt;
                              return (
                                <button
                                  key={opt}
                                  type="button"
                                  onClick={() => handleSelectAnswer(q.id, opt)}
                                  className={cn(
                                    "rounded-lg border px-3 py-1.5 text-xs font-medium transition",
                                    isSelected
                                      ? "border-primary bg-primary text-primary-foreground shadow-sm"
                                      : "border-border bg-card text-foreground hover:border-primary/50 hover:bg-accent",
                                  )}
                                >
                                  {opt}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      ))}
                    </div>

                    <div className="flex items-center justify-between border-t border-border pt-3">
                      <span className="text-xs text-muted-foreground">
                        {Object.keys(interactiveAnswers).length} of {interactiveQuestions.length}{" "}
                        answered
                      </span>
                      <button
                        type="button"
                        disabled={interactiveLoading}
                        onClick={handleFinalizeInteractive}
                        className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2 text-xs font-bold text-primary-foreground shadow hover:opacity-90 disabled:opacity-50"
                      >
                        {interactiveLoading ? (
                          <>
                            <Bot className="size-3.5 animate-spin" /> Finalizing Triage...
                          </>
                        ) : (
                          <>
                            <Sparkles className="size-3.5" /> Complete Clinical Triage →
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Interactive Result Card */}
              {interactiveResult && interactiveStep === "result" && (
                <div
                  className={cn(
                    "rounded-xl border p-5 transition-all",
                    interactiveResult.emergency
                      ? "border-danger/50 bg-danger/5 shadow-sm"
                      : "border-primary/40 bg-card shadow-sm",
                  )}
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2.5 py-0.5 text-xs font-semibold text-primary">
                        <Sparkles className="size-3" /> Comprehensive Triage Report
                      </span>
                      <span
                        className={cn(
                          "rounded-full px-2.5 py-0.5 text-xs font-bold",
                          interactiveResult.emergency
                            ? "bg-danger text-white animate-pulse"
                            : "bg-primary/20 text-primary",
                        )}
                      >
                        Acuity Score: Level {interactiveResult.acuityScore} / 5
                      </span>
                      {interactiveResult.emergency && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-danger px-2.5 py-0.5 text-xs font-bold text-white">
                          <AlertTriangle className="size-3" /> EMERGENCY
                        </span>
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => setInteractiveStep("input")}
                      className="text-xs text-muted-foreground hover:text-foreground hover:underline"
                    >
                      Re-run Triage ↺
                    </button>
                  </div>

                  <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <div>
                      <p className="text-xs text-muted-foreground">Recommended Department</p>
                      <h4 className="text-lg font-bold text-primary">
                        {interactiveResult.department?.name ||
                          interactiveResult.department?.departmentName ||
                          "General Medicine"}
                      </h4>
                    </div>

                    <div>
                      <p className="text-xs text-muted-foreground">Clinical Rationale</p>
                      <p className="mt-0.5 text-xs text-foreground leading-relaxed">
                        {interactiveResult.clinicalReason}
                      </p>
                    </div>
                  </div>

                  <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
                    <span className="text-xs text-muted-foreground">
                      Disposition:{" "}
                      <strong>{interactiveResult.disposition?.toUpperCase() || "ROUTINE"}</strong>
                    </span>
                    <button
                      type="button"
                      onClick={() => setStep(2)}
                      className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
                    >
                      Proceed to Select Doctor →
                    </button>
                  </div>
                </div>
              )}

              {/* Quick AI Result Card (if in quick mode) */}
              {aiResult && triageMode === "quick" ? (
                <div
                  className={cn(
                    "rounded-xl border p-5 transition-all",
                    aiResult.emergency
                      ? "border-danger/50 bg-danger/5 shadow-sm"
                      : "border-primary/40 bg-card shadow-sm",
                  )}
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border pb-3">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2.5 py-0.5 text-xs font-semibold text-primary">
                        <Sparkles className="size-3" /> AI Triage Result
                      </span>
                      {aiResult.emergency ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-danger px-2.5 py-0.5 text-xs font-bold text-white animate-pulse">
                          <AlertTriangle className="size-3" /> EMERGENCY CASE
                        </span>
                      ) : null}
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {aiResult.aiUsed ? "OpenRouter AI Model" : "Rule Engine Fallback"}
                    </span>
                  </div>

                  <div className="mt-4 grid gap-4 sm:grid-cols-2">
                    <div>
                      <p className="text-xs text-muted-foreground">Recommended Department</p>
                      <h4 className="text-lg font-bold text-primary">
                        {aiResult.department?.name}
                      </h4>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {departments.find((d) => d.id === aiResult.department?.id)?.location ??
                          "Specialist Clinic"}
                      </p>
                    </div>

                    <div>
                      <p className="text-xs text-muted-foreground">AI Clinical Rationale</p>
                      <p className="mt-0.5 text-sm font-medium text-foreground">
                        "{aiResult.reason}"
                      </p>
                    </div>
                  </div>

                  {aiResult.emergency ? (
                    <div className="mt-4 flex items-center gap-2 rounded-lg bg-danger/10 p-3 text-xs text-danger">
                      <AlertTriangle className="size-4 shrink-0" />
                      <span>
                        Your symptoms match emergency criteria. You will be assigned top queue
                        priority upon confirmation.
                      </span>
                    </div>
                  ) : null}

                  <div className="mt-5 flex items-center justify-between border-t border-border pt-4">
                    <span className="text-xs text-muted-foreground">
                      {aiResult.doctors?.length ?? 0} active doctors in {aiResult.department?.name}
                    </span>
                    <button
                      type="button"
                      onClick={() => setStep(2)}
                      className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
                    >
                      Choose Doctor in {aiResult.department?.name} →
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}

          {/* STEP 1: CHOOSE DEPARTMENT (MANUAL OR OVERRIDE) */}
          {step === 1 ? (
            <div>
              <div className="mb-4 flex items-center justify-between">
                <h3 className="text-base font-semibold text-foreground">Select Department</h3>
                <button
                  onClick={() => setStep(0)}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                >
                  <Sparkles className="size-3.5" /> Use AI Recommendation instead
                </button>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                {departments
                  .filter((d) => d.status === "ACTIVE")
                  .map((d) => {
                    const isSelected = departmentId === d.id;
                    const isAiDept = aiResult?.department?.id === d.id;
                    return (
                      <button
                        key={d.id}
                        onClick={() => {
                          setDepartmentId(d.id);
                          setAiResult((prev) =>
                            prev
                              ? {
                                  ...prev,
                                  department: {
                                    id: d.id,
                                    departmentCode: d.departmentCode,
                                    name: d.name,
                                  },
                                }
                              : null,
                          );
                        }}
                        className={cn(
                          "relative rounded-lg border p-4 text-left transition-colors",
                          isSelected
                            ? "border-primary bg-primary/10 shadow-sm"
                            : "border-border bg-card hover:bg-muted",
                        )}
                      >
                        {isAiDept ? (
                          <span className="absolute right-3 top-3 inline-flex items-center gap-1 rounded-full bg-primary px-2 py-0.5 text-[10px] font-bold text-primary-foreground">
                            <Sparkles className="size-2.5" /> AI Pick
                          </span>
                        ) : null}
                        <p className="text-sm font-semibold text-foreground">{d.name}</p>
                        <p className="text-xs text-muted-foreground">{d.location}</p>
                      </button>
                    );
                  })}
              </div>
            </div>
          ) : null}

          {/* STEP 2: CHOOSE DOCTOR */}
          {step === 2 ? (
            <div>
              <div className="mb-4 flex items-center justify-between">
                <div>
                  <h3 className="text-base font-semibold text-foreground">
                    Choose Doctor in{" "}
                    {departments.find((d) => d.id === departmentId)?.name ?? "Selected Department"}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    Sorted by shortest current waiting time.
                  </p>
                </div>
              </div>

              <div className="mb-2 hidden grid-cols-[1fr_auto_auto] gap-6 px-4 text-xs text-muted-foreground sm:grid">
                <span />
                <span className="w-28 text-center">Status</span>
                <span className="w-28 text-center">Waiting Time</span>
              </div>

              <div className="space-y-3">
                {doctors.length === 0 ? (
                  <p className="py-6 text-center text-sm text-muted-foreground">
                    Loading doctors for this department...
                  </p>
                ) : (
                  doctors.map((d) => {
                    const isSelected = doctorId === d.id;
                    return (
                      <button
                        key={d.id}
                        onClick={() => setDoctorId(d.id)}
                        className={cn(
                          "flex w-full items-center gap-4 rounded-lg border p-4 text-left transition-colors sm:gap-6",
                          isSelected
                            ? "border-primary bg-primary/10 shadow-sm"
                            : "border-border bg-card hover:bg-muted",
                        )}
                      >
                        <span
                          className={cn(
                            "flex size-5 shrink-0 items-center justify-center rounded-full border",
                            isSelected
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-muted-foreground",
                          )}
                        >
                          {isSelected ? <CheckCircle2 className="size-3.5" /> : null}
                        </span>

                        <span className="flex-1">
                          <span className="block text-sm font-semibold text-foreground">
                            {d.name}{" "}
                            <span className="text-xs font-normal text-primary">
                              ({d.qualification || "MBBS, M.Med.Sc"})
                            </span>
                          </span>
                          <span className="block text-xs text-muted-foreground">
                            {d.department} •{" "}
                            <span className="font-medium text-foreground/80">
                              {d.specialization || "General Medicine"}
                            </span>{" "}
                            • {d.experienceYears || 5} yrs exp
                          </span>
                        </span>

                        <span className="w-28 text-center">
                          <span
                            className={cn(
                              "inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold",
                              d.availabilityStatus === "CONSULTING"
                                ? "bg-success/15 text-success"
                                : "bg-amber-500/15 text-amber-600 dark:text-amber-400",
                            )}
                          >
                            {d.availabilityStatus === "CONSULTING" ? "Available" : "On Break"}
                          </span>
                        </span>

                        <span className="w-32 text-center text-sm font-bold text-foreground">
                          {waitTimePredictions[d.id]
                            ? `${waitTimePredictions[d.id]} min`
                            : `${d.estimatedWaitingMinutes} min`}
                          {d.availabilityStatus !== "CONSULTING" && (
                            <span className="block text-[10px] font-normal text-amber-600 dark:text-amber-400">
                              (incl. 30m break)
                            </span>
                          )}
                        </span>
                      </button>
                    );
                  })
                )}
              </div>
            </div>
          ) : null}

          {/* STEP 3: CONFIRMATION */}
          {step === 3 ? (
            <div className="space-y-5">
              <h3 className="text-base font-semibold text-foreground">
                Review & Confirm Your Queue
              </h3>

              <div className="rounded-xl border border-border bg-card p-4 sm:p-5">
                <dl className="grid gap-4 sm:grid-cols-3">
                  <Summary
                    label="Department"
                    value={departments.find((d) => d.id === departmentId)?.name ?? "—"}
                  />
                  <Summary
                    label="Assigned Doctor"
                    value={doctors.find((d) => d.id === doctorId)?.name ?? "—"}
                  />
                  <Summary
                    label="Queue Priority"
                    value={isEmergency ? "EMERGENCY (Top Priority)" : "Normal"}
                    highlight={isEmergency}
                  />
                </dl>
              </div>
            </div>
          ) : null}

          {/* Navigation Buttons */}
          <div className="flex items-center justify-between border-t border-border pt-5">
            <button
              onClick={() => (step === 0 ? navigate({ to: "/dashboard" }) : setStep(step - 1))}
              className="rounded-lg border border-border bg-card px-4 py-2.5 text-sm font-medium hover:bg-muted sm:px-6"
            >
              {step === 0 ? "Cancel" : "Back"}
            </button>

            <button
              disabled={!canContinue || submitting}
              onClick={() => {
                if (step === 0 && !aiResult && symptomText.trim()) {
                  handleAnalyzeAI();
                } else if (step === 3) {
                  confirm();
                } else {
                  setStep(step + 1);
                }
              }}
              className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50 sm:px-8"
            >
              {step === 3 ? (submitting ? "Confirming..." : "Confirm & Join Queue") : "Next"}
            </button>
          </div>
        </Panel>
      </div>
    </PatientLayout>
  );
}

function Summary({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={
          highlight ? "text-lg font-bold text-danger" : "text-sm font-semibold text-foreground"
        }
      >
        {value}
      </p>
    </div>
  );
}
