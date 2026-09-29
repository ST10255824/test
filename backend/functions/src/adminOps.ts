import * as functions from "firebase-functions";
import { db } from "./firebaseAdmin";

interface AssignCouponRequest {
  customerId: string;
  couponCode: string;
  discountPercent: number;
}

/** Part 1 US-25: "filter Customer accounts and give coupons/discounts to customers." */
export const assignCoupon = functions.https.onCall(async (data: AssignCouponRequest, context) => {
  const role = context.auth?.token.role;
  if (!context.auth || (role !== "ADMIN" && role !== "MANAGER")) {
    throw new functions.https.HttpsError("permission-denied", "Only store managers or admins can assign coupons.");
  }
  if (!data.customerId || !data.couponCode || data.discountPercent <= 0 || data.discountPercent > 100) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid coupon details.");
  }

  await db
    .collection("users")
    .doc(data.customerId)
    .collection("coupons")
    .doc(data.couponCode)
    .set({
      couponCode: data.couponCode,
      discountPercent: data.discountPercent,
      assignedBy: context.auth.uid,
      assignedAt: Date.now(),
      redeemed: false
    });

  return { success: true };
});
