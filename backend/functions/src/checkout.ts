import * as functions from "firebase-functions";
import { db, rtdb } from "./firebaseAdmin";
import { computeCheckoutTotals, PricedLine } from "./pricing";
import { assertSufficientStock, StockCheckLine, InsufficientStockError } from "./stock";
import { CheckoutRequest, Order, OrderItem, Product } from "./types";

/**
 * The atomic checkout transaction referenced throughout Part 1 (sections 2.3, 9.3.9, 9.3.11):
 * "Firestore's transaction API (runTransaction) is used instead of a SQL WHERE stock > 0
 * update ... to prevent two buyers from purchasing the final unit of an item." Every price and
 * stock figure is re-read from Firestore inside the transaction — nothing the client sent is
 * trusted for money or inventory.
 */
export const checkoutOrder = functions.https.onCall(async (data: CheckoutRequest, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in to place an order.");
  }
  const customerId = context.auth.uid;

  if (!data.storeId || !Array.isArray(data.items) || data.items.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "Cart is empty or store is missing.");
  }

  const orderRef = db.collection("orders").doc();

  const order = await db.runTransaction(async (transaction) => {
    const productRefs = data.items.map((item) =>
      db.collection("storeNodes").doc(data.storeId).collection("products").doc(item.productId)
    );
    const productSnaps = await Promise.all(productRefs.map((ref) => transaction.get(ref)));

    const products: Product[] = productSnaps.map((snap, index) => {
      if (!snap.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          `Product ${data.items[index].productId} is no longer available.`
        );
      }
      return snap.data() as Product;
    });

    const stockLines: StockCheckLine[] = products.map((product, index) => ({
      productId: product.productId,
      requestedQuantity: data.items[index].quantity,
      currentStockLevel: product.currentStockLevel,
      productName: product.name
    }));

    try {
      assertSufficientStock(stockLines);
    } catch (error) {
      if (error instanceof InsufficientStockError) {
        throw new functions.https.HttpsError("failed-precondition", error.message);
      }
      throw new functions.https.HttpsError("invalid-argument", (error as Error).message);
    }

    // Decrement stock for every line inside the same transaction that validated it —
    // this is what closes the race window between two concurrent checkouts.
    productRefs.forEach((ref, index) => {
      transaction.update(ref, {
        currentStockLevel: products[index].currentStockLevel - data.items[index].quantity
      });
    });

    const pricedLines: PricedLine[] = products.map((product, index) => ({
      productId: product.productId,
      quantity: data.items[index].quantity,
      unitPrice: product.unitPrice
    }));
    const breakdown = computeCheckoutTotals(
      pricedLines,
      data.applyLoyaltyDiscount,
      data.driverTip ?? 0,
      data.voucherAmount ?? 0
    );

    const orderItems: OrderItem[] = products.map((product, index) => ({
      productId: product.productId,
      name: product.name,
      quantity: data.items[index].quantity,
      unitPrice: product.unitPrice,
      aisleNumber: product.aisleNumber,
      barcode: product.barcode,
      imageUrl: product.imageUrl,
      isSubstituted: false,
      substitutionApproved: null
    }));

    const deliveryOtp = Math.floor(100000 + Math.random() * 900000).toString();
    const deliveryQrToken = `${orderRef.id}-${Math.random().toString(36).slice(2, 10)}`;

    const newOrder: Order = {
      orderId: orderRef.id,
      customerId,
      storeId: data.storeId,
      shopperId: null,
      driverId: null,
      orderStatus: "RECEIVED",
      items: orderItems,
      subtotal: breakdown.subtotal,
      loyaltyDiscount: breakdown.loyaltyDiscount,
      driverTip: breakdown.driverTip,
      serviceFee: breakdown.serviceFee,
      totalAmount: breakdown.total,
      deliverySlotType: data.deliverySlotType,
      deliverySlotLabel: data.deliverySlotLabel,
      deliveryAddress: null,
      paymentReferenceToken: `TOKENISED-${orderRef.id}`,
      placementTime: Date.now(),
      scheduledTime: null,
      deliveredTime: null,
      deliveryOtp,
      deliveryQrToken,
      dispatchConfirmedByAdmin: false
    };

    transaction.set(orderRef, newOrder);
    return newOrder;
  });

  // Lets the Realtime Database security rules (database.rules.json) authorise the
  // substitution chat without the RTDB ever having to trust a client-supplied customerId.
  await rtdb.ref(`chatParticipants/${order.orderId}`).set({ customerId: order.customerId });

  return { orderId: order.orderId };
});
