import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { AdminLayout } from "@/components/portal/shells";
import { Panel, Field, inputClass } from "@/components/portal/ui-kit";
import {
  generateFollowUpInstructions,
  type FollowUpResult,
} from "@/services/api";

export const Route = createFileRoute("/admin/ai-followup")({
  head: () => ({ meta: [{ title: "AI Follow-up Instructions" }] }),
  component: FollowUpInstructionsPage,
});

function FollowUpInstructionsPage() {
  const [diagnosis, setDiagnosis] = useState("");
  const [medications, setMedications] = useState("");
  const [patientAge, setPatientAge] = useState("");
  const [patientGender, setPatientGender] = useState("");
  const [department, setDepartment] = useState("");
  const [additionalNotes, setAdditionalNotes] = useState("");
  const [result, setResult] = useState<FollowUpResult | null>(null);
  const [generating, setGenerating] = useState(false);

  async function generate() {
    setGenerating(true);
    const payload: Record<string, string> = {};
    if (diagnosis) payload["diagnosis"] = diagnosis;
    if (medications) payload["medications"] = medications;
    if (patientAge) payload["patientAge"] = patientAge;
    if (patientGender) payload["patientGender"] = patientGender;
    if (department) payload["department"] = department;
    if (additionalNotes) payload["additionalNotes"] = additionalNotes;
    const r = await generateFollowUpInstructions(payload as any);
    setResult(r);
    setGenerating(false);
  }

  return (
    <AdminLayout title="AI Follow-up Instructions">
      <div className="space-y-6">
        <Panel title="Generate Patient Follow-up Instructions">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Diagnosis">
              <input
                className={inputClass}
                placeholder="e.g. Upper respiratory infection"
                value={diagnosis}
                onChange={(e) => setDiagnosis(e.target.value)}
              />
            </Field>
            <Field label="Medications Prescribed">
              <input
                className={inputClass}
                placeholder="e.g. Paracetamol 500mg TDS"
                value={medications}
                onChange={(e) => setMedications(e.target.value)}
              />
            </Field>
            <Field label="Patient Age">
              <input
                className={inputClass}
                placeholder="e.g. 35"
                value={patientAge}
                onChange={(e) => setPatientAge(e.target.value)}
              />
            </Field>
            <Field label="Patient Gender">
              <select className={inputClass} value={patientGender} onChange={(e) => setPatientGender(e.target.value)}>
                <option value="">Select</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </Field>
            <Field label="Department">
              <input
                className={inputClass}
                placeholder="e.g. General Medicine"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              />
            </Field>
            <Field label="Additional Notes">
              <input
                className={inputClass}
                placeholder="e.g. Patient has diabetes"
                value={additionalNotes}
                onChange={(e) => setAdditionalNotes(e.target.value)}
              />
            </Field>
          </div>
          <div className="mt-4">
            <button
              onClick={generate}
              disabled={generating}
              className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {generating ? "Generating..." : "Generate Instructions"}
            </button>
          </div>
        </Panel>

        {result && (
          <>
            <Panel title="Follow-up Instructions">
              <div className="space-y-4">
                {result.aiUsed && (
                  <span className="inline-block rounded-full bg-success-soft px-3 py-1 text-xs font-medium text-success">
                    AI Generated
                  </span>
                )}

                <div className="rounded-xl border border-border p-4">
                  <h3 className="text-sm font-semibold text-foreground">Summary</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{result.summary}</p>
                </div>

                <div className="rounded-xl border border-border p-4">
                  <h3 className="text-sm font-semibold text-foreground">Diet Instructions</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{result.dietInstructions}</p>
                </div>

                <div className="rounded-xl border border-border p-4">
                  <h3 className="text-sm font-semibold text-foreground">Activity Restrictions</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{result.activityRestrictions}</p>
                </div>

                <div className="rounded-xl border border-border p-4">
                  <h3 className="text-sm font-semibold text-foreground">Follow-up Date</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{result.followUpDate}</p>
                </div>
              </div>
            </Panel>

            <div className="grid gap-6 lg:grid-cols-2">
              <Panel title="Medication Reminders">
                <ul className="space-y-2">
                  {result.medicationReminders.map((item, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                      <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                      {item}
                    </li>
                  ))}
                </ul>
              </Panel>

              <Panel title="Warning Signs (Seek Immediate Help)">
                <ul className="space-y-2">
                  {result.warningSigns.map((item, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-danger">
                      <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-danger" />
                      {item}
                    </li>
                  ))}
                </ul>
              </Panel>

              <Panel title="Self-Care Tips">
                <ul className="space-y-2">
                  {result.selfCareTips.map((item, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                      <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-success" />
                      {item}
                    </li>
                  ))}
                </ul>
              </Panel>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  );
}
