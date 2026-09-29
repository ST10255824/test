import { useEffect, useState, type FormEvent } from "react";
import { collection, doc, onSnapshot, query, setDoc, where } from "firebase/firestore";
import { db } from "../lib/firebase";
import type { Customer } from "../lib/types";

/** Part 1 US-25: "filter Customer accounts and give coupons/discounts to customers." Writes
 * the coupon straight to Firestore (users/{uid}/coupons/{code}), permitted for ADMIN/MANAGER by
 * firestore/firestore.rules — the assignCoupon Cloud Function does the same write when Cloud
 * Functions are deployed (see README "Cloud Functions require Blaze"); both paths are trusted
 * equally by the rules since both require the ADMIN/MANAGER custom claim. */
export function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [couponTarget, setCouponTarget] = useState<Customer | null>(null);

  useEffect(() => {
    const customersQuery = query(collection(db, "users"), where("role", "==", "CUSTOMER"));
    return onSnapshot(customersQuery, (snapshot) => {
      setCustomers(snapshot.docs.map((d) => ({ customerId: d.id, ...d.data() }) as Customer));
      setLoading(false);
    });
  }, []);

  const visible = customers.filter(
    (c) => c.name?.toLowerCase().includes(search.toLowerCase()) || c.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Customers</h1>
      <p style={{ color: "var(--text-secondary)", margin: "0 0 20px" }}>
        {loading ? "Loading…" : `${customers.length} registered customers`}
      </p>

      <input
        placeholder="Search by name or email…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ width: 320, marginBottom: 16 }}
      />

      <div className="card">
        {loading ? (
          <p style={{ color: "var(--text-secondary)" }}>Loading customers…</p>
        ) : visible.length === 0 ? (
          <p style={{ color: "var(--text-secondary)" }}>No customers match.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Loyalty points</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((customer) => (
                <tr key={customer.customerId}>
                  <td>{customer.name}</td>
                  <td>{customer.email}</td>
                  <td>{customer.mobileNumber}</td>
                  <td>{customer.loyaltyPoints ?? 0}</td>
                  <td>
                    <button className="pill-button outline" style={{ padding: "6px 14px" }} onClick={() => setCouponTarget(customer)}>
                      Add coupon
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {couponTarget && <CouponDialog customer={couponTarget} onClose={() => setCouponTarget(null)} />}
    </div>
  );
}

function CouponDialog({ customer, onClose }: { customer: Customer; onClose: () => void }) {
  const [code, setCode] = useState("");
  const [percent, setPercent] = useState("10");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      await setDoc(doc(db, "users", customer.customerId, "coupons", code.trim().toUpperCase()), {
        couponCode: code.trim().toUpperCase(),
        discountPercent: Number(percent),
        assignedAt: Date.now(),
        redeemed: false
      });
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(11,28,56,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center"
      }}
      onClick={onClose}
    >
      <form onSubmit={handleSubmit} className="card" style={{ width: 340 }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginTop: 0 }}>Coupon for {customer.name}</h3>
        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Coupon code</label>
        <input value={code} onChange={(e) => setCode(e.target.value)} required style={{ width: "100%", marginTop: 4, marginBottom: 12 }} />
        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Discount %</label>
        <input
          type="number"
          min={1}
          max={100}
          value={percent}
          onChange={(e) => setPercent(e.target.value)}
          required
          style={{ width: "100%", marginTop: 4, marginBottom: 20 }}
        />
        <div style={{ display: "flex", gap: 10 }}>
          <button type="button" className="pill-button outline" style={{ flex: 1 }} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-button primary" style={{ flex: 1 }} disabled={saving}>
            {saving ? "Saving…" : "Assign"}
          </button>
        </div>
      </form>
    </div>
  );
}
