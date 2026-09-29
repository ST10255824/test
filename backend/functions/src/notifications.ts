import * as functions from "firebase-functions";
import { db } from "./firebaseAdmin";
import { Order } from "./types";

/**
 * WIL group requirement (see project WhatsApp thread): "when a customer creates an order they
 * get an email that gets sent to them revising their order and payment." Fires on every new
 * order and emails a receipt via SendGrid's REST API (no extra npm dependency needed — Node 20
 * ships a global `fetch`).
 *
 * Like every other Cloud Function in this codebase, this is written and ready but NOT deployed:
 * Cloud Functions need the Blaze plan, and this project stays on the free Spark plan (declined
 * twice — see README "Scope decisions"). It also needs a real SENDGRID_API_KEY and a verified
 * sender identity, neither of which exist for this project. Once Blaze + SendGrid are set up,
 * deploy with `firebase deploy --only functions:sendOrderConfirmationEmail` and set the key via
 * `firebase functions:config:set sendgrid.key="..."` (1st-gen) or a `.env` file (2nd-gen).
 */
export const sendOrderConfirmationEmail = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snapshot) => {
    const order = snapshot.data() as Order;

    const apiKey = functions.config().sendgrid?.key as string | undefined;
    if (!apiKey) {
      functions.logger.warn(
        `sendOrderConfirmationEmail: no SendGrid API key configured — skipping email for order ${order.orderId}.`
      );
      return;
    }

    const customerSnap = await db.collection("users").doc(order.customerId).get();
    const customerEmail = customerSnap.data()?.email as string | undefined;
    if (!customerEmail) {
      functions.logger.warn(`sendOrderConfirmationEmail: order ${order.orderId} has no customer email on file.`);
      return;
    }

    const itemLines = order.items
      .map((item) => `${item.quantity} x ${item.name} — R${(item.unitPrice * item.quantity).toFixed(2)}`)
      .join("\n");

    const body = {
      personalizations: [{ to: [{ email: customerEmail }] }],
      from: { email: "orders@macksons.co.za", name: "Mackson's Delivery" },
      subject: `Order #${order.orderId.slice(-6).toUpperCase()} confirmed`,
      content: [
        {
          type: "text/plain",
          value:
            `Thanks for your order!\n\n${itemLines}\n\n` +
            `Subtotal: R${order.subtotal.toFixed(2)}\n` +
            `Service fee: R${order.serviceFee.toFixed(2)}\n` +
            `Driver tip: R${order.driverTip.toFixed(2)}\n` +
            `Total paid: R${order.totalAmount.toFixed(2)}\n\n` +
            `Delivery: ${order.deliverySlotLabel}\n` +
            `Track your order any time in the Mackson's app.`
        }
      ]
    };

    const response = await fetch("https://api.sendgrid.com/v3/mail/send", {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    if (!response.ok) {
      functions.logger.error(`sendOrderConfirmationEmail: SendGrid returned ${response.status} for order ${order.orderId}.`);
    }
  });
