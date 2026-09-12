import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Lock, Eye, EyeOff } from "lucide-react";
import { getStaffProfile } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { Panel } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonCard, SkeletonPanel } from "@/components/ui/loading";
import type { StaffProfile } from "@/types";

export const Route = createFileRoute("/staff/profile")({
  head: () => ({
    meta: [
      { title: "My Profile — Staff Portal" },
      { name: "description", content: "Staff account details, shift information and password." },
      { property: "og:title", content: "My Profile — Staff Portal" },
      { property: "og:description", content: "Staff account details and shift information." },
    ],
  }),
  component: StaffProfilePage,
});

function StaffProfilePage() {
  const { session } = useAuth();
  const [profile, setProfile] = useState<StaffProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const [showPhotoModal, setShowPhotoModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string>("");
  const [customPhotoInput, setCustomPhotoInput] = useState("");
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passLoading, setPassLoading] = useState(false);

  const AVATAR_PRESETS = [
    "https://images.unsplash.com/photo-1594824813591-9e798031d279?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150&auto=format&fit=crop&q=80",
  ];

  useEffect(() => {
    getStaffProfile(session?.userId)
      .then((p) => {
        setProfile(p);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
    if (session?.userId) {
      const savedPhoto = localStorage.getItem(`staff_photo_${session.userId}`);
      if (savedPhoto) setAvatarUrl(savedPhoto);
    }
  }, [session?.userId]);

  function handleSavePhoto(url: string) {
    if (!url) return;
    setAvatarUrl(url);
    if (session?.userId) {
      localStorage.setItem(`staff_photo_${session.userId}`, url);
    }
    setShowPhotoModal(false);
    setFeedbackMsg({ type: "success", text: "Staff photo updated successfully!" });
    setTimeout(() => setFeedbackMsg(null), 3500);
  }

  async function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!newPassword || newPassword.length < 4) {
      setFeedbackMsg({ type: "error", text: "New password must be at least 4 characters." });
      return;
    }
    if (newPassword !== confirmPassword) {
      setFeedbackMsg({ type: "error", text: "New passwords do not match." });
      return;
    }

    setPassLoading(true);
    // Simulate updating password on backend
    setTimeout(() => {
      setPassLoading(false);
      setShowPasswordModal(false);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setFeedbackMsg({ type: "success", text: "Password changed successfully! Please use your new password next time you sign in." });
      setTimeout(() => setFeedbackMsg(null), 4000);
    }, 600);
  }

  if (loading && !profile) {
    return (
      <StaffLayout title="My Profile">
        <div className="grid gap-6 lg:grid-cols-3">
          <SkeletonCard className="text-center" />
          <SkeletonPanel title className="lg:col-span-2" />
        </div>
      </StaffLayout>
    );
  }

  return (
    <StaffLayout title="My Profile">
      {feedbackMsg && (
        <div
          className={`mb-4 rounded-lg p-3 text-center text-sm font-semibold ${
            feedbackMsg.type === "success" ? "bg-success/15 text-success" : "bg-danger/15 text-danger"
          }`}
        >
          {feedbackMsg.type === "success" ? "✓ " : "⚠ "}
          {feedbackMsg.text}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel className="text-center">
          <div className="relative mx-auto size-28 overflow-hidden rounded-full border-2 border-primary/20 bg-accent text-2xl font-bold text-accent-foreground shadow">
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
          <p className="text-sm text-muted-foreground">{profile?.role ?? "Front Desk Staff"}</p>
          <p className="mt-1 text-xs font-semibold text-primary">{profile?.department ?? "Outpatient Department"}</p>
          <button
            onClick={() => setShowPhotoModal(true)}
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm"
          >
            Change Photo
          </button>
        </Panel>

        <div className="space-y-6 lg:col-span-2">
          <Panel title="Staff Credentials & Shift">
            {profile ? (
              <dl className="grid gap-5 sm:grid-cols-2">
                <Row label="Staff ID" value={profile.staffCode} />
                <Row label="Department" value={profile.department} />
                <Row label="Full Name" value={profile.name} />
                <Row label="Role / Designation" value={profile.role} />
                <Row label="Contact Phone" value={profile.phone} />
                <Row label="Assigned Shift" value={profile.workingShift || "08:30 AM - 05:00 PM"} />
                <Row label="Official Email" value={profile.email} />
                <Row label="Joining Date" value={profile.joiningDate || "01 Jan 2024"} />
              </dl>
            ) : (
              <p className="text-sm text-muted-foreground">Loading profile...</p>
            )}
          </Panel>

          <Panel>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground">
                  <Lock className="size-4 text-primary" /> Security & Password
                </h3>
                <p className="text-xs text-muted-foreground">
                  Keep your hospital portal account credentials up to date.
                </p>
              </div>
              <button
                onClick={() => setShowPasswordModal(true)}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 shadow-sm"
              >
                Change Password
              </button>
            </div>
          </Panel>
        </div>
      </div>

      {/* Change Photo Modal */}
      {showPhotoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95">
            <h3 className="text-base font-bold text-foreground">Change Staff Avatar</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Select an avatar preset or enter an image URL:
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
                placeholder="https://example.com/staff_photo.jpg"
                value={customPhotoInput}
                onChange={(e) => setCustomPhotoInput(e.target.value)}
              />
              <button
                type="button"
                onClick={() => handleSavePhoto(customPhotoInput.trim())}
                disabled={!customPhotoInput.trim()}
                className="w-full rounded-lg bg-primary py-2 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                Apply Photo
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

      {/* Change Password Modal */}
      {showPasswordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-xl animate-in fade-in zoom-in-95">
            <h3 className="text-base font-bold text-foreground">Change Password</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Enter your current password and choose a secure new password.
            </p>

            <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-3">
              <div>
                <label className="block text-xs font-medium text-foreground">Current Password</label>
                <div className="relative">
                  <input
                    type={showCurrentPassword ? "text" : "password"}
                    required
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 pr-10 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="Enter current password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showCurrentPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-foreground">New Password</label>
                <div className="relative">
                  <input
                    type={showNewPassword ? "text" : "password"}
                    required
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 pr-10 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="At least 4 characters"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-foreground">Confirm New Password</label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 pr-10 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="Re-enter new password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div className="mt-5 flex justify-end gap-2 border-t border-border pt-4">
                <button
                  type="button"
                  onClick={() => setShowPasswordModal(false)}
                  className="rounded-lg border border-border px-4 py-1.5 text-xs font-medium hover:bg-muted"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={passLoading}
                  className="rounded-lg bg-primary px-4 py-1.5 text-xs font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
                >
                  {passLoading ? "Updating..." : "Update Password"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </StaffLayout>
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
