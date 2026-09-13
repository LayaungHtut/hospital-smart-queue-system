import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { Bell, Menu, Clock } from "lucide-react";
import { useState, useEffect, type ReactNode } from "react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";
import { getPatientNotifications } from "@/services/api";

const NOTIFICATION_POLL_MS = 15_000;

export interface NavItem {
  label: string;
  to: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: number;
  exact?: boolean;
}

interface PortalShellProps {
  portal: "patient" | "doctor" | "staff" | "admin";
  brand: string;
  brandSub?: string;
  title: string;
  nav: NavItem[];
  userName: string;
  userRole: string;
  showClock?: boolean;
  children: ReactNode;
}

export function PortalShell({
  portal,
  brand,
  brandSub,
  title,
  nav,
  userName,
  userRole,
  showClock = true,
  children,
}: PortalShellProps) {
  const [open, setOpen] = useState(false);
  const [currentTime, setCurrentTime] = useState<string>("");
  const [dynamicBrand, setDynamicBrand] = useState(brand);
  const [dynamicLogo, setDynamicLogo] = useState<string | null>(null);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const navigate = useNavigate();
  const { session, signOut, isReady } = useAuth();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  useEffect(() => {
    if (portal !== "patient" || !session?.userId) return;
    let active = true;
    async function poll() {
      try {
        const list = await getPatientNotifications(session?.userId);
        if (active) setUnreadNotifications(list.filter((n) => !n.read).length);
      } catch (err) {
        console.error(err);
      }
    }
    poll();
    const interval = setInterval(poll, NOTIFICATION_POLL_MS);
    // Refetch immediately when another part of the app (e.g. the
    // notifications page's "Mark all as read") changes read state, instead
    // of waiting for the next poll tick.
    window.addEventListener("patient_notifications_updated", poll);
    return () => {
      active = false;
      clearInterval(interval);
      window.removeEventListener("patient_notifications_updated", poll);
    };
  }, [portal, session?.userId]);

  useEffect(() => {
    const savedName = localStorage.getItem("system_hospital_name");
    const savedLogo = localStorage.getItem("system_logo_url");
    if (savedName) setDynamicBrand(savedName);
    if (savedLogo) setDynamicLogo(savedLogo);

    function onSettingsChange() {
      const n = localStorage.getItem("system_hospital_name");
      const l = localStorage.getItem("system_logo_url");
      if (n) setDynamicBrand(n);
      setDynamicLogo(l || null);
    }
    window.addEventListener("system_settings_updated", onSettingsChange);
    return () => window.removeEventListener("system_settings_updated", onSettingsChange);
  }, [brand]);

  useEffect(() => {
    function update() {
      setCurrentTime(
        new Date().toLocaleTimeString("en-US", {
          hour: "numeric",
          minute: "2-digit",
          hour12: true,
        }),
      );
    }
    update();
    const timer = setInterval(update, 30000);
    return () => clearInterval(timer);
  }, []);

  const loginPath =
    portal === "patient"
      ? "/login"
      : portal === "doctor"
        ? "/doctor/login"
        : portal === "staff"
          ? "/staff/login"
          : "/admin/login";

  const notificationsPath =
    portal === "patient"
      ? "/notifications"
      : portal === "doctor"
        ? "/doctor/appointments"
        : portal === "staff"
          ? "/staff/notifications"
          : "/admin/audit-logs";

  const expectedRole = portal.toUpperCase();
  const isAuthenticated = !!(session && session.role === expectedRole);

  useEffect(() => {
    // Wait for the auth session to be restored from localStorage first -
    // otherwise this fires on the one render where `session` is still seeded
    // as null (to match the server-rendered HTML) and redirects an
    // already-logged-in user to the login page.
    if (isReady && !isAuthenticated) {
      navigate({ to: loginPath, replace: true });
    }
  }, [isReady, isAuthenticated, loginPath, navigate]);

  function handleLogout() {
    signOut();
    navigate({ to: loginPath, replace: true });
  }

  if (!isAuthenticated) {
    return null;
  }

  const effectiveUserName = session?.name || userName || "User";
  const userInitials = effectiveUserName.trim().slice(0, 2).toUpperCase();

  return (
    <div
      className={cn(
        "flex h-screen bg-background overflow-hidden",
        portal === "staff" && "theme-staff",
      )}
    >
      <aside
        className={cn(
          "sticky top-0 z-40 flex h-full w-64 flex-col bg-sidebar text-sidebar-foreground transition-transform lg:static lg:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex items-center gap-3 border-b border-sidebar-border px-5 py-5">
          {dynamicLogo ? (
            <div className="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-sidebar-border bg-sidebar-primary/10">
              <img src={dynamicLogo} alt="Hospital Logo" className="size-full object-contain" />
            </div>
          ) : (
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground font-bold">
              +
            </div>
          )}
          <div className="leading-tight overflow-hidden">
            <p className="text-sm font-semibold truncate">{dynamicBrand}</p>
            {brandSub ? <p className="text-xs opacity-70 truncate">{brandSub}</p> : null}
          </div>
        </div>

        <div className="flex items-center gap-3 border-b border-sidebar-border px-5 py-4">
          <div className="flex size-10 items-center justify-center rounded-full bg-sidebar-accent text-sm font-semibold text-sidebar-accent-foreground">
            {userInitials}
          </div>
          <div className="leading-tight">
            <p className="text-sm font-semibold">{effectiveUserName}</p>
            <p className="text-xs opacity-70">{userRole}</p>
            <p className="mt-1 flex items-center gap-1 text-[11px] opacity-80">
              <span className="size-1.5 rounded-full bg-success" /> Online
            </p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {nav.map((item) => {
            const active = item.exact ? pathname === item.to : pathname.startsWith(item.to);
            const Icon = item.icon;
            return (
              <Link
                key={item.to}
                to={item.to}
                onClick={() => setOpen(false)}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  active
                    ? "bg-sidebar-primary text-sidebar-primary-foreground"
                    : "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                )}
              >
                <Icon className="size-4 shrink-0" />
                <span className="flex-1 truncate">{item.label}</span>
                {item.badge ? (
                  <span className="rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-semibold text-danger-foreground">
                    {item.badge}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>

        <button
          onClick={handleLogout}
          className="m-3 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
        >
          <span className="text-base leading-none">⏻</span> Logout
        </button>
      </aside>

      {open ? (
        <div
          className="fixed inset-0 z-30 bg-foreground/40 lg:hidden"
          onClick={() => setOpen(false)}
          aria-hidden
        />
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col min-h-0 overflow-y-auto">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-border bg-card px-4 py-3 sm:px-6">
          <button
            className="rounded-md p-1.5 text-muted-foreground hover:bg-muted lg:hidden"
            onClick={() => setOpen((v) => !v)}
            aria-label="Toggle navigation"
          >
            <Menu className="size-5" />
          </button>
          <h1 className="truncate text-lg font-semibold text-foreground">{title}</h1>
          <div className="ml-auto flex items-center gap-4">
            {showClock && currentTime ? (
              <span className="hidden items-center gap-1.5 text-sm text-muted-foreground sm:flex">
                <Clock className="size-4" /> {currentTime}
              </span>
            ) : null}
            <Link
              to={notificationsPath}
              className="relative p-1 text-muted-foreground hover:text-foreground transition-colors"
              title="Notifications"
            >
              <Bell className="size-5" />
              {unreadNotifications > 0 ? (
                <span className="absolute -right-0.5 -top-0.5 flex size-4 items-center justify-center rounded-full bg-danger text-[10px] font-semibold text-danger-foreground">
                  {unreadNotifications > 9 ? "9+" : unreadNotifications}
                </span>
              ) : null}
            </Link>
            <span className="hidden text-sm font-medium text-foreground sm:inline">{userName}</span>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
