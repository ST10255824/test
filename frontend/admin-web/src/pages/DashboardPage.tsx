import { useEffect, useMemo, useState } from "react";
import { collection, onSnapshot, query, where } from "firebase/firestore";
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { db, STORE_ID } from "../lib/firebase";
import type { DriverProfile, Order, OrderStatus } from "../lib/types";
import { KpiCard } from "../components/KpiCard";
import { StatusPill } from "../components/StatusPill";

const ACTIVE_STATUSES: OrderStatus[] = ["RECEIVED", "PICKING", "READY_FOR_COLLECTION", "EN_ROUTE"];
const CHART_COLORS: Record<string, string> = {
  RECEIVED: "#6b7280",
  PICKING: "#d97706",
  READY_FOR_COLLECTION: "#2563eb",
  EN_ROUTE: "#7c3aed",
  DELIVERED: "#17a34a"
};

function startOfToday(): number {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

/** Live operations dashboard — real-time Firestore listener rather than the 30-second poll the
 * Android admin screens used, since a browser tab can comfortably hold an open connection. */
export function DashboardPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [drivers, setDrivers] = useState<DriverProfile[]>([]);

  useEffect(() => {
    const ordersQuery = query(collection(db, "orders"), where("storeId", "==", STORE_ID));
    const unsubscribe = onSnapshot(ordersQuery, (snapshot) => {
      setOrders(snapshot.docs.map((d) => ({ orderId: d.id, ...d.data() }) as Order));
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    const driversQuery = collection(db, "drivers");
    const unsubscribe = onSnapshot(driversQuery, (snapshot) => {
      setDrivers(snapshot.docs.map((d) => ({ driverId: d.id, ...d.data() }) as DriverProfile));
    });
    return unsubscribe;
  }, []);

  const todayStart = useMemo(() => startOfToday(), []);
  const ordersToday = orders.filter((o) => o.placementTime >= todayStart);
  const revenueToday = ordersToday.reduce((sum, o) => sum + (o.totalAmount ?? 0), 0);
  const pendingOrders = orders.filter((o) => ACTIVE_STATUSES.includes(o.orderStatus));
  const activeDrivers = drivers.filter((d) => d.isAvailable).length;

  const statusCounts = orders.reduce<Record<string, number>>((acc, o) => {
    acc[o.orderStatus] = (acc[o.orderStatus] ?? 0) + 1;
    return acc;
  }, {});
  const chartData = Object.entries(statusCounts).map(([status, count]) => ({ name: status, value: count }));

  const recentOrders = [...orders].sort((a, b) => b.placementTime - a.placementTime).slice(0, 8);

  return (
    <div>
      <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Dashboard</h1>
      <p style={{ color: "var(--text-secondary)", margin: "0 0 24px" }}>Live view of {STORE_ID} — updates in real time.</p>

      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <KpiCard icon="🧾" label="Orders today" value={String(ordersToday.length)} accent="#2563eb" />
        <KpiCard icon="💰" label="Revenue today" value={`R${revenueToday.toFixed(2)}`} accent="#d4a017" />
        <KpiCard icon="🚚" label="Active drivers" value={String(activeDrivers)} accent="#17a34a" />
        <KpiCard icon="⏳" label="Pending orders" value={String(pendingOrders.length)} accent="#d97706" />
      </div>

      <div style={{ display: "flex", gap: 20, flexWrap: "wrap" }}>
        <div className="card" style={{ flex: "1 1 320px" }}>
          <h3 style={{ marginTop: 0 }}>Order status breakdown</h3>
          {chartData.length === 0 ? (
            <p style={{ color: "var(--text-secondary)" }}>No orders yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85}>
                  {chartData.map((entry) => (
                    <Cell key={entry.name} fill={CHART_COLORS[entry.name] ?? "#94a3b8"} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card" style={{ flex: "2 1 480px" }}>
          <h3 style={{ marginTop: 0 }}>Recent orders</h3>
          {recentOrders.length === 0 ? (
            <p style={{ color: "var(--text-secondary)" }}>No orders yet.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Placed</th>
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((order) => (
                  <tr key={order.orderId}>
                    <td>#{order.orderId.slice(-6)}</td>
                    <td>R{(order.totalAmount ?? 0).toFixed(2)}</td>
                    <td>
                      <StatusPill status={order.orderStatus} />
                    </td>
                    <td>{new Date(order.placementTime).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
