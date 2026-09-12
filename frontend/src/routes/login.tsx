import { createFileRoute, Link } from "@tanstack/react-router";
import { LoginCard } from "@/components/portal/LoginCard";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Patient Login — Hospital Smart Queue" },
      { name: "description", content: "Sign in to book a queue and track your waiting position." },
      { property: "og:title", content: "Patient Login — Hospital Smart Queue" },
      { property: "og:description", content: "Sign in to book a queue and track your position." },
    ],
  }),
  component: PatientLogin,
});

function PatientLogin() {
  return (
    <LoginCard
      role="PATIENT"
      title="Patient Login"
      subtitle="Sign in to manage your hospital queue"
      redirectTo="/dashboard"
      footer={
        <p className="text-center text-sm text-muted-foreground">
          New patient?{" "}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Create an account
          </Link>
        </p>
      }
    />
  );
}
