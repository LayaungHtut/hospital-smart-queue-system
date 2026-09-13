import { useMemo, useState, type ReactNode } from "react";
import {
  AlertTriangle,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Info,
  Search,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";

/* -------------------------------- Alert ---------------------------------- */

export type AlertTone = "info" | "success" | "warning" | "error";

const alertToneClasses: Record<AlertTone, string> = {
  info: "bg-info-soft text-info",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
  error: "bg-danger-soft text-danger",
};

const alertToneIcon: Record<AlertTone, React.ComponentType<{ className?: string }>> = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  error: AlertTriangle,
};

/**
 * A daisyUI-style alert: an icon + message in a colored, rounded box.
 * `role="alert"` so screen readers announce it as soon as it appears.
 */
export function Alert({
  tone = "info",
  children,
  onDismiss,
  className,
}: {
  tone?: AlertTone;
  children: ReactNode;
  onDismiss?: () => void;
  className?: string | undefined;
}) {
  const Icon = alertToneIcon[tone];
  return (
    <div
      role="alert"
      className={cn(
        "flex items-start gap-3 rounded-lg px-4 py-3 text-sm shadow-sm",
        alertToneClasses[tone],
        className,
      )}
    >
      <Icon className="mt-0.5 size-5 shrink-0 stroke-current" />
      <div className="min-w-0 flex-1">{children}</div>
      {onDismiss ? (
        <button
          onClick={onDismiss}
          aria-label="Dismiss"
          className="shrink-0 rounded p-0.5 opacity-70 transition hover:opacity-100"
        >
          <X className="size-4" />
        </button>
      ) : null}
    </div>
  );
}

/* -------------------------------- Panel ---------------------------------- */

export function Panel({
  title,
  action,
  className,
  children,
}: {
  title?: string;
  action?: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section className={cn("rounded-xl border border-border bg-card p-5 shadow-sm", className)}>
      {title || action ? (
        <div className="mb-4 flex items-center justify-between gap-3">
          {title ? <h2 className="text-base font-semibold text-foreground">{title}</h2> : <span />}
          {action}
        </div>
      ) : null}
      {children}
    </section>
  );
}

/* ------------------------------- StatCard --------------------------------- */

export function StatCard({
  label,
  value,
  caption,
  tone = "primary",
  icon,
  footer,
}: {
  label: string;
  value: ReactNode;
  caption?: string;
  tone?: "primary" | "success" | "warning" | "danger";
  icon?: ReactNode;
  footer?: ReactNode;
}) {
  const toneClass = {
    primary: "text-primary",
    success: "text-success",
    warning: "text-warning",
    danger: "text-danger",
  }[tone];

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-medium text-muted-foreground">{label}</p>
        {icon}
      </div>
      <p className={cn("mt-3 text-3xl font-bold tracking-tight", toneClass)}>{value}</p>
      {caption ? <p className="mt-1 text-sm text-muted-foreground">{caption}</p> : null}
      {footer ? <div className="mt-3 text-sm">{footer}</div> : null}
    </div>
  );
}

/* -------------------------------- Stepper --------------------------------- */

export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="mb-8 flex items-center">
      {steps.map((step, i) => {
        const done = i < current;
        const active = i === current;
        return (
          <li key={step} className="flex flex-1 items-center last:flex-none">
            <div className="flex flex-col items-center gap-2">
              <span
                className={cn(
                  "flex size-8 items-center justify-center rounded-full border text-sm font-semibold",
                  done && "border-primary bg-primary text-primary-foreground",
                  active && "border-primary bg-primary text-primary-foreground",
                  !done && !active && "border-border bg-card text-muted-foreground",
                )}
              >
                {done ? <Check className="size-4" /> : i + 1}
              </span>
              <span
                className={cn(
                  "hidden whitespace-nowrap text-xs font-medium sm:block",
                  active || done ? "text-foreground" : "text-muted-foreground",
                )}
              >
                {step}
              </span>
            </div>
            {i < steps.length - 1 ? (
              <span
                className={cn("mx-2 mb-6 h-px flex-1", done ? "bg-primary" : "bg-border")}
                aria-hidden
              />
            ) : null}
          </li>
        );
      })}
    </ol>
  );
}

