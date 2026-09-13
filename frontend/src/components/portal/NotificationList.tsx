import { useState } from "react";
import { Bell, CheckCheck } from "lucide-react";
import { Alert, TabNav, type AlertTone } from "./ui-kit";
import type { NotificationItem } from "@/types";

const toneFor: Record<NotificationItem["category"], AlertTone> = {
  EMERGENCY: "error",
  QUEUE: "info",
  DOCTOR: "success",
  SCHEDULE: "warning",
};

export function NotificationList({
  items,
  onMarkAll,
}: {
  items: NotificationItem[];
  onMarkAll: () => void;
}) {
  const [tab, setTab] = useState("all");

  const filtered =
    tab === "unread"
      ? items.filter((n) => !n.read)
      : tab === "important"
        ? items.filter((n) => n.important)
        : items;

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <TabNav
          value={tab}
          onChange={setTab}
          tabs={[
            { value: "all", label: `All (${items.length})` },
            { value: "unread", label: `Unread (${items.filter((n) => !n.read).length})` },
            { value: "important", label: "Important" },
          ]}
        />
        <button
          onClick={onMarkAll}
          className="mb-5 inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground"
        >
          <CheckCheck className="size-4" /> Mark all as read
        </button>
      </div>

      {filtered.length === 0 ? (
        <p className="py-10 text-center text-sm text-muted-foreground">
          <Bell className="mx-auto mb-2 size-6" />
          Nothing here yet.
        </p>
      ) : (
        <div className="space-y-3">
          {filtered.map((n) => (
            <Alert
              key={n.id}
              tone={toneFor[n.category]}
              className={!n.read ? "ring-1 ring-current/30" : ""}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-semibold">{n.title}</p>
                  <p className="opacity-90">{n.message}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2 text-xs opacity-80">
                  {n.time}
                  {!n.read ? <span className="size-2 rounded-full bg-current" /> : null}
                </div>
              </div>
            </Alert>
          ))}
        </div>
      )}
    </div>
  );
}
