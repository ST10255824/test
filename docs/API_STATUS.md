# API Layer — Status and Architecture

This documents every API this project talks to: what's written, what's live, and why.

## Cloud Functions (`functions/src/`) — written, not deployed

All server-side business logic is implemented as Firebase Cloud Functions, each with a single
responsibility and typed request/response contracts (see `functions/src/types.ts`):

| Function | Trigger | Purpose |
|---|---|---|
| `checkoutOrder` | Callable (HTTPS) | Atomic stock-decrement transaction, order creation |
| `acceptDeliveryRun` | Callable (HTTPS) | Driver claims a staged run (transactional, race-safe) |
| `confirmProofOfDelivery` | Callable (HTTPS) | Server-side OTP/QR verification before marking DELIVERED |
| `proposeSubstitution` / `respondToSubstitution` | Callable (HTTPS) | Picker/customer substitution handshake |
| `onOrderCreate` | Firestore trigger | Auto-assigns the order to an on-duty picker |
| `onOrderStatusChange` | Firestore trigger | Pushes an FCM notification to the customer |
| `onUserCreate` / `assignUserRole` | Auth trigger / Callable | Custom-claim role assignment |
| `assignCoupon` | Callable (HTTPS) | Admin issues a loyalty coupon |
| `onProductWrite` | Firestore trigger | Maintains the lowercase search index field |
| `sendOrderConfirmationEmail` | Firestore trigger | Emails a receipt via the SendGrid REST API |

Every one of these is unit-tested (`functions/test/`) and would deploy with a single
`firebase deploy --only functions` command.

**Why they're not deployed:** Cloud Functions require the Firebase Blaze (pay-as-you-go) plan —
this is a Google Cloud platform requirement, not a Firebase-specific limitation, and it applies
regardless of whether usage would exceed the free tier. This project intentionally stays on the
free Spark plan, so these Functions are not live in production right now.

**What runs instead:** the exact same business logic (atomic stock decrement, dispatch
verification, substitution handshake, etc.) is re-implemented client-side as Firestore
transactions, authorised by scoped Firestore/RTDB Security Rules instead of server-side Admin
SDK trust — see `firestore/firestore.rules` and the corresponding methods in
`FirestoreRepository.kt` and `admin-web/src/pages/`. This is a deliberate, documented trade-off,
not an oversight: every relaxed rule and every client-side transaction carries a code comment
explaining which undeployed Function it stands in for and why the substitution is safe (e.g.
Firestore transactions still close the same race windows a Cloud Function transaction would).

## Live API: OpenStreetMap Nominatim

The one genuinely live, external HTTP API this project calls in production is address
search/geocoding for delivery addresses (`AddressSearchService.kt`):

- **Endpoint:** `GET https://nominatim.openstreetmap.org/search`
- **Why this instead of Google Places Autocomplete:** Google Places (and Google Maps SDK) also
  require a billing-enabled Google Cloud project — the same Blaze-style trade-off as Cloud
  Functions. Nominatim is free with no billing account required.
- **Request handling:** query params (`q`, `countrycodes=za`, a `viewbox` biased toward the
  store's coordinates, `bounded=0` for soft rather than hard geographic filtering), a required
  `User-Agent` header identifying the app per Nominatim's usage policy, and keystrokes are
  debounced 450ms client-side to stay within Nominatim's ~1 request/second rate limit.
- **Response handling:** JSON array parsed into `AddressSuggestion` (display name, lat/lng);
  network or parse failures degrade to an empty result list rather than crashing the screen.
- **Downstream validation:** every returned address is checked against the store's delivery
  radius using `GeofenceValidator` (Haversine distance) before a customer can select it.

## Summary

| Layer | Status |
|---|---|
| Cloud Functions (custom REST-like API surface) | Written, unit-tested, **not deployed** (needs Blaze) |
| Firestore/RTDB (client SDK, rules-gated) | **Live** — replaces the above in production |
| Nominatim address search | **Live** — real external HTTP API, in production use |
| Firebase Auth, Firestore, RTDB, Hosting | **Live** — all free-tier, no billing required |
