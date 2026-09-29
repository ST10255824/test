# Architecture reference

Companion to Part 1 sections 9.3.9 (Analysis Artifact) and 9.3.12 (Architecture Artifacts).
This maps every pattern and class named in the documentation to where it actually lives in
this codebase.

## Design patterns (GoF, Part 1 section 9.3.12 item 2)

| Pattern | Where | Why |
|---|---|---|
| **Observer** | `FirestoreRepository.listenToOrderUpdates` / `.listenToStoreOrders`, `FirebaseRealtimeService.observeDriverLocation` / `.observeSubstitutionChat` — all Kotlin `Flow`s backed by Firestore/RTDB snapshot listeners | UI redraws the instant the backend changes (order status, driver GPS, chat) with no manual refresh or polling. |
| **Singleton** | `FirebaseAuthManager`, `FirestoreRepository`, `FirebaseRealtimeService`, `FunctionsRepository`, `CartStore` — all Kotlin `object` declarations | One connection/session context per SDK for the app's whole lifetime; matches Part 1's "Kotlin's object declaration... ensures a single, cohesive connection context." |
| **Repository** | `FirestoreRepository` (DAO over Firestore), `FirebaseRealtimeService` (DAO over RTDB) | Hides Firebase SDK calls behind a clean interface — ViewModels never import `com.google.firebase.firestore.*` directly. |
| **Factory / DI** | `util/ViewModelFactory.kt` (`viewModelFactory { ... }`) | Every screen builds its ViewModel by injecting the singleton repositories through a constructor, e.g. `viewModelFactory { CheckoutViewModel() }`, so a test can substitute a fake repository without touching the screen. |

## Architecture patterns (Part 1 section 2.2)

| Pattern | Where |
|---|---|
| **MVVM** | Every `ui/<role>/XyzScreen.kt` + its paired `XyzViewModel` (`StateFlow<UiState>` exposed, Compose `collectAsStateWithLifecycle()` on the View side). |
| **Layered (N-Tier)** | Presentation (`ui/`) → Domain/Data Access (`domain/`, `data/remote/`) → Cloud Service Persistence (Firebase). |
| **Serverless, event-driven** | `functions/src/orders.ts` (`onOrderCreate`, `onOrderStatusChange`), `functions/src/catalogue.ts` (`onProductWrite`), `functions/src/auth.ts` (`onUserCreate`) — all Firestore/Auth triggers, no server to provision. |

## Domain class diagram → Kotlin data classes

| Part 1 class | Kotlin file |
|---|---|
| System Manager | `data/model/UserModels.kt` → `SystemAdminProfile` |
| Store Manager | `data/model/UserModels.kt` → `StoreManagerProfile` |
| Customer | `data/model/UserModels.kt` → `Customer` |
| StoreNode | `data/model/CatalogModels.kt` → `StoreNode` |
| Product | `data/model/CatalogModels.kt` → `Product` |
| Shopping Cart | `data/model/CartModels.kt` → `ShoppingCart`, `CartItem` |
| Order | `data/model/OrderModels.kt` → `Order`, `OrderItem` |
| Personal Shopper | `data/model/UserModels.kt` → `PickerProfile` |
| Delivery Driver | `data/model/UserModels.kt` → `DriverProfile` |

## The one thing that got the most testing attention

Part 1 section 2.3 flags concurrent checkout (two customers buying the last unit) and, by the
same logic, two drivers accepting the same delivery run, as the highest-risk race conditions in
the system. Both are handled with a Firestore `runTransaction`:

- `functions/src/checkout.ts` — reads live stock inside the transaction, validates it with
  `assertSufficientStock` (`functions/src/stock.ts`), and decrements it in the same transaction
  that creates the order. A client can never see a stale stock count and check out anyway.
- `functions/src/delivery.ts` (`acceptDeliveryRun`) — reads the order inside a transaction and
  only assigns `driverId` if the order is still `READY_FOR_COLLECTION` with no driver attached.

Both are covered by Jest tests for the pure validation logic they call
(`functions/test/stock.test.ts`); the transactional wrapper itself needs the Firestore emulator
to test end-to-end (see `npm run seed` + manual verification in the README), since Jest alone
can't simulate two concurrent Firestore transactions racing.
