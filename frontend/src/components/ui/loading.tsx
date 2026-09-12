import { cn } from "@/lib/utils";

function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("animate-pulse rounded-md bg-primary/10", className)} {...props} />;
}

export function LoadingSpinner({
  size = "md",
  className,
}: {
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const sizeClasses = {
    sm: "size-4",
    md: "size-6",
    lg: "size-10",
  };
  return (
    <svg
      className={cn("animate-spin text-primary", sizeClasses[size], className)}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={2}
      stroke="currentColor"
      aria-hidden="true"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" />
      <path
        className="opacity-75"
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 2a10 10 0 0 1 10 10"
      />
    </svg>
  );
}

export function LoadingOverlay({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm",
        className,
      )}
    >
      <div className="flex flex-col items-center gap-4 rounded-xl bg-card p-6 shadow-lg border border-border">
        <LoadingSpinner size="lg" />
        <p className="text-sm font-medium text-muted-foreground">Loading...</p>
      </div>
    </div>
  );
}

export function SkeletonCard({ className }: { className?: string }) {
  return (
    <div className={cn("rounded-xl border border-border bg-card p-5 shadow-sm", className)}>
      <Skeleton className="h-4 w-3/4 mb-3" />
      <Skeleton className="h-8 w-1/2" />
      <Skeleton className="h-4 w-1/4 mt-3" />
    </div>
  );
}

export function SkeletonStatCard({ className }: { className?: string }) {
  return (
    <div className={cn("rounded-xl border border-border bg-card p-5 shadow-sm", className)}>
      <Skeleton className="h-4 w-3/4 mb-3" />
      <Skeleton className="h-10 w-1/3 mb-2" />
      <Skeleton className="h-3 w-1/4" />
    </div>
  );
}

export function SkeletonTable({
  rows = 5,
  columns = 4,
  className,
}: {
  rows?: number;
  columns?: number;
  className?: string;
}) {
  return (
    <div className={cn("overflow-x-auto rounded-lg border border-border", className)}>
      <table className="w-full min-w-[640px] text-sm">
        <thead className="bg-muted/60 text-left text-muted-foreground">
          <tr>
            <th className="px-4 py-3 font-medium">#</th>
            {Array.from({ length: columns }).map((_, i) => (
              <th key={i} className="px-4 py-3 font-medium">
                <Skeleton className="h-4 w-3/4" />
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {Array.from({ length: rows }).map((_, i) => (
            <tr key={i} className="bg-card">
              <td className="px-4 py-3 text-muted-foreground">
                <Skeleton className="h-4 w-6" />
              </td>
              {Array.from({ length: columns }).map((_, j) => (
                <td key={j} className="px-4 py-3">
                  <Skeleton className="h-4 w-full" />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function SkeletonChart({
  height = 200,
  className,
}: {
  height?: number;
  className?: string;
}) {
  return (
    <div
      className={cn("rounded-lg border border-border bg-card p-5", className)}
      style={{ height }}
    >
      <Skeleton className="h-4 w-1/4 mb-4" />
      <Skeleton className="w-full h-full" />
    </div>
  );
}

export function SkeletonList({ items = 5, className }: { items?: number; className?: string }) {
  return (
    <div className={cn("space-y-3", className)}>
      {Array.from({ length: items }).map((_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="size-10 rounded-lg" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function SkeletonPanel({
  title = true,
  action = false,
  className,
}: {
  title?: boolean;
  action?: boolean;
  className?: string;
}) {
  return (
    <div className={cn("rounded-xl border border-border bg-card p-5 shadow-sm", className)}>
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          {title ? <Skeleton className="h-5 w-1/4" /> : null}
          {action ? <Skeleton className="h-6 w-20" /> : null}
        </div>
      )}
      <SkeletonList items={3} />
    </div>
  );
}

export { Skeleton };
