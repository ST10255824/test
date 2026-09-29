interface KpiCardProps {
  icon: string;
  label: string;
  value: string;
  accent: string;
}

export function KpiCard({ icon, label, value, accent }: KpiCardProps) {
  return (
    <div className="card" style={{ flex: 1, minWidth: 180 }}>
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: "50%",
          background: `${accent}26`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 18,
          marginBottom: 12
        }}
      >
        {icon}
      </div>
      <div style={{ fontSize: 26, fontWeight: 800 }}>{value}</div>
      <div style={{ fontSize: 13, color: "var(--text-secondary)", marginTop: 2 }}>{label}</div>
    </div>
  );
}
