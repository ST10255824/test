import { useEffect, useState, type FormEvent } from "react";
import { collection, deleteDoc, doc, onSnapshot, setDoc, updateDoc } from "firebase/firestore";
import { db, STORE_ID } from "../lib/firebase";
import { createStaffAuthAccount } from "../lib/staffAccounts";
import type { PickerProfile } from "../lib/types";

/** Store Manager/Admin staff roster for in-store pickers — mirrors the Drivers page. Add creates
 * a real Firebase Auth login (see lib/staffAccounts.ts) plus the pickers/{uid} Firestore profile
 * the Android app's picker flow reads; Remove deletes that profile so they stop appearing as
 * on-duty/assignable. It doesn't delete the underlying Auth account — the client SDK can only
 * ever delete the *currently signed in* user, never someone else's, so a fully removed login
 * needs the Firebase Console or an Admin SDK script (see functions/serviceAccountKey.json). */
export function PickersPage() {
  const [pickers, setPickers] = useState<PickerProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [editTarget, setEditTarget] = useState<PickerProfile | "new" | null>(null);

  useEffect(() => {
    return onSnapshot(collection(db, "pickers"), (snapshot) => {
      setPickers(snapshot.docs.map((d) => ({ shopperId: d.id, ...d.data() }) as PickerProfile));
      setLoading(false);
    });
  }, []);

  const removePicker = async (picker: PickerProfile) => {
    if (!window.confirm(`Remove ${picker.staffName} from the picker roster?`)) return;
    await deleteDoc(doc(db, "pickers", picker.shopperId));
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 800, margin: "0 0 4px" }}>Pickers</h1>
          <p style={{ color: "var(--text-secondary)", margin: "0 0 20px" }}>
            {loading ? "Loading…" : `${pickers.length} in-store pickers`}
          </p>
        </div>
        <button className="pill-button primary" onClick={() => setEditTarget("new")}>
          + Add picker
        </button>
      </div>

      <div className="card">
        {loading ? (
          <p style={{ color: "var(--text-secondary)" }}>Loading pickers…</p>
        ) : pickers.length === 0 ? (
          <p style={{ color: "var(--text-secondary)" }}>No pickers on the roster yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Store</th>
                <th>Duty status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {pickers.map((picker) => (
                <tr key={picker.shopperId}>
                  <td>{picker.staffName}</td>
                  <td>{picker.assignedStoreId}</td>
                  <td>
                    <span
                      className="status-pill"
                      style={{
                        color: picker.dutyStatus === "ON_DUTY" ? "var(--success)" : "var(--neutral)",
                        background: picker.dutyStatus === "ON_DUTY" ? "var(--success-bg)" : "var(--neutral-bg)"
                      }}
                    >
                      {picker.dutyStatus === "ON_DUTY" ? "On duty" : "Off duty"}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button className="pill-button outline" style={{ padding: "6px 14px" }} onClick={() => setEditTarget(picker)}>
                        Edit
                      </button>
                      <button className="pill-button outline" style={{ padding: "6px 14px" }} onClick={() => removePicker(picker)}>
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

      {editTarget && <PickerDialog picker={editTarget === "new" ? null : editTarget} onClose={() => setEditTarget(null)} />}
    </div>
  );
}

function PickerDialog({ picker, onClose }: { picker: PickerProfile | null; onClose: () => void }) {
  const isNew = picker === null;
  const [staffName, setStaffName] = useState(picker?.staffName ?? "");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [dutyStatus, setDutyStatus] = useState<PickerProfile["dutyStatus"]>(picker?.dutyStatus ?? "ON_DUTY");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (isNew) {
        const uid = await createStaffAuthAccount(email.trim(), password);
        await setDoc(doc(db, "pickers", uid), {
          shopperId: uid,
          staffName: staffName.trim(),
          assignedStoreId: STORE_ID,
          dutyStatus
        });
      } else {
        await updateDoc(doc(db, "pickers", picker.shopperId), {
          staffName: staffName.trim(),
          dutyStatus
        });
      }
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save picker");
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
        <h3 style={{ marginTop: 0 }}>{isNew ? "Add picker" : `Edit ${picker.staffName}`}</h3>

        {error && (
          <p style={{ color: "var(--error)", background: "var(--error-bg)", padding: "8px 12px", borderRadius: 10, fontSize: 13 }}>
            {error}
          </p>
        )}

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Staff name</label>
        <input
          value={staffName}
          onChange={(e) => setStaffName(e.target.value)}
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

        <label style={{ fontSize: 13, color: "var(--text-secondary)" }}>Duty status</label>
        <select
          value={dutyStatus}
          onChange={(e) => setDutyStatus(e.target.value as PickerProfile["dutyStatus"])}
          style={{ width: "100%", marginTop: 4, marginBottom: 20 }}
        >
          <option value="ON_DUTY">On duty</option>
          <option value="OFF_DUTY">Off duty</option>
        </select>

        <div style={{ display: "flex", gap: 10 }}>
          <button type="button" className="pill-button outline" style={{ flex: 1 }} onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="pill-button primary" style={{ flex: 1 }} disabled={saving}>
            {saving ? "Saving…" : isNew ? "Create picker" : "Save changes"}
          </button>
        </div>
      </form>
    </div>
  );
}
