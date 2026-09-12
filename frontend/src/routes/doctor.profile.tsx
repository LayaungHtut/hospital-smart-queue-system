import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Clock, Mail, Phone, Stethoscope, User } from "lucide-react";
import { getDoctorProfile } from "@/services/api";
import { DoctorLayout } from "@/components/portal/shells";
import { Panel } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonPanel } from "@/components/ui/loading";
import type { DoctorProfile } from "@/types";

export const Route = createFileRoute("/doctor/profile")({
  head: () => ({
    meta: [
      { title: "Doctor Profile — Doctor Portal" },
      { name: "description", content: "Doctor details, specialization, and consultation hours." },
      { property: "og:title", content: "Doctor Profile — Doctor Portal" },
      { property: "og:description", content: "Doctor details and profile." },
    ],
  }),
  component: DoctorProfilePage,
});

function DoctorProfilePage() {
  const { session } = useAuth();
  const [profile, setProfile] = useState<DoctorProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [doctorId, setDoctorId] = useState<string | number>(
    session?.doctorId ?? session?.userId ?? "D001",
  );

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

  useEffect(() => {
    getDoctorProfile(doctorId)
      .then((res) => {
        setProfile(res);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [doctorId]);

  return (
    <DoctorLayout title="Doctor Profile">
      <div className="mx-auto max-w-3xl space-y-6">
        {loading ? (
          <>
            <SkeletonCard />
            <SkeletonPanel title />
          </>
        ) : (
          <Panel title="Doctor Information">
            <div className="space-y-6">
              <div className="flex items-center gap-4 border-b border-border pb-6">
                <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  <User className="size-8" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-foreground">{profile?.name}</h2>
                  <p className="text-sm text-muted-foreground">
                    Code: <strong className="text-primary">{profile?.doctorCode}</strong> •{" "}
                    {profile?.specialization}
                  </p>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-4">
                  <Stethoscope className="size-5 text-primary" />
                  <div>
                    <p className="text-xs text-muted-foreground">Department</p>
                    <p className="text-sm font-semibold text-foreground">{profile?.department}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-4">
                  <Phone className="size-5 text-primary" />
                  <div>
                    <p className="text-xs text-muted-foreground">Phone Number</p>
                    <p className="text-sm font-semibold text-foreground">{profile?.phone}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-4">
                  <Mail className="size-5 text-primary" />
                  <div>
                    <p className="text-xs text-muted-foreground">Email</p>
                    <p className="text-sm font-semibold text-foreground">{profile?.email}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3 rounded-xl border border-border bg-card p-4">
                  <Clock className="size-5 text-primary" />
                  <div>
                    <p className="text-xs text-muted-foreground">Working Hours</p>
                    <p className="text-sm font-semibold text-foreground">
                      {profile?.queueOpenTime ?? "09:00"} — {profile?.queueCloseTime ?? "17:00"}
                    </p>
                  </div>
                </div>
              </div>

              <div className="rounded-xl bg-accent/40 p-4 border border-border">
                <h3 className="text-sm font-bold text-foreground">Consultation Parameters</h3>
                <div className="mt-2 grid grid-cols-2 gap-4 text-xs text-muted-foreground">
                  <div>
                    <span>Average Consultation Time:</span>
                    <p className="text-sm font-bold text-foreground mt-0.5">
                      {profile?.averageConsultationMinutes ?? 15} minutes
                    </p>
                  </div>
                  <div>
                    <span>Maximum Queue Capacity:</span>
                    <p className="text-sm font-bold text-foreground mt-0.5">
                      {profile?.maxQueueSize ?? 20} patients
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </Panel>
        )}
      </div>
    </DoctorLayout>
  );
}
