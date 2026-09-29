import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../lib/AuthContext";

const navItems = [
  { to: "/", label: "Dashboard", icon: "📊", end: true },
  { to: "/orders", label: "Orders", icon: "🧾" },
  { to: "/inventory", label: "Inventory", icon: "📦" },
  { to: "/customers", label: "Customers", icon: "👥" },
  { to: "/pickers", label: "Pickers", icon: "🧺" },
  { to: "/drivers", label: "Drivers", icon: "🚚" }
];

/** Sidebar + top bar shell, styled after the "Administrator App" dashboard mockup in the Part 1
 * prototype (dark navy sidebar, MACKSONS wordmark, active item highlighted). */
export function Layout() {
  const { user, role, signOut } = useAuth();

  return (
    <div className="admin-shell">
      <aside
        className="admin-sidebar"
        style={{
          background: "var(--navy-deep)",
          color: "white",
          display: "flex",
          flexDirection: "column",
          padding: "24px 16px"
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "0 8px 28px" }}>
          <span style={{ fontSize: 26 }}>🧺</span>
          <div>
            <div style={{ fontWeight: 800, fontSize: 17, color: "var(--gold)", letterSpacing: 0.5 }}>MACKSON'S</div>
            <div style={{ fontSize: 10, letterSpacing: 1.5, color: "#9fb0c9" }}>ADMIN CONSOLE</div>
          </div>
        </div>

        <nav style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1 }}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              style={({ isActive }) => ({
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "11px 14px",
                borderRadius: 12,
                textDecoration: "none",
                fontWeight: 600,
                fontSize: 14,
                color: isActive ? "var(--navy-deep)" : "#d7e0f0",
                background: isActive ? "var(--gold)" : "transparent"
              })}
            >
              <span aria-hidden>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div style={{ borderTop: "1px solid rgba(255,255,255,0.12)", paddingTop: 16, marginTop: 16 }}>
          <div style={{ fontSize: 13, color: "#d7e0f0", marginBottom: 2 }}>{user?.email}</div>
          <div style={{ fontSize: 11, color: "var(--gold-light)", marginBottom: 12 }}>{role}</div>
          <button className="pill-button outline" style={{ width: "100%", color: "white", borderColor: "rgba(255,255,255,0.25)" }} onClick={signOut}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