/* ---------------------------- QueueTokenBadge ------------------------------ */

const urgencyStroke: Record<"general" | "priority" | "critical", string> = {
  general: "bg-success",
  priority: "bg-warning",
  critical: "bg-danger",
};

/**
 * Ticket-inspired queue token — see DESIGN.md "Queue Token Badges".
 * White/ivory surface, dashed divider between the station and the token
 * number, a left urgency stroke, and title-token numerals for the ID.
 */
export function QueueTokenBadge({
  token,
  station,
  urgency = "general",
  className,
}: {
  token: ReactNode;
  station: ReactNode;
  urgency?: "general" | "priority" | "critical";
  className?: string;
}) {
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-xl border border-border bg-card pl-5 shadow-sm",
        className,
      )}
    >
      <span className={cn("absolute inset-y-0 left-0 w-1", urgencyStroke[urgency])} aria-hidden />
      <div className="flex items-center gap-4 border-b border-dashed border-border px-4 py-2.5">
        <span className="text-[11px] font-bold uppercase tracking-[0.06em] text-muted-foreground">
          {station}
        </span>
      </div>
      <div className="px-4 py-3.5">
        <span className="font-sans text-[40px] font-extrabold leading-11 tracking-[0.04em] text-foreground">
          {token}
        </span>
      </div>
    </div>
  );
}

/* ------------------------------ StatusBadge -------------------------------- */

const badgeTones: Record<string, string> = {
  ACTIVE: "bg-success-soft text-success",
  CONSULTING: "bg-success-soft text-success",
  COMPLETED: "bg-success-soft text-success",
  CONFIRMED: "bg-success-soft text-success",
  RESOLVED: "bg-success-soft text-success",
  NORMAL: "bg-success-soft text-success",
  LOW: "bg-success-soft text-success",
  ON_BREAK: "bg-warning-soft text-warning",
  MEDIUM: "bg-warning-soft text-warning",
  PENDING: "bg-warning-soft text-warning",
  WAITING: "bg-warning-soft text-warning",
  INACTIVE: "bg-danger-soft text-danger",
  UNAVAILABLE: "bg-danger-soft text-danger",
  HIGH: "bg-danger-soft text-danger",
  EMERGENCY: "bg-danger-soft text-danger",
  CANCELLED: "bg-danger-soft text-danger",
  MISSED: "bg-danger-soft text-danger",
  APPOINTMENT: "bg-info-soft text-info",
  CALLED: "bg-info-soft text-info",
  IN_CONSULTATION: "bg-info-soft text-info",
};

export function humanize(value: string | undefined | null) {
  if (!value) return "—";
  return value
    .toLowerCase()
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

export function StatusBadge({
  status,
  label,
}: {
  status: string | undefined | null;
  label?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        status
          ? (badgeTones[status] ?? "bg-muted text-muted-foreground")
          : "bg-muted text-muted-foreground",
      )}
    >
      {label ?? humanize(status)}
    </span>
  );
}

/* --------------------------------- TabNav ---------------------------------- */

