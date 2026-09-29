import { useEffect, useState } from "react";
import { collection, doc, onSnapshot, query, updateDoc, where } from "firebase/firestore";
import { db, STORE_ID } from "../lib/firebase";
import type { Order, OrderStatus } from "../lib/types";
import { StatusPill } from "../components/StatusPill";

const ALL: OrderStatus[] = ["RECEIVED", "PICKING", "READY_FOR_COLLECTION", "EN_ROUTE", "ARRIVED_AT_NODE", "DELIVERED", "CANCELLED"];

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<OrderStatus | "ALL">("ALL");

  useEffect(() => {
    const ordersQuery = query(collection(db, "orders"), where("storeId", "==", STORE_ID));
    return onSnapshot(ordersQuery, (snapshot) => {
      setOrders(snapshot.docs.map((d) => ({ orderId: d.id, ...d.data() }) as Order));
      setLoading(false);
    });
  }, []);

  const visible = orders
    .filter((o) => filter === "ALL" || o.orderStatus === filter)
    .sort((a, b) => b.placementTime - a.placementTime);

  // Stands in for the acceptDeliveryRun Cloud Function's staff-facing half: a driver shows the
  // order's short code at the counter (DriverActiveRunScreen on Android) and staff match it
  // against the physical order before releasing it — this is what unblocks their app's
  // "Mark as collected" button.
  async function confirmDispatch(orderId: string) {
    await updateDoc(doc(db, "orders", orderId), { dispatchConfirmedByAdmin: true });
  }

  return (
    <div>
      <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Orders</h1>
      <p style={{ color: "var(--text-secondary)", margin: "0 0 20px" }}>{loading ? "Loading…" : `${visible.length} orders`}</p>

      <div style={{ marginBottom: 16 }}>
        <select value={filter} onChange={(e) => setFilter(e.target.value as OrderStatus | "ALL")}>
          <option value="ALL">All statuses</option>
          {ALL.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>

      <div className="card">
        {loading ? (
          <p style={{ color: "var(--text-secondary)" }}>Loading orders…</p>
        ) : visible.length === 0 ? (
          <p style={{ color: "var(--text-secondary)" }}>No orders match this filter.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>Items</th>
                <th>Delivery slot</th>
                <th>Total</th>
                <th>Status</th>
                <th>Placed</th>
                <th>Dispatch</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((order) => (
                <tr key={order.orderId}>
                  <td>#{order.orderId.slice(-6)}</td>
                  <td>{order.items?.reduce((sum, i) => sum + i.quantity, 0) ?? 0} items</td>
                  <td>{order.deliverySlotLabel ?? "—"}</td>
                  <td>R{(order.totalAmount ?? 0).toFixed(2)}</td>
                  <td>
                    <StatusPill status={order.orderStatus} />
                  </td>
                  <td>{new Date(order.placementTime).toLocaleString()}</td>
                  <td>
                    {order.orderStatus === "READY_FOR_COLLECTION" && order.driverId ? (
                      order.dispatchConfirmedByAdmin ? (
                        <span style={{ color: "var(--success)" }}>Confirmed ✓</span>
                      ) : (
                        <button onClick={() => confirmDispatch(order.orderId)}>
                          Confirm dispatch #{order.orderId.slice(-6).toUpperCase()}
                        </button>
                      )
                    ) : (
                      "—"
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
