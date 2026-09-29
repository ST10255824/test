import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../lib/AuthContext";

export function LoginPage() {
  const { user, loading, error, signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (!loading && user) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await signIn(email, password);
    } catch {
      // error surfaced via useAuth().error
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "linear-gradient(160deg, var(--navy-deep), var(--navy))"
      }}
    >
      <form onSubmit={handleSubmit} className="card" style={{ width: 380, textAlign: "center" }}>
        <div
          style={{
            width: 72,
            height: 72,
            borderRadius: "50%",
            background: "var(--navy)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 32,
            margin: "0 auto 16px"
          }}
        >
          🧺
        </div>
        <h1 style={{ fontSize: 22, fontWeight: 800, margin: "0 0 2px" }}>MACKSON'S</h1>
        <p style={{ fontSize: 12, letterSpacing: 2, color: "var(--gold)", fontWeight: 700, margin: "0 0 24px" }}>
          ADMIN CONSOLE
        </p>

        {error && (
          <div
            style={{
              background: "var(--error-bg)",
              color: "var(--error)",
              borderRadius: 12,
              padding: "10px 14px",
              fontSize: 13,
              marginBottom: 16,
              textAlign: "left"
            }}
          >
            {error}
          </div>
        )}

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          style={{ width: "100%", marginBottom: 10 }}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          style={{ width: "100%", marginBottom: 20 }}
        />
        <button type="submit" className="pill-button primary" disabled={submitting} style={{ width: "100%" }}>
          {submitting ? "Signing in…" : "Log in"}
        </button>
        <p style={{ fontSize: 12, color: "var(--text-secondary)", marginTop: 18 }}>
          Store Manager / Admin accounts only. Customers, pickers, and drivers use the Mackson's
          Delivery mobile app.
        </p>
      </form>
    </div>
  );
}
