# Mackson's Delivery — Task 2: Code & Implementation

INSY7315 Work Integrated Learning — Part 2 (Code and Implementation, 70%). This repository
implements the platform documented in Part 1 (Documentation & Prototype): a Firebase-backed,
on-demand grocery delivery system for **Mackson's Supermarket**, serving four user roles —
Customer, In-Store Picker, and Delivery Driver on a shared Android app, and Store Manager/Admin
on a **separate web console**.

## Two client apps, one backend

Store Manager/Admin tooling is deliberately **not part of the mobile app** — it's its own web
app (`admin-web/`), per the client's explicit requirement that admin tooling live on the web.
Both clients read and write the exact same Firebase project (`mackson-delivery`), enforced by
the same Firestore/Realtime Database Security Rules, so there's one source of truth regardless
of which client touches the data:

```
frontend/
  app/         Android app — Customer, In-Store Picker, Delivery Driver (Kotlin, Jetpack Compose)
  admin-web/   Admin console — Store Manager/Admin only (React, TypeScript, Firebase Hosting)
backend/
  functions/   Cloud Functions backend shared by both clients
  firestore/   Firestore & Realtime Database Security Rules and indexes
```

Live admin console: **https://mackson-delivery.web.app** (Store Manager/Admin accounts only —
see `frontend/admin-web/README.md` for setup and local dev).

