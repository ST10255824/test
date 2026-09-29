import { useEffect, useState, type FormEvent } from "react";
import { collection, deleteDoc, doc, onSnapshot, setDoc, updateDoc } from "firebase/firestore";
import { db } from "../lib/firebase";
import { createStaffAuthAccount } from "../lib/staffAccounts";
import type { DriverProfile } from "../lib/types";

/** Store Manager/Admin staff roster for delivery drivers — mirrors the Pickers page. Add creates
 * a real Firebase Auth login (see lib/staffAccounts.ts) plus the drivers/{uid} Firestore profile
 * the Android app's driver flow and the customer's tracking screen both read; Remove deletes
 * that profile so they stop appearing as available/assignable. It doesn't delete the underlying
 * Auth account — the client SDK can only ever delete the *currently signed in* user, never
 * someone else's, so a fully removed login needs the Firebase Console or an Admin SDK script
 * (see functions/serviceAccountKey.json). */
export function DriversPage() {
  const [drivers, setDrivers] = useState<DriverProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [editTarget, setEditTarget] = useState<DriverProfile | "new" | null>(null);

  useEffect(() => {
    return onSnapshot(collection(db, "drivers"), (snapshot) => {
      setDrivers(snapshot.docs.map((d) => ({ driverId: d.id, ...d.data() }) as DriverProfile));
      setLoading(false);
    });
  }, []);

  const removeDriver = async (driver: DriverProfile) => {
    if (!window.confirm(`Remove ${driver.driverName} from the driver roster?`)) return;
    await deleteDoc(doc(db, "drivers", driver.driverId));
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Drivers</h1>
          <p style={{ color: "var(--text-secondary)", margin: "0 0 20px" }}>
            {loading ? "Loading…" : `${drivers.length} registered drivers`}
          </p>
        </div>
        <button className="pill-button primary" onClick={() => setEditTarget("new")}>
          + Add driver
        </button>
      </div>

      <div className="card">
        {loading ? (
          <p style={{ color: "var(--text-secondary)" }}>Loading drivers…</p>
        ) : drivers.length === 0 ? (
          <p style={{ color: "var(--text-secondary)" }}>No drivers registered yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Vehicle</th>
                <th>License plate</th>
                <th>Payout balance</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {drivers.map((driver) => (
                <tr key={driver.driverId}>
                  <td>{driver.driverName}</td>
                  <td>{driver.vehicleType}</td>
                  <td>{driver.licensePlate ?? "—"}</td>
                  <td>R{(driver.currentPayoutBalance ?? 0).toFixed(2)}</td>
                  <td>
                    <span
                      className="status-pill"
                      style={{
                        color: driver.isAvailable ? "var(--success)" : "var(--neutral)",
                        background: driver.isAvailable ? "var(--success-bg)" : "var(--neutral-bg)"
                      }}
                    >
                      {driver.isAvailable ? "Available" : "Offline"}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button className="pill-button outline" style={{ padding: "6px 14px" }} onClick={() => setEditTarget(driver)}>
                        Edit
                      </button>
                      <button className="pill-button outline" style={{ padding: "6px 14px" }} onClick={() => removeDriver(driver)}>
                        Remove
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {editTarget && <DriverDialog driver={editTarget === "new" ? null : editTarget} onClose={() => setEditTarget(null)} />}
    </div>
  );
}

function DriverDialog({ driver, onClose }: { driver: DriverProfile | null; onClose: () => void }) {
  const isNew = driver === null;
  const [driverName, setDriverName] = useState(driver?.driverName ?? "");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [vehicleType, setVehicleType] = useState(driver?.vehicleType ?? "Motorbike");
  const [licensePlate, setLicensePlate] = useState(driver?.licensePlate ?? "");
  const [isAvailable, setIsAvailable] = useState(driver?.isAvailable ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (isNew) {
        const uid = await createStaffAuthAccount(email.trim(), password);
        await setDoc(doc(db, "drivers", uid), {
          driverId: uid,
          driverName: driverName.trim(),
          vehicleType,
          licensePlate: licensePlate.trim(),
          photoUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7c/Profile_avatar_placeholder_large.png/500px-Profile_avatar_placeholder_large.png",
          currentPayoutBalance: 0,
          isAvailable
        });
      } else {
        await updateDoc(doc(db, "drivers", driver.driverId), {
          driverName: driverName.trim(),
          vehicleType,
          licensePlate: licensePlate.trim(),
          isAvailable
        });
      }
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save driver");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      style={{ position: "fixed", inset: 0, background: "rgba(11,28,56,0.45)", display: "flex", alignItems: "center", justifyContent: "center" }}
      onClick={onClose}
    >
      <form onSubmit={handleSubmit} className="card" style={{ width: 360 }} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ marginTop: 0 }}>{isNew ? "Add driver" : `Edit ${driver.driverName}`}</h3>

        {error && (
          <p style={{ color: "var(--error)", background: "var(--error-bg)", padding: "8px 12px", borderRadius: 10, fontSize: 13 }}>
            {error}
          </p>
        )}

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Driver name</label>
        <input
          value={driverName}
          onChange={(e) => setDriverName(e.target.value)}
          required
          style={{ width: "100%", marginTop: 4, marginBottom: 12 }}
        />

        {isNew && (
          <>
            <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Login email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{ width: "100%", marginTop: 4, marginBottom: 12 }}
            />
            <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Temporary password</label>
            <input
              type="text"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              placeholder="At least 6 characters"
              style={{ width: "100%", marginTop: 4, marginBottom: 12 }}
            />
          </>
        )}

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Vehicle type</label>
        <select value={vehicleType} onChange={(e) => setVehicleType(e.target.value)} style={{ width: "100%", marginTop: 4, marginBottom: 12 }}>
          <option value="Motorbike">Motorbike</option>
          <option value="Car">Car</option>
          <option value="Bicycle">Bicycle</option>
          <option value="Van">Van</option>
        </select>

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>License plate</label>
        <input
          value={licensePlate}
          onChange={(e) => setLicensePlate(e.target.value)}
          placeholder="e.g. ND 12 FG KZN"
          style={{ width: "100%", marginTop: 4, marginBottom: 12 }}
        />

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>
          <input type="checkbox" checked={isAvailable} onChange={(e) => setIsAvailable(e.target.checked)} style={{ marginRight: 8 }} />
          Available for runs
        </label>

        <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
          <button type="button" className="pill-button outline" style={{ flex: 1 }} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-button primary" style={{ flex: 1 }} disabled={saving}>
            {saving ? "Saving…" : isNew ? "Create driver" : "Save changes"}
          </button>
        </div>
      </form>
    </div>
  );
}
