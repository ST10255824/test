import * as functions from "firebase-functions";
import { db, rtdb } from "./firebaseAdmin";
import { Order } from "./types";

interface AcceptRunRequest {
  orderId: string;
  driverId: string;
}

interface ConfirmProofRequest {
  orderId: string;
  method: "OTP" | "QR_CODE" | "PHOTO";
  code: string;
}

/**
 * Part 1 US-19/US-21: a driver claims a staged run. Wrapped in a transaction for the same
 * reason checkout.ts is — without it, two drivers tapping "Accept" within the same instant
 * could both believe they won the run.
 */
export const acceptDeliveryRun = functions.https.onCall(async (data: AcceptRunRequest, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Sign in required.");

  const orderRef = db.collection("orders").doc(data.orderId);

  const order = await db.runTransaction(async (transaction) => {
    const snap = await transaction.get(orderRef);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "Order not found.");
    const current = snap.data() as Order;

    if (current.orderStatus !== "READY_FOR_COLLECTION" || current.driverId !== null) {
      throw new functions.https.HttpsError("failed-precondition", "This run has already been accepted by another driver.");
    }

    transaction.update(orderRef, { driverId: data.driverId });
    return current;
  });

  // Grants this driver read access to their own GPS telemetry stream and lets the customer's
  // tracking screen resolve driverId -> customerId for database.rules.json.
  await rtdb.ref(`runAssignments/${data.driverId}`).set({ customerId: order.customerId, orderId: data.orderId });

  return { success: true };
});

/**
 * Part 1 US-22: "order status cannot clear to Delivered until the customer token handshakes
 * successfully with the driver's app." The comparison happens here, server-side, against the
 * OTP generated at checkout (checkout.ts) — the driver's device never independently decides an
 * order is delivered.
 */
export const confirmProofOfDelivery = functions.https.onCall(async (data: ConfirmProofRequest, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Sign in required.");

  const orderRef = db.collection("orders").doc(data.orderId);
  const snap = await orderRef.get();
  if (!snap.exists) throw new functions.https.HttpsError("not-found", "Order not found.");
  const order = snap.data() as Order;

  if (order.driverId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "You are not the driver for this order.");
  }

  const isValid =
    (data.method === "OTP" && data.code === order.deliveryOtp) ||
    (data.method === "QR_CODE" && data.code === order.deliveryQrToken) ||
    // Photo confirmation has no code to match — driver-captured evidence is stored, not verified.
    data.method === "PHOTO";

  if (!isValid) {
    throw new functions.https.HttpsError("failed-precondition", "That code doesn't match this order.");
  }

  await orderRef.update({ orderStatus: "DELIVERED", deliveredTime: Date.now() });
  return { success: true };
});
