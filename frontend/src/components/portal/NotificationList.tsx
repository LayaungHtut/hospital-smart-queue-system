import { useState } from "react";
import {
  AlertTriangle,
  Bell,
  CalendarClock,
  CheckCheck,
  CircleCheck,
  ListOrdered,
} from "lucide-react";
import { TabNav } from "./ui-kit";
import { cn } from "@/lib/utils";
import type { NotificationItem } from "@/types";

const iconFor: Record<NotificationItem["category"], React.ComponentType<{ className?: string }>> = {
  EMERGENCY: AlertTriangle,
  QUEUE: ListOrdered,
  DOCTOR: CircleCheck,
  SCHEDULE: CalendarClock,
};

const toneFor: Record<NotificationItem["category"], string> = {
  EMERGENCY: "bg-danger-soft text-danger",
  QUEUE: "bg-info-soft text-info",
  DOCTOR: "bg-success-soft text-success",
  SCHEDULE: "bg-warning-soft text-warning",
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
        <ul className="divide-y divide-border">
          {filtered.map((n) => {
            const Icon = iconFor[n.category];
            return (
              <li key={n.id} className="flex items-start gap-4 py-4">
                <span
                  className={cn(
                    "flex size-9 shrink-0 items-center justify-center rounded-full",
                    toneFor[n.category],
                  )}
                >
                  <Icon className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{n.title}</p>
                  <p className="text-sm text-muted-foreground">{n.message}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2 text-xs text-muted-foreground">
                  {n.time}
                  {!n.read ? <span className="size-2 rounded-full bg-danger" /> : null}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
