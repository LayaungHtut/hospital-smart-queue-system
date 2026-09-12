import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AdminLayout } from "@/components/portal/shells";
import { Panel, StatCard, Field, inputClass } from "@/components/portal/ui-kit";
import { SkeletonStatCard } from "@/components/ui/loading";
import {
  getDoctors,
  getDepartments,
  predictConsultationDuration,
  predictDurationsForDepartment,
  type DurationPrediction,
} from "@/services/api";
import type { Department, DoctorDetail } from "@/types";

export const Route = createFileRoute("/admin/ai-consultation-duration")({
  head: () => ({ meta: [{ title: "AI Consultation Duration" }] }),
  component: ConsultationDurationPage,
});

function ConsultationDurationPage() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<DoctorDetail[]>([]);
  const [selectedDept, setSelectedDept] = useState("");
  const [selectedDoctor, setSelectedDoctor] = useState("");
  const [symptoms, setSymptoms] = useState("");
  const [appointmentType, setAppointmentType] = useState("consultation");
  const [prediction, setPrediction] = useState<DurationPrediction | null>(null);
  const [deptPredictions, setDeptPredictions] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [predicting, setPredicting] = useState(false);

  useEffect(() => {
    Promise.all([getDepartments(), getDoctors()])
      .then(([depts, docs]) => {
        setDepartments(depts);
        setDoctors(docs);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    if (selectedDept) {
      setDoctors([]);
      getDoctors().then((docs) => {
        const filtered = docs.filter(
          (d) => !selectedDept || String(d.department) === selectedDept || d.department === selectedDept
        );
        setDoctors(filtered.length > 0 ? filtered : docs);
      });
    }
  }, [selectedDept]);

  async function predictSingle() {
    if (!selectedDoctor) return;
    setPredicting(true);
    const result = await predictConsultationDuration(selectedDoctor, undefined, symptoms, appointmentType);
    setPrediction(result);
    setPredicting(false);
  }

  async function predictAll() {
    if (!selectedDept) return;
    setPredicting(true);
    const dept = departments.find((d) => d.departmentCode === selectedDept || d.name === selectedDept);
    const code = dept?.departmentCode ?? selectedDept;
    const result = await predictDurationsForDepartment(code, undefined, symptoms);
    setDeptPredictions(result.predictions);
    setPrediction(null);
    setPredicting(false);
  }

  const deptDoctors = selectedDept
    ? doctors.filter((d) => String(d.department) === selectedDept)
    : doctors;

  return (
    <AdminLayout title="AI Consultation Duration">
      <div className="space-y-6">
        <Panel title="Predict Consultation Duration">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Field label="Department">
              <select
                className={inputClass}
                value={selectedDept}
                onChange={(e) => {
                  setSelectedDept(e.target.value);
                  setSelectedDoctor("");
                }}
              >
                <option value="">All Departments</option>
                {departments.map((d) => (
                  <option key={d.departmentCode} value={d.departmentCode}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Doctor">
              <select
                className={inputClass}
                value={selectedDoctor}
                onChange={(e) => setSelectedDoctor(e.target.value)}
              >
                <option value="">Select Doctor</option>
                {deptDoctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Appointment Type">
              <select
                className={inputClass}
                value={appointmentType}
                onChange={(e) => setAppointmentType(e.target.value)}
              >
                <option value="consultation">Consultation</option>
                <option value="checkup">Check-up</option>
                <option value="followup">Follow-up</option>
                <option value="procedure">Procedure</option>
              </select>
            </Field>
            <Field label="Symptoms (optional)">
              <input
                className={inputClass}
                placeholder="e.g. chest pain, fever"
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
              />
            </Field>
          </div>
          <div className="mt-4 flex gap-3">
            <button
              onClick={predictSingle}
              disabled={!selectedDoctor || predicting}
              className="rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {predicting ? "Predicting..." : "Predict for Doctor"}
            </button>
            <button
              onClick={predictAll}
              disabled={!selectedDept || predicting}
              className="rounded-xl border border-border px-5 py-2 text-sm font-medium hover:bg-accent disabled:opacity-50"
            >
              {predicting ? "Predicting..." : "Predict All Doctors in Dept"}
            </button>
          </div>
        </Panel>

        {prediction && (
          <Panel title="Prediction Result">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Predicted Duration" value={`${prediction.predictedMinutes} min`} tone="primary" />
              <StatCard label="Doctor Average" value={`${prediction.doctorAverageMinutes} min`} tone="success" />
              <StatCard label="Patient Age" value={prediction.patientAge} tone="warning" />
              <StatCard label="Patient Type" value={prediction.isNewPatient ? "New" : "Returning"} tone={prediction.isNewPatient ? "danger" : "success"} />
            </div>
          </Panel>
        )}

        {Object.keys(deptPredictions).length > 0 && (
          <Panel title="Department Predictions">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {Object.entries(deptPredictions).map(([docId, minutes]) => {
                const doc = doctors.find((d) => d.id === docId);
                return (
                  <StatCard
                    key={docId}
                    label={doc?.name ?? docId}
                    value={`${minutes} min`}
                    tone={minutes <= 12 ? "success" : minutes <= 18 ? "warning" : "danger"}
                  />
                );
              })}
            </div>
          </Panel>
        )}

        {loading && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
            <SkeletonStatCard />
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
