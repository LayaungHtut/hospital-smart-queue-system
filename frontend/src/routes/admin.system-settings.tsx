import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getSystemSettings, saveSystemSettings } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import { Field, Panel, inputClass } from "@/components/portal/ui-kit";
import { SkeletonCard } from "@/components/ui/loading";
import type { SystemSettings } from "@/types";

export const Route = createFileRoute("/admin/system-settings")({
  head: () => ({
    meta: [
      { title: "System Settings — Admin Portal" },
      {
        name: "description",
        content: "Hospital name, time zone and date/time display preferences.",
      },
      { property: "og:title", content: "System Settings — Admin Portal" },
      { property: "og:description", content: "Hospital name, time zone and display preferences." },
    ],
  }),
  component: SystemSettingsPage,
});

const PRESET_LOGOS = [
  { name: "Medical Plus", url: "https://images.unsplash.com/photo-1516549655169-df83a0774514?w=100&auto=format&fit=crop&q=80" },
  { name: "Stethoscope", url: "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=100&auto=format&fit=crop&q=80" },
  { name: "Healthcare", url: "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=100&auto=format&fit=crop&q=80" },
];

function SystemSettingsPage() {
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getSystemSettings()
      .then((res) => {
        const savedName = localStorage.getItem("system_hospital_name");
        const savedLogo = localStorage.getItem("system_logo_url");
        setSettings({
          ...res,
          hospitalName: savedName || res.hospitalName || "CareQueue General Hospital",
          logoUrl: savedLogo !== null ? savedLogo : res.logoUrl || "",
        });
      })
      .catch(console.error);
  }, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!settings) return;
    await saveSystemSettings(settings);
    if (settings.hospitalName) {
      localStorage.setItem("system_hospital_name", settings.hospitalName);
    }
    if (settings.logoUrl !== undefined) {
      localStorage.setItem("system_logo_url", settings.logoUrl);
    }
    window.dispatchEvent(new Event("system_settings_updated"));
    setSaved(true);
    setTimeout(() => setSaved(false), 3500);
  }

  if (!settings) {
    return (
      <AdminLayout title="System Settings">
        <SkeletonCard />
      </AdminLayout>
    );
  }

  return (
    <AdminLayout title="System Settings">
      <form onSubmit={save} className="space-y-6">
        <Panel title="Hospital Brand & Identity">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Hospital Name (Reflected across entire system)">
              <input
                required
                className={inputClass}
                placeholder="e.g. Yangon General Hospital"
                value={settings.hospitalName}
                onChange={(e) => setSettings({ ...settings, hospitalName: e.target.value })}
              />
            </Field>
            <Field label="Hospital Logo URL">
              <input
                className={inputClass}
                placeholder="https://example.com/hospital_logo.png"
                value={settings.logoUrl}
                onChange={(e) => setSettings({ ...settings, logoUrl: e.target.value })}
              />
            </Field>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-border pt-3">
            <span className="text-xs font-semibold text-muted-foreground">Sample Logos:</span>
            {PRESET_LOGOS.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => setSettings({ ...settings, logoUrl: p.url })}
                className="inline-flex items-center gap-1.5 rounded-full border border-border bg-card px-3 py-1 text-xs font-medium text-muted-foreground hover:border-primary hover:text-primary transition"
              >
                <img src={p.url} alt={p.name} className="size-4 rounded-full object-cover" />
                {p.name}
              </button>
            ))}
            {settings.logoUrl && (
              <button
                type="button"
                onClick={() => setSettings({ ...settings, logoUrl: "" })}
                className="text-xs text-danger hover:underline ml-auto"
              >
                Clear Logo
              </button>
            )}
          </div>

          {settings.logoUrl && (
            <div className="mt-4 flex items-center gap-3 rounded-lg border border-border bg-accent/30 p-3">
              <div className="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-border bg-card">
                <img src={settings.logoUrl} alt="Logo Preview" className="size-full object-contain" />
              </div>
              <div>
                <p className="text-xs font-semibold text-foreground">Logo Preview</p>
                <p className="text-[11px] text-muted-foreground">This logo will display in the navigation header across all portals.</p>
              </div>
            </div>
          )}
        </Panel>

        <Panel title="Public Contact Information">
          <p className="mb-3 text-xs text-muted-foreground">
            Exposed to public-facing clients via the /api/system/settings endpoint.
          </p>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Contact Phone">
              <input
                className={inputClass}
                placeholder="e.g. 09123456789"
                value={settings.contactPhone ?? ""}
                onChange={(e) => setSettings({ ...settings, contactPhone: e.target.value })}
              />
            </Field>
            <Field label="Contact Email">
              <input
                type="email"
                className={inputClass}
                placeholder="e.g. info@hospital.com"
                value={settings.contactEmail ?? ""}
                onChange={(e) => setSettings({ ...settings, contactEmail: e.target.value })}
              />
            </Field>
            <Field label="Operating Hours">
              <input
                className={inputClass}
                placeholder="e.g. 09:00 AM - 04:30 PM"
                value={settings.operatingHours ?? ""}
                onChange={(e) => setSettings({ ...settings, operatingHours: e.target.value })}
              />
            </Field>
          </div>
        </Panel>

        <Panel title="Regional Settings">
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Time Zone">
              <select
                className={inputClass}
                value={settings.timeZone}
                onChange={(e) => setSettings({ ...settings, timeZone: e.target.value })}
              >
                <option>Asia/Yangon (GMT+6:30)</option>
                <option>Asia/Bangkok (GMT+7)</option>
                <option>Asia/Singapore (GMT+8)</option>
                <option>UTC</option>
              </select>
            </Field>
            <Field label="Date Format">
              <select
                className={inputClass}
                value={settings.dateFormat}
                onChange={(e) => setSettings({ ...settings, dateFormat: e.target.value })}
              >
                <option>DD/MM/YYYY</option>
                <option>MM/DD/YYYY</option>
                <option>YYYY-MM-DD</option>
              </select>
            </Field>
            <Field label="Time Format">
              <select
                className={inputClass}
                value={settings.timeFormat}
                onChange={(e) => setSettings({ ...settings, timeFormat: e.target.value })}
              >
                <option>12 Hour</option>
                <option>24 Hour</option>
              </select>
            </Field>
          </div>
        </Panel>

        <div className="flex items-center gap-4">
          <button className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90">
            Save Settings
          </button>
          {saved ? <span className="text-sm text-success">Settings saved.</span> : null}
        </div>
      </form>
    </AdminLayout>
  );
}