export function TabNav({
  tabs,
  value,
  onChange,
}: {
  tabs: { value: string; label: string }[];
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="mb-5 flex gap-6 overflow-x-auto border-b border-border scrollbar-none">
      {tabs.map((tab) => (
        <button
          key={tab.value}
          onClick={() => onChange(tab.value)}
          className={cn(
            "-mb-px shrink-0 border-b-2 px-1 pb-3 text-sm font-medium transition-colors",
            value === tab.value
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground",
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

/* -------------------------------- DataTable -------------------------------- */

export interface Column<T> {
  header: string;
  cell: (row: T, index: number) => ReactNode;
  className?: string;
}

export function DataTable<T>({
  rows,
  columns,
  searchable = false,
  searchKeys,
  pageSize = 8,
  numbered = true,
  totalLabel,
  emptyMessage = "No records found.",
  toolbar,
  loading,
}: {
  rows: T[];
  columns: Column<T>[];
  searchable?: boolean;
  searchKeys?: (keyof T)[];
  pageSize?: number;
  numbered?: boolean;
  totalLabel?: string;
  emptyMessage?: string;
  toolbar?: ReactNode;
  loading?: boolean;
}) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);

  const filtered = useMemo(() => {
    if (!query.trim() || !searchKeys) return rows;
    const q = query.toLowerCase();
    return rows.filter((row) =>
      searchKeys.some((key) =>
        String(row[key] ?? "")
          .toLowerCase()
          .includes(q),
      ),
    );
  }, [rows, query, searchKeys]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const visible = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div>
      {(searchable || toolbar) && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          {searchable ? (
            <div className="relative w-full max-w-xs">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setPage(1);
                }}
                placeholder="Search..."
                className="w-full rounded-lg border border-input bg-card py-2 pl-9 pr-3 text-sm outline-none focus:border-primary"
              />
            </div>
          ) : (
            <span />
          )}
          {toolbar}
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full min-w-160 text-sm">
          <thead className="bg-muted/60 text-left text-muted-foreground">
            <tr>
              {numbered ? <th className="px-4 py-3 font-medium">#</th> : null}
              {columns.map((col) => (
                <th key={col.header} className={cn("px-4 py-3 font-medium", col.className)}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {loading ? (
              <tr>
                <td
                  colSpan={columns.length + (numbered ? 1 : 0)}
                  className="px-4 py-10 text-center text-muted-foreground"
                >
                  Loading...
                </td>
              </tr>
            ) : visible.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length + (numbered ? 1 : 0)}
                  className="px-4 py-10 text-center text-muted-foreground"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              visible.map((row, i) => (
                <tr key={i} className="bg-card hover:bg-muted/40">
                  {numbered ? (
                    <td className="px-4 py-3 text-muted-foreground">
                      {(currentPage - 1) * pageSize + i + 1}
                    </td>
                  ) : null}
                  {columns.map((col) => (
                    <td key={col.header} className={cn("px-4 py-3", col.className)}>
                      {col.cell(row, i)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {filtered.length > 0 ? (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
          <span className="text-xs sm:text-sm">
            {totalLabel ??
              `Showing ${(currentPage - 1) * pageSize + 1} to ${Math.min(
                currentPage * pageSize,
                filtered.length,
              )} of ${filtered.length}`}
          </span>
          {pageCount > 1 ? (
            <div className="flex items-center gap-1">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                className="rounded-md border border-border bg-card p-1.5 disabled:opacity-40"
                disabled={currentPage === 1}
                aria-label="Previous page"
              >
                <ChevronLeft className="size-4" />
              </button>
              {Array.from({ length: Math.min(pageCount, 5) }).map((_, i) => {
                let pageNum: number;
                if (pageCount <= 5) {
                  pageNum = i + 1;
                } else if (currentPage <= 3) {
                  pageNum = i + 1;
                } else if (currentPage >= pageCount - 2) {
                  pageNum = pageCount - 4 + i;
                } else {
                  pageNum = currentPage - 2 + i;
                }
                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    className={cn(
                      "size-8 rounded-md border text-xs font-medium",
                      currentPage === pageNum
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-card hover:bg-muted",
                    )}
                  >
                    {pageNum}
                  </button>
                );
              })}
              <button
                onClick={() => setPage((p) => Math.min(pageCount, p + 1))}
                className="rounded-md border border-border bg-card p-1.5 disabled:opacity-40"
                disabled={currentPage === pageCount}
                aria-label="Next page"
              >
                <ChevronRight className="size-4" />
              </button>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

/* --------------------------------- Fields ---------------------------------- */

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="mb-1.5 block font-medium text-foreground">{label}</span>
      {children}
    </label>
  );
}

export const inputClass =
  "w-full rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary";
