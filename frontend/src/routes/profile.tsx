import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getPatientProfile } from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import { Panel } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonPanel } from "@/components/ui/loading";
import type { PatientProfile } from "@/types";

export const Route = createFileRoute("/profile")({
  head: () => ({
    meta: [
      { title: "My Profile — Hospital Smart Queue" },
      { name: "description", content: "Review and update your patient profile details." },
      { property: "og:title", content: "My Profile — Hospital Smart Queue" },
      { property: "og:description", content: "Review and update your patient details." },
    ],
  }),
  component: ProfilePage,
});

function ProfilePage() {
  const { session } = useAuth();
  const [profile, setProfile] = useState<PatientProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [showPhotoModal, setShowPhotoModal] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string>("");
  const [customPhotoInput, setCustomPhotoInput] = useState("");
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const AVATAR_PRESETS = [
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
  ];

  useEffect(() => {
    getPatientProfile(session?.userId)
      .then((p) => {
        setProfile(p);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
    if (session?.userId) {
      const savedPhoto = localStorage.getItem(`patient_photo_${session.userId}`);
      if (savedPhoto) setAvatarUrl(savedPhoto);
    }
  }, [session?.userId]);

  function handleSavePhoto(url: string) {
    if (!url) return;
    setAvatarUrl(url);
    if (session?.userId) {
      localStorage.setItem(`patient_photo_${session.userId}`, url);
    }
    setShowPhotoModal(false);
    setSuccessMsg("Profile photo updated successfully!");
    setTimeout(() => setSuccessMsg(null), 3000);
  }

  const patientCode = profile
    ? String(profile.id).startsWith("P")
      ? profile.id
      : `P0${profile.id}`
    : "—";

  if (loading && !profile) {
    return (
      <PatientLayout title="My Profile">
        <div className="grid gap-6 lg:grid-cols-3">
          <SkeletonCard className="text-center" />
          <SkeletonPanel title className="lg:col-span-2" />
        </div>
      </PatientLayout>
    );
  }

  return (
    <PatientLayout title="My Profile">
      {successMsg && (
        <div className="mb-4 rounded-lg bg-success/15 p-3 text-center text-sm font-semibold text-success">
          ✓ {successMsg}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel className="text-center">
          <div className="relative mx-auto size-24 overflow-hidden rounded-full border-2 border-primary/20 bg-accent text-2xl font-bold text-accent-foreground shadow">
            {avatarUrl ? (
              <img src={avatarUrl} alt="Avatar" className="size-full object-cover" />
            ) : (
              <div className="flex size-full items-center justify-center">
                {profile?.name ? profile.name.slice(0, 2).toUpperCase() : "--"}
              </div>
            )}
          </div>
          <p className="mt-4 text-lg font-semibold text-foreground">
            {profile?.name ?? session?.name ?? "Loading..."}
          </p>
          <p className="text-xs text-muted-foreground">Patient Account</p>
          <button
            onClick={() => setShowPhotoModal(true)}
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm"
          >
            Change Photo
          </button>
        </Panel>

        <Panel title="Profile Information" className="lg:col-span-2">
          {profile ? (
            <dl className="grid gap-5 sm:grid-cols-2">
              <Row label="Patient ID" value={String(patientCode)} />
              <Row label="Full Name" value={profile.name} />
              <Row label="Registered Phone Number" value={profile.phone} />
              <Row label="Date of Birth" value={profile.dateOfBirth || "—"} />
              <Row label="Gender" value={profile.gender || "—"} />
              <Row label="Residential Address" value={profile.address || "Yangon, Myanmar"} />
            </dl>
          ) : (
            <p className="text-sm text-muted-foreground">Loading profile...</p>
          )}
        </Panel>
      </div>

      {/* Change Photo Modal */}
      {showPhotoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95">
            <h3 className="text-base font-bold text-foreground">Select Profile Avatar</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Choose from sample avatars or paste a custom image URL:
            </p>

            <div className="mt-4 flex justify-center gap-3">
              {AVATAR_PRESETS.map((p, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSavePhoto(p)}
                  className="size-14 overflow-hidden rounded-full border-2 border-border transition hover:scale-105 hover:border-primary"
                >
                  <img src={p} alt={`preset ${idx}`} className="size-full object-cover" />
                </button>
              ))}
            </div>

            <div className="mt-5 space-y-2 border-t border-border pt-4">
              <label className="block text-xs font-medium text-foreground">Custom Photo URL:</label>
              <input
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="https://example.com/photo.jpg"
                value={customPhotoInput}
                onChange={(e) => setCustomPhotoInput(e.target.value)}
              />
              <button
                type="button"
                onClick={() => handleSavePhoto(customPhotoInput.trim())}
                disabled={!customPhotoInput.trim()}
                className="w-full rounded-lg bg-primary py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                Apply Custom Photo
              </button>
            </div>

            <div className="mt-4 flex justify-end">
              <button
                type="button"
                onClick={() => setShowPhotoModal(false)}
                className="rounded-lg border border-border px-4 py-1.5 text-xs font-medium hover:bg-muted"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </PatientLayout>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium text-foreground">{value}</dd>
    </div>
  );
}
