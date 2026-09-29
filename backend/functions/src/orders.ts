import * as functions from "firebase-functions";
import { db, messaging, rtdb } from "./firebaseAdmin";
import { Order } from "./types";

/**
 * "Node.js serverless triggers carry out background business logic (e.g., auto-routing
 * picking manifests upon order placement)" — Part 1, section 9.3.9. Fires on every new order
 * and hands it straight to the first on-duty picker at that store node.
 */
export const onOrderCreate = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snapshot) => {
    const order = snapshot.data() as Order;

    const pickerQuery = await db
      .collection("pickers")
      .where("assignedStoreId", "==", order.storeId)
      .where("dutyStatus", "==", "ON_DUTY")
      .limit(1)
      .get();

    if (pickerQuery.empty) {
      functions.logger.warn(`No on-duty picker available for order ${order.orderId} at store ${order.storeId}`);
      return;
    }

    const picker = pickerQuery.docs[0];
    await snapshot.ref.update({
      shopperId: picker.id,
      orderStatus: "PICKING"
    });

    // Grants the picker access to the substitution chat under database.rules.json.
    await rtdb.ref(`chatParticipants/${order.orderId}/shopperId`).set(picker.id);
  });

/**
 * Part 1, section 9.3.9: "Asynchronous push messages about status changes and driver arrivals
 * are sent to client apps via Firebase Cloud Messaging (FCM)." Fires on every order write and
 * only notifies when orderStatus actually changed, so item-level edits (e.g. a pick progress
 * write) don't spam the customer.
 */
export const onOrderStatusChange = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change) => {
    const before = change.before.data() as Order;
    const after = change.after.data() as Order;

    if (before.orderStatus === after.orderStatus) {
      return;
    }

    const customerSnap = await db.collection("users").doc(after.customerId).get();
    const fcmToken = customerSnap.data()?.fcmToken as string | undefined;
    if (!fcmToken) {
      return;
    }

    const { title, body } = notificationCopyFor(after);

    await messaging.send({
      token: fcmToken,
      notification: { title, body },
      data: { orderId: after.orderId, orderStatus: after.orderStatus }
    });
  });

function notificationCopyFor(order: Order): { title: string; body: string } {
  switch (order.orderStatus) {
    case "PICKING":
      return { title: "Your order is being picked", body: `Order #${order.orderId.slice(-6)} is on its way to being packed.` };
    case "READY_FOR_COLLECTION":
      return { title: "Order packed", body: "Your groceries are staged and waiting for a driver." };
    case "EN_ROUTE":
      return { title: "Driver en route", body: "Your driver is on the way — track them live in the app." };
    case "ARRIVED_AT_NODE":
      return { title: "Driver has arrived", body: "Your driver is at your door." };
    case "DELIVERED":
      return { title: "Delivered", body: "Enjoy! Rate your delivery in the app." };
    default:
      return { title: "Order update", body: `Order #${order.orderId.slice(-6)} status: ${order.orderStatus}` };
  }
}
