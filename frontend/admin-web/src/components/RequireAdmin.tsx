import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "../lib/AuthContext";

const ALLOWED_ROLES = ["ADMIN", "MANAGER"];

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, role, loading, signOut } = useAuth();

  if (loading) {
    return (
      <div style={{ display: "flex", height: "100vh", alignItems: "center", justifyContent: "center" }}>
        Loading…
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!role || !ALLOWED_ROLES.includes(role)) {
    return (
      <div style={{ display: "flex", height: "100vh", alignItems: "center", justifyContent: "center", flexDirection: "column", gap: 16 }}>
        <h2>This account isn't an Admin or Store Manager account.</h2>
        <p style={{ color: "var(--text-secondary)" }}>
          Signed in as {user.email}. This console is only for Store Manager / Admin accounts —
          customers, pickers, and drivers use the mobile app instead.
        </p>
        <button className="pill-button primary" onClick={signOut}>
          Sign out
        </button>
      </div>
    );
  }

  return <>{children}</>;
}
