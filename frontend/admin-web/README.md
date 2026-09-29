# Mackson's Delivery — Admin Web Console

Store Manager / Admin console for Mackson's Delivery. This is a **separate app from the mobile
app on purpose** — the client's requirement was that admin tooling live on the web, not inside
the Customer/Picker/Driver mobile app. It shares the exact same Firebase project (and therefore
the exact same live data) as `../app`, enforced by the same Firestore Security Rules
(`../../backend/firestore/firestore.rules`).

Live: **https://mackson-delivery.web.app**

## Stack

React 18 + TypeScript + Vite, Firebase Web SDK (Auth, Firestore, Realtime Database), React
Router, Recharts. No server of its own — it's a static SPA hosted on Firebase Hosting that talks
directly to Firebase from the browser, same as the Android app talks to Firebase from the phone.

## Local development

```bash
npm install
npm run dev
```

Opens on `http://localhost:5173` and talks to the **real** `mackson-delivery` Firebase project
(there's no local-emulator mode for this app — the Firebase config in `src/lib/firebase.ts` is
hardcoded to production, since an admin console has no realistic reason to run against fake
data). Sign in with a Store Manager or Admin account — e.g. the seeded demo account
`manager@mackson.demo` / `Password123!` (see `../../backend/functions/seed/catalogue.ts`). Any other role
(customer/picker/driver) can sign in but sees an access-denied screen instead of the console —
enforced both client-side (`src/components/RequireAdmin.tsx`) and, more importantly, by the
Firestore Security Rules themselves (every read/write this app makes requires the `ADMIN` or
`MANAGER` custom claim regardless of what the UI shows).

## Pages

| Page | What it does |
|---|---|
| Dashboard | Live KPIs (orders today, revenue today, active drivers, pending orders), order-status pie chart, recent orders — all via `onSnapshot` real-time listeners, not polling. |
| Orders | Full order list for the store, filterable by status. |
| Inventory | Live stock override (Part 1 US-23) — same data `app`'s catalogue reads, edits apply instantly. |
| Customers | Registered customer accounts, with a coupon-assignment dialog (Part 1 US-25). |
| Drivers | Registered drivers and their live availability status. |

## Why some writes go straight to Firestore instead of through a Cloud Function

Part 1's architecture routes admin actions like coupon assignment through callable Cloud
Functions (`assignCoupon`, `assignUserRole`). Those Functions are written and tested
(`../../backend/functions/src/adminOps.ts`, `../../backend/functions/src/auth.ts`) but not currently deployed — Cloud
Functions require the Firebase project to be on the Blaze (pay-as-you-go) plan, which needs a
billing card on file, which this project's owner has chosen to hold off on for now. Rather than
leave the admin console partially non-functional, the Firestore Security Rules
(`../../backend/firestore/firestore.rules`) trust a **direct** write from an ADMIN/MANAGER-claimed account
exactly as much as they'd trust the Cloud Function doing the same write — both paths require the
same custom claim, so this isn't a security downgrade, just a different caller. Once Blaze is
enabled, deploying the Functions and switching these two calls to `httpsCallable` instead of
`setDoc`/`updateDoc` is a small, contained change.

## Build & deploy

```bash
npm run build       # outputs to dist/
cd ../..
firebase deploy --only hosting
```

Firebase Hosting is free on the Spark plan (unlike Storage and Functions), so this deploys
without needing Blaze.

## Design

Same navy (`#122a4e`) / gold (`#d4a017`) / Poppins design language as the Android app
(`../app/src/main/java/com/mackson/delivery/ui/theme/`), built around the "Administrator App"
sidebar-dashboard layout from the Part 1 Figma prototype — dark navy sidebar with the MACKSONS
wordmark, KPI cards with icon chips, a data table for recent activity. See `src/theme.css` for
the token definitions.
