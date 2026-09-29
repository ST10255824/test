/**
 * Populates the local Firebase emulators with enough data to demo every role end-to-end:
 * one store, a small catalogue spanning several categories/aisles, one on-duty picker, and
 * one available driver. Run with `npm run seed` while the emulators are running (see README
 * "Run everything locally").
 *
 * This intentionally talks to the *emulator* endpoints only — it refuses to run unless the
 * FIRESTORE_EMULATOR_HOST / FIREBASE_DATABASE_EMULATOR_HOST env vars are set, so it can never
 * accidentally write seed/test data into a real production project. For the real project, see
 * seedProduction.ts instead.
 */
import * as admin from "firebase-admin";
import { seedCatalogueAndStaff } from "./catalogue";

if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_DATABASE_EMULATOR_HOST) {
  console.error(
    "Refusing to seed: FIRESTORE_EMULATOR_HOST / FIREBASE_DATABASE_EMULATOR_HOST are not set.\n" +
      "Run this via `npm run seed`, which sets them for you, with the emulators already running."
  );
  process.exit(1);
}

admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT ?? "demo-mackson-delivery" });

seedCatalogueAndStaff(admin.firestore(), admin.auth())
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Seeding failed:", error);
    process.exit(1);
  });
