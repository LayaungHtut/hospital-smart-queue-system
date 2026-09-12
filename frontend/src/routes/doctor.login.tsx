import { createFileRoute } from "@tanstack/react-router";
import { LoginCard } from "@/components/portal/LoginCard";

export const Route = createFileRoute("/doctor/login")({
  head: () => ({
    meta: [
      { title: "Doctor Login — Hospital Smart Queue" },
      {
        name: "description",
        content: "Doctor portal login for queue management and consultations.",
      },
      { property: "og:title", content: "Doctor Login — Hospital Smart Queue" },
      { property: "og:description", content: "Doctor portal login." },
    ],
  }),
  component: DoctorLoginPage,
});

function DoctorLoginPage() {
  return (
    <LoginCard
      role="DOCTOR"
      title="Doctor Portal"
      subtitle="Sign in with your Doctor ID to manage patient queues"
      redirectTo="/doctor"
    />
  );
}
