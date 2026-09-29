# User story traceability

Maps every user story from Part 1 (Table 1, section 1.3) to where it's implemented in this
repository. "Screen" links are Compose UI; "Logic" links are the ViewModel/domain/backend code
behind them.

## Customer

| Story | Screen | Logic |
|---|---|---|
| US-01 — OTP registration/login | `ui/auth/LoginScreen.kt`, `ui/auth/OtpScreen.kt` | `ui/auth/AuthViewModel.kt`, `ui/auth/OtpViewModel.kt`, `data/remote/FirebaseAuthManager.kt`, `functions/src/auth.ts` |
| US-02 — GPS / pinned delivery addresses within 5km | *(address capture is in `Customer.savedAddresses` / `DeliveryAddress`)* | `domain/GeofenceValidator.kt` (+ `domain/GeofenceValidatorTest.kt`) |
| US-03 — select nearby store, localised catalogue | `ui/customer/HomeScreen.kt` | `ui/customer/HomeScreen.kt: HomeViewModel`, `FirestoreRepository.fetchNearbyStores` |
| US-04 — category browse + search | `ui/customer/BrowseScreen.kt`, `ui/customer/SearchScreen.kt` | `BrowseViewModel`, `SearchViewModel`, `FirestoreRepository.fetchProducts/searchProducts`, `functions/src/catalogue.ts` |
| US-05 — barcode scan for product details | `ui/picker/BarcodeScanScreen.kt` (shared scanner component) | `FirestoreRepository.fetchProductByBarcode` |
| US-06 — product detail, real-time-parity pricing | `ui/customer/ProductDetailScreen.kt` | `ProductDetailViewModel` |
| US-07 — responsive cart | `ui/customer/CartScreen.kt` | `data/repository/CartStore.kt` |
| US-08 — saved shopping lists | *(schema only: `data/model/CatalogModels.kt: ShoppingList`)* | not wired to a screen in this build — see README scope decisions |
| US-09 — delivery slot selection | `ui/customer/CheckoutScreen.kt` | `domain/DeliverySlotPlanner.kt` (+ test) |
| US-10 — loyalty card / voucher discount | `ui/customer/CheckoutScreen.kt`, `ui/customer/ProfileScreen.kt` | `domain/CheckoutCalculator.kt` (+ test), `functions/src/pricing.ts` (+ test) |
| US-11 — driver tip | `ui/customer/CheckoutScreen.kt` | `domain/CheckoutCalculator.kt` |
| US-12 — tokenised payment | `ui/customer/CheckoutScreen.kt` | `functions/src/checkout.ts` (`paymentReferenceToken`, no raw card data ever touches the client or Firestore) |
| US-13 — live tracking + push notifications | `ui/customer/TrackingScreen.kt` | `TrackingViewModel`, `FirestoreRepository.listenToOrderUpdates`, `FirebaseRealtimeService.observeDriverLocation`, `functions/src/orders.ts: onOrderStatusChange` (FCM) |
| US-14 — substitution chat | `ui/customer/ChatScreen.kt` | `ChatViewModel`, `FirebaseRealtimeService`, `functions/src/substitution.ts` |

## In-Store Picker

| Story | Screen | Logic |
|---|---|---|
| US-15 — manifest sorted by aisle | `ui/picker/PickerManifestScreen.kt`, `ui/picker/PickerOrderDetailScreen.kt` | `FirestoreRepository.fetchPickerManifest`, items sorted by `aisleNumber` |
| US-16 — barcode scan to pick | `ui/picker/BarcodeScanScreen.kt`, `ui/picker/PickerScanRoute.kt` | `FirestoreRepository.markItemPicked/observePickProgress` |
| US-17 — flag OOS + propose substitution | `ui/picker/PickerOrderDetailScreen.kt` → scan flow | `functions/src/substitution.ts: proposeSubstitution` |
| US-18 — stage order for collection | `ui/picker/PickerOrderDetailScreen.kt` | `FirestoreRepository.updateOrderStatus`, `functions/src/orders.ts` (customer notification) |

## Delivery Driver

| Story | Screen | Logic |
|---|---|---|
| US-19 — proximity job dashboard | `ui/driver/DriverJobsScreen.kt` | `FirestoreRepository.fetchAllAvailableRuns`, `functions/src/delivery.ts: acceptDeliveryRun` |
| US-20 — turn-by-turn navigation | `ui/driver/DriverActiveRunScreen.kt` | `Intent(ACTION_VIEW, google.navigation:q=...)` handoff to Google Maps |
| US-21 — manual status milestones | `ui/driver/DriverActiveRunScreen.kt` | `FirestoreRepository.updateOrderStatus`, `FusedLocationProviderClient` GPS streaming while `EN_ROUTE` |
| US-22 — proof of delivery | `ui/driver/DriverProofScreen.kt` | `functions/src/delivery.ts: confirmProofOfDelivery` |

## Store Manager / System Admin

Served entirely by the separate **admin-web/** console (not the mobile app) — see
`admin-web/README.md` for why admin lives on the web.

| Story | Screen | Logic |
|---|---|---|
| US-23 — live inventory override | `admin-web/src/pages/InventoryPage.tsx` | direct `updateDoc` on `storeNodes/{id}/products/{id}`, permitted by `firestore.rules` for ADMIN/MANAGER |
| US-24 — operational dashboard, live refresh | `admin-web/src/pages/DashboardPage.tsx` | `onSnapshot` real-time listener (stronger than the 30s-poll NFR — updates instantly instead) |
| US-25 — role/permission management, coupons | `admin-web/src/pages/CustomersPage.tsx` | direct `setDoc` on `users/{id}/coupons/{code}`; `functions/src/auth.ts: assignUserRole` / `functions/src/adminOps.ts: assignCoupon` are the equivalent Cloud Functions, not yet deployed (see README "Cloud Functions require Blaze") |
| US-26 — delivery radius / global config | *(not built)* | `storeNodes/{id}` is writable by ADMIN/MANAGER per the rules, but no settings screen exists yet in `admin-web/` — a documented follow-up |
