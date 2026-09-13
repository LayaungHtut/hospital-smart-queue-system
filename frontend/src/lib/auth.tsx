import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
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
  /**
   * False until the stored session has been read on the client (see the
   * comment in AuthProvider). Callers that redirect unauthenticated users
   * away must wait for this to become true first, otherwise they'll redirect
   * an already-logged-in user during the one render where `session` is still
   * seeded as null.
   */
  isReady: boolean;
  signIn: (session: AuthResponse) => void;
  signOut: () => void;
  hasRole: (role: UserRole) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Must start as null on both server and client: the server has no
  // localStorage (getStoredSession() returns null there), so seeding this from
  // localStorage via a useState initializer would make the client's first
  // render already show the signed-in shell while the server rendered
  // nothing — a guaranteed hydration mismatch on every authenticated page.
  // Read the real session after mount instead, once hydration has settled.
  const [session, setSession] = useState<AuthResponse | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    setSession(getStoredSession());
    setIsReady(true);

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
      } catch {
        // Ignore session storage errors (e.g. storage disabled or quota exceeded)
      }
    }
  }, []);

  const signOut = useCallback(() => {
    setSession(null);
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(STORAGE_KEY);
      try {
        window.sessionStorage.clear();
      } catch {
        // Ignore session storage errors
      }
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isReady,
      signIn,
      signOut,
      hasRole: (role) => session?.role === role,
    }),
    [session, isReady, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
