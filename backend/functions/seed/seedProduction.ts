/**
 * Populates the REAL `mackson-delivery` Firebase project with the same store/catalogue/staff
 * data as seedEmulator.ts. This writes to production, so it's deliberately harder to run by
 * accident than the emulator seeder:
 *   - refuses to run if FIRESTORE_EMULATOR_HOST is set (that means you meant to run the
 *     emulator seeder instead)
 *   - requires CONFIRM_PRODUCTION_SEED=yes to be set explicitly
 *   - requires a service account key at functions/serviceAccountKey.json (gitignored — never
 *     commit this file) so it can't silently fall back to some other ambient credential
 *
 * Run with `npm run seed:production` (see functions/package.json).
 */
import * as admin from "firebase-admin";
import * as path from "path";
import * as fs from "fs";
import { seedCatalogueAndStaff } from "./catalogue";

if (process.env.FIRESTORE_EMULATOR_HOST) {
  console.error("Refusing to seed production: FIRESTORE_EMULATOR_HOST is set — use `npm run seed` instead.");
  process.exit(1);
}

if (process.env.CONFIRM_PRODUCTION_SEED !== "yes") {
  console.error(
    "Refusing to seed production without confirmation.\n" +
      "Run `npm run seed:production` instead of calling this script directly."
  );
  process.exit(1);
}

const keyPath = path.join(__dirname, "..", "serviceAccountKey.json");
if (!fs.existsSync(keyPath)) {
  console.error(
    `Refusing to seed production: no service account key found at ${keyPath}.\n` +
      "Download one from Firebase Console > Project Settings > Service accounts > Generate new private key."
  );
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(keyPath)
});

seedCatalogueAndStaff(admin.firestore(), admin.auth())
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Seeding failed:", error);
    process.exit(1);
  });
