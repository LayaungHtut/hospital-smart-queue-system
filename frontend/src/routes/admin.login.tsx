import { createFileRoute } from "@tanstack/react-router";
import { LoginCard } from "@/components/portal/LoginCard";

export const Route = createFileRoute("/admin/login")({
  head: () => ({
    meta: [
      { title: "Admin Login — Hospital Smart Queue" },
      { name: "description", content: "Administrator sign in for hospital queue configuration." },
      { property: "og:title", content: "Admin Login — Hospital Smart Queue" },
      { property: "og:description", content: "Administrator sign in for system configuration." },
    ],
  }),
  component: AdminLogin,
});

function AdminLogin() {
  return (
    <LoginCard
      role="ADMIN"
      title="Admin Portal"
      subtitle="System administration sign in"
      redirectTo="/admin"
    />
  );
}