Android app (Customer/Picker/Driver): **[download the APK from Releases](https://github.com/EMKNDN/INSY7315-The-V-Unit/releases/latest)**
— install directly on a device or emulator (API 26+) without cloning or building from source.

## Scope decision (read this first)

Part 1 documents three separate native Android client apps (Customer, Picker, Driver) plus an
Admin console, on top of a shared Firebase backend. This build keeps the documented **backend
architecture exactly as designed** (Firestore + Realtime Database + Cloud Functions + FCM,
MVVM + Repository + Observer + Singleton + Factory patterns, RBAC via custom claims) but
consolidates the three *mobile* client experiences into **one Kotlin/Jetpack Compose app** with
role-based navigation, rather than shipping three separate Gradle projects. A single grader
account can therefore explore all three mobile roles (`role-hub` screen after sign-in) without
needing three devices or three separate logins. Firestore Security Rules and Realtime Database
Rules still enforce the real RBAC boundary server-side regardless of which client is open — the
app consolidation is a client-side convenience, not a security shortcut.

**Cloud Functions and Cloud Storage are not currently deployed** — both require upgrading the
Firebase project to the Blaze (pay-as-you-go) plan, which needs a billing card on file. The
project currently runs on the free Spark plan. This means:
- Working end-to-end: registration/login, browsing the real seeded catalogue, the admin console
  (dashboard, orders, inventory, customers, drivers), and any Firestore-direct write (e.g.
  inventory stock edits, coupon assignment).
- **Not working until Blaze is enabled:** checkout, accepting a delivery run, proof of delivery,
  and the substitution chat's Function-backed matching — everything that specifically requires a
  callable Cloud Function. The code for all of it is written and unit-tested
  (`functions/src/*.ts`, `functions/test/*.test.ts`); it just isn't live. Deploying it once
  Blaze is enabled is a single command — see "Deploy the backend" below.

Other scope decisions, each called out with a comment at the point they matter in the code:

- **Phone/SMS OTP** (`FirebaseAuthManager.startPhoneVerification`) is fully implemented but
  needs a Blaze-plan Firebase project with SHA-1/SHA-256 fingerprints registered before real
  SMS will send. Email/password is the primary sign-in path so the app is fully usable and
  CI-buildable without that setup.
- **Driver proximity dashboard** (`FirestoreRepository.fetchAllAvailableRuns`) lists every
  staged run rather than geo-filtering by distance — real proximity ranking needs a geohash
  index (e.g. GeoFire) keyed off live driver coordinates, which only exist once a driver goes
  on duty. Documented as a follow-up.
- **Proof of delivery** supports OTP entry and QR-code scan (both fully wired to the
  `confirmProofOfDelivery` Cloud Function); photo capture is defined in the data schema
  (`ProofOfDeliveryMethod.PHOTO`, `confirmProofOfDelivery` handles it server-side) but has no
  camera-capture UI in this build.
- **Visual design** matches the Part 1 Figma prototype's navy/gold colour system, Poppins-style
  typography, and pill-shaped buttons (`ui/theme/`), including a brand mark
  (`ui/common/BrandLogo.kt`) inspired by — but not a pixel copy of — the prototype's crest logo,
  since that art was Figma placeholder work rather than a real trademark asset.
- **Seed catalogue is real client data.** The 33 products in `functions/seed/seedEmulator.ts`
  (names, brands, prices, barcodes) are taken directly from Mackson's own price list, not
  invented — including two items from their in-house MACKSONS bakery brand. Product photos are
  real, freely-licensed photography from Wikimedia Commons rather than icon placeholders, loaded
  at runtime via Coil (`ui/common/ProductThumbnail.kt`); a few show a real third-party brand
  (the cleanest available photo for that item) which is fine for a non-commercial student
  prototype but should be swapped for the client's own product photography (via Cloud Storage,
  per Part 1's architecture) before any real-world use. The full 67,700-SKU price list covers
  the whole store (hardware, electronics, etc.), not just groceries — this build seeds a
  representative slice across the seven catalogue categories.
- **Search** uses a maintained `nameSearchKey` field and a Firestore range query
  (`functions/src/catalogue.ts`) rather than a dedicated search service like Algolia — adequate
  for a single store's catalogue size, called out as a production follow-up.

## Architecture at a glance

```
Android app (Customer/Picker/Driver)       admin-web/ (Store Manager/Admin, React)
   Kotlin, Jetpack Compose, MVVM              TypeScript, Firebase Hosting
        │                                          │
        ├── FirebaseAuthManager                    ├── AuthContext (email/password + role claim)
        ├── FirestoreRepository                    ├── Direct Firestore reads/writes, scoped by
        ├── FirebaseRealtimeService                │   the same Security Rules (RequireAdmin)
        └── FunctionsRepository                     └── (assignCoupon/inventory writes go direct
                │                                        to Firestore, not through Functions,
                ▼                                        since Functions need Blaze — see rules)
Cloud Functions (Node 20 / TypeScript) ── not currently deployed (needs Blaze) ──
        ├── checkoutOrder            atomic stock-decrement + order creation (runTransaction)
        ├── acceptDeliveryRun        atomic driver-claim on a staged run
        ├── proposeSubstitution /
        │   respondToSubstitution    picker ↔ customer substitution workflow
        ├── confirmProofOfDelivery   server-side OTP/QR check before status → DELIVERED
        ├── assignUserRole           admin-only RBAC role grants (custom claims)
        ├── assignCoupon             admin/manager coupon assignment
        └── onOrderCreate / onOrderStatusChange / onProductWrite / onUserCreate  (triggers)
                │
                ▼
Firebase project "mackson-delivery": Cloud Firestore · Realtime Database · Authentication
(Cloud Messaging / Storage / Functions provisioned in code, pending Blaze upgrade)
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full pattern-by-pattern mapping back
to Part 1 (which class implements which GoF pattern and why), and
[`docs/USER_STORY_TRACEABILITY.md`](docs/USER_STORY_TRACEABILITY.md) for a table linking every
user story in Part 1 to the exact file(s) that implement it.

## Repository structure

```
frontend/
  app/                  Android app (Kotlin, Jetpack Compose) — Customer, Picker, Driver only
    src/main/java/com/mackson/delivery/
      data/model/       Domain + DTO classes (mirrors the Part 1 class diagrams)
      data/remote/       FirebaseAuthManager, FirestoreRepository, FirebaseRealtimeService, FunctionsRepository
      data/repository/   CartStore (client-side cart session)
      domain/             Pure business logic: CheckoutCalculator, StockValidator, GeofenceValidator, DeliverySlotPlanner
      ui/auth/ customer/ picker/ driver/ navigation/ common/ theme/
    src/test/             JVM unit tests for domain/ logic (no Android/Firebase dependency)
  admin-web/             Store Manager/Admin web console (React, TypeScript, Vite)
    src/lib/              firebase.ts (SDK init), AuthContext.tsx (role-gated auth), types.ts
    src/components/       Layout, RequireAdmin, KpiCard, StatusPill
    src/pages/             LoginPage, DashboardPage, OrdersPage, InventoryPage, CustomersPage, DriversPage
backend/
  functions/             Cloud Functions backend (TypeScript) — shared by both clients
    src/                  checkout.ts, orders.ts, substitution.ts, delivery.ts, auth.ts, adminOps.ts, catalogue.ts
    test/                 Jest unit tests for pricing.ts / stock.ts
    seed/                 catalogue.ts (shared seed data), seedEmulator.ts, seedProduction.ts
  firestore/              firestore.rules, firestore.indexes.json, database.rules.json, storage.rules
.github/workflows/        ci.yml, cd-staging.yml, cd-production.yml
docs/                     Architecture + traceability docs referenced above
```

## Getting started

### 1. Prerequisites

- Android Studio (Ladybug or later) with an Android SDK — used to open/run `app/`
- Node.js 20+ and npm — used for `functions/`
- A Firebase project (free Spark plan is enough for everything except real SMS OTP, which
  needs Blaze) — create one at <https://console.firebase.google.com>
- The Firebase CLI: `npm install -g firebase-tools`, then `firebase login`

### 2. Firebase project setup

1. Create a Firebase project, then add an **Android app** to it with package name
   `com.mackson.delivery`.
2. Download the generated `google-services.json` and save it as `frontend/app/google-services.json`
   (this filename is gitignored on purpose — see `frontend/app/google-services.json.example` for the
   expected shape).
3. Enable in the Firebase console: **Authentication** (Email/Password provider, and Phone if
   you want real OTP), **Firestore Database**, **Realtime Database**, **Cloud Storage**,
   **Cloud Messaging**.
4. Copy `.firebaserc.example` to `.firebaserc` and replace the project id with your own.
5. Copy `local.properties.example` to `local.properties`. Android Studio will fill in `sdk.dir`
   automatically; add your own `MAPS_API_KEY` (Google Maps SDK for Android — needed for the
   live order-tracking screen) from
   <https://console.cloud.google.com/google/maps-apis/credentials>, using the same GCP project.

### 3. Run everything locally (recommended first run)

```bash
firebase emulators:start --only functions,firestore,database,auth,storage
```

In a second terminal, seed some demo data (store, catalogue, an on-duty picker, an available
driver, a store manager):

```bash
cd backend/functions
npm install
npm run seed
```

Then in Android Studio, run the app against the emulators (point the Firebase SDK at
`10.0.2.2` for the emulator host from an AVD, or use a physical device on the same network with
the emulator's `--host` flag). Register a new customer from the app's **Register** screen, or
sign in as `picker@mackson.demo` / `driver@mackson.demo` (password `Password123!`) to explore
the other two mobile roles from the role-hub screen after login. For the admin console, run
`npm run dev` inside `frontend/admin-web/` and sign in as `manager@mackson.demo` — see
`frontend/admin-web/README.md`.

### 4. Deploy the backend to a real Firebase project

```bash
firebase deploy --only firestore:rules,firestore:indexes,database
# Storage rules and Functions need the Blaze plan first — see "Scope decision" above:
# firebase deploy --only storage,functions
```

To seed a real (non-emulator) project with the same demo data, download a service account key
from Firebase Console → Project Settings → Service accounts → *Generate new private key*, save
it as `functions/serviceAccountKey.json` (gitignored — never commit this), then:

```bash
cd functions
npm run seed:production
```

### 5. Build the Android app

Open the repository root in Android Studio and let it sync, or from the command line:

```bash
./gradlew assembleDebug
```

### 6. Deploy the admin web console

```bash
cd admin-web
npm install
npm run build
cd ..
firebase deploy --only hosting
```

Hosting is on the free Spark plan (unlike Storage/Functions), so this works without Blaze. See
`admin-web/README.md` for the Firebase Web SDK config and local dev instructions.

## Testing

```bash
# Android — pure Kotlin domain logic (checkout math, stock validation, geofencing, slots)
./gradlew testDebugUnitTest

# Cloud Functions — pure pricing/stock logic, no emulator required
cd functions && npm test
```

Both suites are wired into `.github/workflows/ci.yml` and run on every pull request into
`develop`/`main`.

## CI/CD

Branching follows the GitFlow-style strategy documented in Part 1 section 9.3.14:

- **`main`** — production-ready; protected, no direct pushes.
- **`develop`** — integration branch; protected, no direct pushes.
- **`feature/US-xx-description`** — one branch per user story, opened against `develop`.
- **`hotfix/description`** — urgent production fixes, opened against `main`.

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | PR into `develop`/`main`, and pushes to those branches | Android: unit tests, lint, `assembleDebug`. Functions: lint, `tsc` build, Jest tests. |
| `cd-staging.yml` | push to `develop` (i.e. a merged PR) | Deploys Functions/Firestore rules/RTDB rules/Storage rules to the staging Firebase project; builds a debug APK and pushes it to Firebase App Distribution's `internal-testers` group. |
| `cd-production.yml` | push to `main` (i.e. a merged release PR) | Re-runs backend tests, deploys the backend to the production Firebase project, builds a release `.aab` (unsigned — see below) and uploads it as a workflow artifact. |

Required GitHub Actions secrets (Settings → Secrets and variables → Actions):

| Secret | Used by |
|---|---|
| `GOOGLE_SERVICES_JSON` | `ci.yml` — base64 of your `google-services.json` |
| `GOOGLE_SERVICES_JSON_STAGING` / `_PRODUCTION` | `cd-*.yml` — base64 of the per-environment config |
| `FIREBASE_SERVICE_ACCOUNT_STAGING` / `_PRODUCTION` | `cd-*.yml` — a Firebase service account JSON key with Firebase Admin permissions |
| `FIREBASE_PROJECT_ID_STAGING` / `_PRODUCTION` | `cd-*.yml` — the target project id |
| `FIREBASE_APP_ID_STAGING` | `cd-staging.yml` — the Android app id, for Firebase App Distribution |

```bash
base64 -i app/google-services.json | pbcopy   # macOS — paste as the secret value
```

**Branch protection**: enable "Require status checks to pass" (select the `android` and
`functions` jobs from `ci.yml`) and "Require at least 1 approving review" on both `main` and
`develop` in GitHub repo settings — this is what makes the PR-review-then-merge flow in Part 1
actually enforced rather than just described.

### Release signing

`cd-production.yml` builds an **unsigned** release bundle so the pipeline is demonstrable
without a Play Console account. To publish for real: generate a keystore, add it as a base64
secret, and add a `signingConfigs { release { ... } }` block to `app/build.gradle.kts` that
reads the keystore path/passwords from environment variables set in the workflow.

## Security

See Part 1 section 9.3.13 for the full write-up. In this codebase specifically:

- **RBAC** is enforced by Firebase Auth custom claims (`functions/src/auth.ts`) checked in both
  `firestore/firestore.rules` and `firestore/database.rules.json` — never trusted from the
  client.
- **Money- and stock-affecting operations** (`checkoutOrder`, `acceptDeliveryRun`,
  `confirmProofOfDelivery`) only ever run inside Cloud Functions using `runTransaction`, so a
  modified APK can't bypass the atomic stock-decrement or double-claim a delivery run.
- **Realtime Database** access to driver telemetry and substitution chat is scoped per-order via
  a `chatParticipants` / `runAssignments` mapping written by the Functions themselves — a
  customer can only ever read the chat/location for their own order, not anyone else's.
- Full OWASP/XSS/POPIA discussion lives in the combined Part 1+2 documentation, not duplicated
  here.
