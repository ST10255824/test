import * as functions from "firebase-functions";
import { auth, db } from "./firebaseAdmin";
import { UserRole } from "./types";

/**
 * Every new Firebase Auth user starts as CUSTOMER (Part 1 RBAC: "every other role must be
 * explicitly granted server-side" — see the comment on FirebaseAuthManager.verifyAdminPermissions
 * in the Android app). Staff roles (PICKER/DRIVER/MANAGER/ADMIN) are granted exclusively via
 * assignUserRole below, which only an existing ADMIN can call.
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  await auth.setCustomUserClaims(user.uid, { role: "CUSTOMER" satisfies UserRole });

  const userDoc = await db.collection("users").doc(user.uid).get();
  if (!userDoc.exists) {
    await db.collection("users").doc(user.uid).set({
      customerId: user.uid,
      email: user.email ?? "",
      name: user.displayName ?? "",
      mobileNumber: user.phoneNumber ?? "",
      role: "CUSTOMER",
      loyaltyPoints: 0,
      savedAddresses: [],
      biometricEnabled: false
    });
  }
});

interface AssignRoleRequest {
  targetUid: string;
  role: UserRole;
}

/** Part 1 US-25: admin establishes roles and enforces permission hierarchies. */
export const assignUserRole = functions.https.onCall(async (data: AssignRoleRequest, context) => {
  if (!context.auth || context.auth.token.role !== "ADMIN") {
    throw new functions.https.HttpsError("permission-denied", "Only an admin can assign roles.");
  }
  const validRoles: UserRole[] = ["CUSTOMER", "PICKER", "DRIVER", "MANAGER", "ADMIN"];
  if (!validRoles.includes(data.role)) {
    throw new functions.https.HttpsError("invalid-argument", "Unknown role.");
  }

  await auth.setCustomUserClaims(data.targetUid, { role: data.role });
  await db.collection("users").doc(data.targetUid).set({ role: data.role }, { merge: true });

  return { success: true };
});
