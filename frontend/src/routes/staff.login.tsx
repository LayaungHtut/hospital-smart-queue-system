import { createFileRoute } from "@tanstack/react-router";
import { LoginCard } from "@/components/portal/LoginCard";

export const Route = createFileRoute("/staff/login")({
  head: () => ({
    meta: [
      { title: "Staff Login — Hospital Smart Queue" },
      {
        name: "description",
        content: "Front-desk staff sign in for queue and emergency handling.",
      },
      { property: "og:title", content: "Staff Login — Hospital Smart Queue" },
      { property: "og:description", content: "Front-desk staff sign in for queue management." },
    ],
  }),
  component: StaffLogin,
});

function StaffLogin() {
  return (
    <LoginCard
      role="STAFF"
      title="Staff Portal"
      subtitle="Front desk and reception sign in"
      redirectTo="/staff"
    />
  );
}
