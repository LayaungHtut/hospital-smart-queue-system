import { createFileRoute } from "@tanstack/react-router";
import { PatientLayout } from "@/components/portal/shells";
import { Panel } from "@/components/portal/ui-kit";

export const Route = createFileRoute("/help")({
  head: () => ({
    meta: [
      { title: "Help & FAQ — Hospital Smart Queue" },
      {
        name: "description",
        content: "Answers about joining queues, waiting times and emergencies.",
      },
      { property: "og:title", content: "Help & FAQ — Hospital Smart Queue" },
      {
        property: "og:description",
        content: "Answers about queues, waiting times and emergencies.",
      },
    ],
  }),
  component: HelpPage,
});

const faqs = [
  {
    q: "How do I join a queue?",
    a: "Go to New Queue, select your symptoms, then pick a department and doctor. Your queue number is issued immediately.",
  },
  {
    q: "When should I arrive?",
    a: "Arrive at the hospital before your estimated waiting time ends. You will be notified 3 turns before your turn.",
  },
  {
    q: "What if I miss my turn?",
    a: "Missed turns are cancelled automatically. You can create a new queue if registration is still open.",
  },
  {
    q: "How are emergencies handled?",
    a: "Tell the front desk immediately. Staff will register an emergency case, which is prioritised above normal queues.",
  },
];

function HelpPage() {
  return (
    <PatientLayout title="Help">
      <div className="grid gap-6 lg:grid-cols-3">
        <Panel title="Frequently Asked Questions" className="lg:col-span-2">
          <dl className="divide-y divide-border">
            {faqs.map((f) => (
              <div key={f.q} className="py-4 first:pt-0 last:pb-0">
                <dt className="text-sm font-semibold text-foreground">{f.q}</dt>
                <dd className="mt-1 text-sm text-muted-foreground">{f.a}</dd>
              </div>
            ))}
          </dl>
        </Panel>
        <Panel title="Contact">
          <p className="text-sm text-muted-foreground">City Hospital reception</p>
          <p className="mt-2 text-sm font-medium text-foreground">09 987654321</p>
          <p className="text-sm font-medium text-foreground">help@hospital.com</p>
          <p className="mt-4 text-sm text-muted-foreground">
            Queue registration: 07:00 AM – 11:30 AM
          </p>
        </Panel>
      </div>
    </PatientLayout>
  );
}
