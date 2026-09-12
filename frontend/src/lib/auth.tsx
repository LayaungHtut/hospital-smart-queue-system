import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthResponse, UserRole } from "@/types";

export const STORAGE_KEY = "hqs.session";

export function getStoredSession(): AuthResponse | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

interface AuthContextValue {
  session: AuthResponse | null;
  signIn: (session: AuthResponse) => void;
  signOut: () => void;
  hasRole: (role: UserRole) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(() => getStoredSession());

  useEffect(() => {
    function handleStorage() {
      setSession(getStoredSession());
    }
    function handlePageShow(event: PageTransitionEvent) {
      setSession(getStoredSession());
    }
    window.addEventListener("storage", handleStorage);
    window.addEventListener("pageshow", handlePageShow);
    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, []);

  const signIn = useCallback((next: AuthResponse) => {
    setSession(next);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      try {
        window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      } catch {}
    }
  }, []);

  const signOut = useCallback(() => {
    setSession(null);
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(STORAGE_KEY);
      try {
        window.sessionStorage.clear();
      } catch {}
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      signIn,
      signOut,
      hasRole: (role) => session?.role === role,
    }),
    [session, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
