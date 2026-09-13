import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getQueueSettings, saveQueueSettings } from "@/services/api";
import { AdminLayout } from "@/components/portal/shells";
import { Field, Panel, inputClass } from "@/components/portal/ui-kit";
import { SkeletonCard } from "@/components/ui/loading";
import type { QueueSettings } from "@/types";

export const Route = createFileRoute("/admin/queue-settings")({
  head: () => ({
    meta: [
      { title: "Queue Settings — Admin Portal" },
      {
        name: "description",
        content: "Configure registration windows, waiting limits and priority rules.",
      },
      { property: "og:title", content: "Queue Settings — Admin Portal" },
      { property: "og:description", content: "Configure registration windows and queue rules." },
    ],
  }),
  component: QueueSettingsPage,
});

function QueueSettingsPage() {
  const [settings, setSettings] = useState<QueueSettings | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getQueueSettings().then(setSettings).catch(console.error);
  }, []);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!settings) return;
    await saveQueueSettings(settings);
    setSaved(true);
  }

  if (!settings) {
    return (
      <AdminLayout title="Queue Settings">
        <SkeletonCard />
      </AdminLayout>
    );
  }

  return (
    <AdminLayout title="Queue Settings">
      <form onSubmit={save} className="space-y-6">
        <Panel title="Registration Window">
          <p className="mb-4 text-sm text-muted-foreground">
            New queues (online or walk-in) are only accepted between these times, and never during
            the break window below. Emergency cases always bypass both.
          </p>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Registration Start Time">
              <input
                type="time"
                className={inputClass}
                value={settings.registrationStartTime}
                onChange={(e) =>
                  setSettings({ ...settings, registrationStartTime: e.target.value })
                }
              />
            </Field>
            <Field label="Registration End Time">
              <input
                type="time"
                className={inputClass}
                value={settings.registrationEndTime}
                onChange={(e) => setSettings({ ...settings, registrationEndTime: e.target.value })}
              />
            </Field>
            <Field label="Break Start Time">
              <input
                type="time"
                className={inputClass}
                value={settings.breakStartTime}
                onChange={(e) => setSettings({ ...settings, breakStartTime: e.target.value })}
              />
            </Field>
            <Field label="Break End Time">
              <input
                type="time"
                className={inputClass}
                value={settings.breakEndTime}
                onChange={(e) => setSettings({ ...settings, breakEndTime: e.target.value })}
              />
            </Field>
          </div>
        </Panel>

        <Panel title="Queue Rules">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Maximum Waiting Time (minutes)">
              <input
                type="number"
                min={5}
                className={inputClass}
                value={settings.maxWaitingMinutes}
                onChange={(e) =>
                  setSettings({ ...settings, maxWaitingMinutes: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Notify Patient Before (turns)">
              <input
                type="number"
                min={1}
                className={inputClass}
                value={settings.notifyBeforeTurns}
                onChange={(e) =>
                  setSettings({ ...settings, notifyBeforeTurns: Number(e.target.value) })
                }
              />
            </Field>
          </div>
          <div className="mt-4 space-y-3">
            <Toggle
              label="Auto-cancel queue after a missed turn"
              checked={settings.autoCancelAfterMissedTurn}
              onChange={(v) => setSettings({ ...settings, autoCancelAfterMissedTurn: v })}
            />
            <Toggle
              label="Allow patients to book future appointments"
              checked={settings.allowFutureBooking}
              onChange={(v) => setSettings({ ...settings, allowFutureBooking: v })}
            />
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

function Toggle({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex items-center gap-3 text-sm text-foreground">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="size-4 rounded border-border accent-(--color-primary)"
      />
      {label}
    </label>
  );
}
