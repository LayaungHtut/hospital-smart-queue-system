import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getPatientNotifications, markAllNotificationsRead } from "@/services/api";
import { PatientLayout } from "@/components/portal/shells";
import { NotificationList } from "@/components/portal/NotificationList";
import { Panel } from "@/components/portal/ui-kit";
import { useAuth } from "@/lib/auth";
import { SkeletonList } from "@/components/ui/loading";
import type { NotificationItem } from "@/types";

export const Route = createFileRoute("/notifications")({
  head: () => ({
    meta: [
      { title: "Notifications — Hospital Smart Queue" },
      { name: "description", content: "Queue alerts and appointment updates for your visits." },
      { property: "og:title", content: "Notifications — Hospital Smart Queue" },
      { property: "og:description", content: "Queue alerts and appointment updates." },
    ],
  }),
  component: PatientNotifications,
});

function PatientNotifications() {
  const { session } = useAuth();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [popup, setPopup] = useState<NotificationItem | null>(null);

  useEffect(() => {
    getPatientNotifications(session?.userId)
      .then((data) => {
        const unread = data.filter((n) => !n.read);
        if (unread[0]) {
          setPopup(unread[0]);
          setTimeout(() => setPopup(null), 4000);
        }
        setItems(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [session?.userId]);

  async function markAll() {
    await markAllNotificationsRead();
    setItems((prev) => prev.map((n) => ({ ...n, read: true })));
  }

  return (
    <PatientLayout title="Notifications">
      {popup && (
        <div className="mb-4 rounded-lg border border-primary/20 bg-primary/10 p-4 shadow-lg animate-in fade-in slide-in-from-top-2">
          <div className="flex items-start gap-3">
            <div className="mt-0.5 size-2 shrink-0 rounded-full bg-primary" />
            <div className="flex-1">
              <p className="text-sm font-semibold text-foreground">{popup.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{popup.message}</p>
            </div>
            <button onClick={() => setPopup(null)} className="text-xs text-muted-foreground hover:text-foreground">&times;</button>
          </div>
        </div>
      )}
      <Panel>
        {loading ? (
          <SkeletonList items={5} />
        ) : (
          <NotificationList items={items} onMarkAll={markAll} />
        )}
      </Panel>
    </PatientLayout>
  );
}
