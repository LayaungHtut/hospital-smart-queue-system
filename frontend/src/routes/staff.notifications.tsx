import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getStaffNotifications, markAllStaffNotificationsRead } from "@/services/api";
import { StaffLayout } from "@/components/portal/shells";
import { NotificationList } from "@/components/portal/NotificationList";
import { Panel } from "@/components/portal/ui-kit";
import { SkeletonList } from "@/components/ui/loading";
import type { NotificationItem } from "@/types";

export const Route = createFileRoute("/staff/notifications")({
  head: () => ({
    meta: [
      { title: "Notifications — Staff Portal" },
      {
        name: "description",
        content: "Emergency alerts, reassignments and doctor status updates.",
      },
      { property: "og:title", content: "Notifications — Staff Portal" },
      { property: "og:description", content: "Emergency alerts and doctor status updates." },
    ],
  }),
  component: StaffNotificationsPage,
});

function StaffNotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStaffNotifications()
      .then((data) => {
        setItems(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  async function markAll() {
    await markAllStaffNotificationsRead();
    setItems((prev) => prev.map((n) => ({ ...n, read: true })));
  }

  return (
    <StaffLayout title="Notifications">
      <Panel>
        {loading ? (
          <SkeletonList items={5} />
        ) : (
          <NotificationList items={items} onMarkAll={markAll} />
        )}
      </Panel>
    </StaffLayout>
  );
}
