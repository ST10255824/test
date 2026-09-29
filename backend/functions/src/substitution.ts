import * as functions from "firebase-functions";
import { db, rtdb } from "./firebaseAdmin";
import { Order, Product } from "./types";

interface ProposeSubstitutionRequest {
  orderId: string;
  productId: string;
  replacementProductId: string;
}

interface RespondToSubstitutionRequest {
  orderId: string;
  messageId: string;
  approved: boolean;
}

/** Part 1 US-17: picker flags an out-of-stock item and proposes a replacement. */
export const proposeSubstitution = functions.https.onCall(async (data: ProposeSubstitutionRequest, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Sign in required.");

  const orderRef = db.collection("orders").doc(data.orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) throw new functions.https.HttpsError("not-found", "Order not found.");
  const order = orderSnap.data() as Order;

  if (order.shopperId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "You are not the picker assigned to this order.");
  }

  const replacementSnap = await db
    .collection("storeNodes")
    .doc(order.storeId)
    .collection("products")
    .doc(data.replacementProductId)
    .get();
  if (!replacementSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Replacement product not found.");
  }
  const replacement = replacementSnap.data() as Product;

  const updatedItems = order.items.map((item) =>
    item.productId === data.productId ? { ...item, isSubstituted: true, substitutionApproved: null } : item
  );
  await orderRef.update({ items: updatedItems });

  const messageRef = rtdb.ref(`substitutionChats/${data.orderId}/messages`).push();
  await messageRef.set({
    messageId: messageRef.key,
    senderId: context.auth.uid,
    senderRole: "PICKER",
    timestamp: Date.now(),
    payloadText: `${replacement.name} is out of stock. Would you like ${replacement.name} instead?`,
    proposedProductId: data.replacementProductId,
    isApproved: null
  });

  return { messageId: messageRef.key };
});

/** Part 1 US-14: customer approves or declines the picker's proposed substitute. */
export const respondToSubstitution = functions.https.onCall(async (data: RespondToSubstitutionRequest, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Sign in required.");

  const orderRef = db.collection("orders").doc(data.orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) throw new functions.https.HttpsError("not-found", "Order not found.");
  const order = orderSnap.data() as Order;

  if (order.customerId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "This is not your order.");
  }

  const messageRef = rtdb.ref(`substitutionChats/${data.orderId}/messages/${data.messageId}`);
  const messageSnap = await messageRef.get();
  if (!messageSnap.exists()) throw new functions.https.HttpsError("not-found", "Substitution message not found.");
  const message = messageSnap.val();

  await messageRef.update({ isApproved: data.approved });

  const updatedItems = await Promise.all(
    order.items.map(async (item) => {
      if (item.productId !== message.proposedProductId && !(item.isSubstituted && item.substitutionApproved === null)) {
        return item;
      }
      if (!data.approved) {
        return { ...item, isSubstituted: false, substitutionApproved: null };
      }
      const replacementSnap = await db
        .collection("storeNodes")
        .doc(order.storeId)
        .collection("products")
        .doc(message.proposedProductId)
        .get();
      const replacement = replacementSnap.data() as Product | undefined;
      return {
        ...item,
        name: replacement?.name ?? item.name,
        unitPrice: replacement?.unitPrice ?? item.unitPrice,
        isSubstituted: true,
        substitutionApproved: true
      };
    })
  );

  await orderRef.update({ items: updatedItems });
  return { success: true };
});
