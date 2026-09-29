import { initializeApp, deleteApp } from "firebase/app";
import { getAuth, createUserWithEmailAndPassword, signOut } from "firebase/auth";
import { firebaseConfig } from "./firebase";

/**
 * Creates a real Firebase Auth login for a new picker/driver, called from PickersPage/
 * DriversPage's "Add" form. The obvious approach — createUserWithEmailAndPassword(auth, ...) on
 * the console's own `auth` instance — would sign the currently-logged-in admin OUT and INTO the
 * new account, since a Firebase Auth SDK instance only ever holds one session. This spins up a
 * second, throwaway Firebase App+Auth instance just for the signup call, signs out of it, then
 * tears it down — the admin's own session in the primary `auth` instance is never touched.
 *
 * No custom `role` claim is set here (that requires the Admin SDK, i.e. a deployed Cloud
 * Function — see docs/API_STATUS.md for why this project doesn't have one). That's fine: the
 * Android app's role-hub screen is a free choice for any signed-in user, and every actual
 * picker/driver write is authorised in firestore.rules by matching `request.auth.uid` against
 * the picker/driver's own document id, not by a claim — see PickersPage.tsx/DriversPage.tsx for
 * where that matching id gets set.
 */
export async function createStaffAuthAccount(email: string, password: string): Promise<string> {
  const secondaryApp = initializeApp(firebaseConfig, `staff-signup-${Date.now()}`);
  try {
    const secondaryAuth = getAuth(secondaryApp);
    const result = await createUserWithEmailAndPassword(secondaryAuth, email, password);
    const uid = result.user.uid;
    await signOut(secondaryAuth);
    return uid;
  } finally {
    await deleteApp(secondaryApp);
  }
}
