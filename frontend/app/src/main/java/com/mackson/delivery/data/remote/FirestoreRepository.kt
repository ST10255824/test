package com.mackson.delivery.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mackson.delivery.data.model.CartItem
import com.mackson.delivery.data.model.Customer
import com.mackson.delivery.data.model.DeliveryAddress
import com.mackson.delivery.data.model.DeliverySlotType
import com.mackson.delivery.data.model.DriverProfile
import com.mackson.delivery.data.model.Order
import com.mackson.delivery.data.model.OrderItem
import com.mackson.delivery.data.model.OrderStatus
import com.mackson.delivery.data.model.PickerProfile
import com.mackson.delivery.data.model.Product
import com.mackson.delivery.data.model.ShoppingCart
import com.mackson.delivery.data.model.StoreNode
import com.mackson.delivery.domain.CheckoutCalculator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Main Data Access Object (DAO) performing CRUD against Cloud Firestore
 * (Part 1, section 9.3.9: "FirestoreRepository"). Wrapping every Firestore call behind this
 * object is the Repository pattern documented in section 9.3.12 — ViewModels never touch the
 * Firebase SDK directly, so the UI layer is unaffected if the underlying database changes.
 */
object FirestoreRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_STORES = "storeNodes"
    private const val COLLECTION_PRODUCTS = "products"
    private const val COLLECTION_ORDERS = "orders"
    private const val COLLECTION_PICKERS = "pickers"
    private const val COLLECTION_DRIVERS = "drivers"

    // ---- Store / catalogue ------------------------------------------------------------

    suspend fun fetchNearbyStores(): List<StoreNode> =
        db.collection(COLLECTION_STORES)
            .whereEqualTo("isAcceptingOrders", true)
            .get().await()
            .documents.mapNotNull { it.toObject(StoreNode::class.java) }

    /** A single store's own coordinates/service radius — used by AddressSearchScreen to check a
     * chosen delivery address against the store's geofence (Part 1 US-02). */
    suspend fun fetchStore(storeId: String): StoreNode? =
        db.collection(COLLECTION_STORES).document(storeId).get().await().toObject(StoreNode::class.java)

    /** Loads the localised catalogue for one store — Part 1 US-03. */
    suspend fun fetchProducts(storeId: String, category: String? = null): List<Product> {
        var query: Query = db.collection(COLLECTION_STORES)
            .document(storeId)
            .collection(COLLECTION_PRODUCTS)
        if (!category.isNullOrBlank()) {
            query = query.whereEqualTo("category", category)
        }
        return query.get().await().documents.mapNotNull { it.toObject(Product::class.java) }
    }

    suspend fun fetchProduct(storeId: String, productId: String): Product? =
        db.collection(COLLECTION_STORES)
            .document(storeId)
            .collection(COLLECTION_PRODUCTS)
            .document(productId)
            .get().await()
            .toObject(Product::class.java)

    suspend fun fetchCategories(storeId: String): List<String> =
        fetchProducts(storeId).map { it.category }.distinct().sorted()

    suspend fun searchProducts(storeId: String, term: String): List<Product> {
        // Firestore has no native full-text search; for the marking demo this does a
        // prefix match on a lowercase "nameSearchKey" field maintained by onProductWrite
        // (functions/src/catalogue.ts). Production would move this to Algolia/Typesense.
        val lower = term.lowercase()
        return db.collection(COLLECTION_STORES)
            .document(storeId)
            .collection(COLLECTION_PRODUCTS)
            .orderBy("nameSearchKey")
            .startAt(lower)
            .endAt(lower + "")
            .get().await()
            .documents.mapNotNull { it.toObject(Product::class.java) }
    }

    /** Picker barcode scan lookup — Part 1 US-16. */
    suspend fun fetchProductByBarcode(storeId: String, barcode: String): Product? =
        db.collection(COLLECTION_STORES)
            .document(storeId)
            .collection(COLLECTION_PRODUCTS)
            .whereEqualTo("barcode", barcode)
            .limit(1)
            .get().await()
            .documents.firstOrNull()?.toObject(Product::class.java)

    // Live inventory override (Part 1 US-23) is an admin-only action, done from admin-web/
    // rather than this Android app — see InventoryPage.tsx there for the equivalent write.

    // ---- Users / accounts ---------------------------------------------------------------

    suspend fun fetchCustomerProfile(uid: String): Customer? =
        db.collection(COLLECTION_USERS).document(uid).get().await().toObject(Customer::class.java)

    suspend fun saveCustomerProfile(customer: Customer) {
        db.collection(COLLECTION_USERS).document(customer.customerId).set(customer).await()
    }

    // Managing customer accounts / assigning coupons (Part 1 US-25) is an admin-only action,
    // done from admin-web/ rather than this Android app — see CustomersPage.tsx there.

    suspend fun fetchAvailablePickers(storeId: String): List<PickerProfile> =
        db.collection(COLLECTION_PICKERS)
            .whereEqualTo("assignedStoreId", storeId)
            .get().await()
            .documents.mapNotNull { it.toObject(PickerProfile::class.java) }

    suspend fun fetchAvailableDrivers(): List<DriverProfile> =
        db.collection(COLLECTION_DRIVERS)
            .whereEqualTo("isAvailable", true)
            .get().await()
            .documents.mapNotNull { it.toObject(DriverProfile::class.java) }

    /** Customer tracking screen shows this for door-side verification (name/photo/plate). */
    suspend fun fetchDriverProfile(driverId: String): DriverProfile? =
        db.collection(COLLECTION_DRIVERS).document(driverId).get().await().toObject(DriverProfile::class.java)

    // ---- Orders -----------------------------------------------------------------------

    /** Persists a new order with status RECEIVED. Prefer the `checkoutOrder` callable function
     * for real checkouts (it does the atomic stock decrement) — this is used for admin/manual
     * order edits and by the Cloud Function itself. */
    suspend fun saveOrder(order: Order) {
        db.collection(COLLECTION_ORDERS).document(order.orderId).set(order).await()
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, extra: Map<String, Any?> = emptyMap()) {
        val updates = mutableMapOf<String, Any?>("orderStatus" to status.name)
        updates.putAll(extra)
        db.collection(COLLECTION_ORDERS).document(orderId).update(updates).await()
    }

    suspend fun fetchOrderHistory(customerId: String): List<Order> =
        db.collection(COLLECTION_ORDERS)
            .whereEqualTo("customerId", customerId)
            .orderBy("placementTime", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }

    suspend fun fetchPickerManifest(shopperId: String): List<Order> =
        db.collection(COLLECTION_ORDERS)
            .whereEqualTo("shopperId", shopperId)
            .whereEqualTo("orderStatus", OrderStatus.PICKING.name)
            .get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }

    /**
     * All runs currently staged for collection, across stores — Part 1 US-19 ("proximity
     * dashboard... within a specified radius"). Real geo-proximity filtering needs a geohash
     * index (e.g. GeoFire for Firestore); left as a follow-up since it needs live driver
     * coordinates to rank against, which only exist once a driver goes on duty.
     */
    suspend fun fetchAllAvailableRuns(): List<Order> =
        db.collection(COLLECTION_ORDERS)
            .whereEqualTo("orderStatus", OrderStatus.READY_FOR_COLLECTION.name)
            .whereEqualTo("driverId", null)
            .get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }

    /**
     * Live version of fetchAllAvailableRuns — a real OS-level push notification the instant a
     * new run is staged would need a Cloud Function trigger + FCM, which needs the Blaze plan
     * this project doesn't have. This is the closest equivalent reachable on the free Spark
     * plan: while a driver has the jobs screen open, a newly staged run appears within the same
     * second via Firestore's SnapshotListener, with no manual refresh needed.
     */
    /**
     * A driver's already-accepted, not-yet-delivered run, if one exists — so a driver who
     * leaves the active-run screen mid-delivery (backgrounds the app, signs out and back in,
     * the process gets killed) lands back on it instead of an empty jobs list with no way back
     * in. Filtered client-side rather than with a second whereEqualTo/whereIn on orderStatus so
     * this never needs a composite Firestore index.
     */
    suspend fun fetchActiveRunForDriver(driverId: String): Order? =
        db.collection(COLLECTION_ORDERS)
            .whereEqualTo("driverId", driverId)
            .get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }
            .firstOrNull {
                it.orderStatus in setOf(OrderStatus.READY_FOR_COLLECTION, OrderStatus.EN_ROUTE, OrderStatus.ARRIVED_AT_NODE)
            }

    fun listenToAvailableRuns(): Flow<List<Order>> = callbackFlow {
        val registration = db.collection(COLLECTION_ORDERS)
            .whereEqualTo("orderStatus", OrderStatus.READY_FOR_COLLECTION.name)
            .whereEqualTo("driverId", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(Order::class.java) } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun fetchAvailableRunsForDrivers(storeId: String): List<Order> =
        db.collection(COLLECTION_ORDERS)
            .whereEqualTo("storeId", storeId)
            .whereEqualTo("orderStatus", OrderStatus.READY_FOR_COLLECTION.name)
            .get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }

    // The operational dashboard's live order snapshot (Part 1 US-24) is now
    // admin-web/'s DashboardPage.tsx, not this Android app.

    /** Picker marks one manifest line as physically picked — Part 1 US-16.
     * Stored in a subcollection so concurrent scans never race on a single array field. */
    suspend fun markItemPicked(orderId: String, productId: String, picked: Boolean) {
        db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .collection("pickProgress")
            .document(productId)
            .set(mapOf("picked" to picked))
            .await()
    }

    suspend fun markAllItemsPicked(orderId: String, productIds: List<String>) {
        val batch = db.batch()
        productIds.forEach { productId ->
            val docRef = db.collection(COLLECTION_ORDERS)
                .document(orderId)
                .collection("pickProgress")
                .document(productId)
            batch.set(docRef, mapOf("picked" to true))
        }
        batch.commit().await()
    }

    fun observePickProgress(orderId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val registration = db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .collection("pickProgress")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val progress = snapshot?.documents
                    ?.associate { it.id to (it.getBoolean("picked") ?: false) }
                    ?: emptyMap()
                trySend(progress)
            }
        awaitClose { registration.remove() }
    }

    /** Driver scans each item off against the basket before proof of delivery is unlocked —
     * WIL group requirement: "driver must have a barcode scanner similar to the in store picker
     * that automatically ticks off the items in the basket... and that there wasn't any
     * mishaps." Same subcollection-per-order pattern as pickProgress, for the same
     * race-avoidance reason. */
    suspend fun markItemDelivered(orderId: String, productId: String, delivered: Boolean) {
        db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .collection("deliveryProgress")
            .document(productId)
            .set(mapOf("delivered" to delivered))
            .await()
    }

    suspend fun markAllItemsDelivered(orderId: String, productIds: List<String>) {
        val batch = db.batch()
        productIds.forEach { productId ->
            val docRef = db.collection(COLLECTION_ORDERS)
                .document(orderId)
                .collection("deliveryProgress")
                .document(productId)
            batch.set(docRef, mapOf("delivered" to true))
        }
        batch.commit().await()
    }

    fun observeDeliveryProgress(orderId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val registration = db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .collection("deliveryProgress")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val progress = snapshot?.documents
                    ?.associate { it.id to (it.getBoolean("delivered") ?: false) }
                    ?: emptyMap()
                trySend(progress)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Client-side stand-in for the checkoutOrder Cloud Function (functions/src/checkout.ts).
     * That Function is written and tested but never deployed — Cloud Functions require the
     * Blaze plan, and this project stays on the free Spark plan — so the exact same atomic
     * transaction (re-validate stock, decrement it, price the order from live product data)
     * runs here instead, authorised by the relaxed rules in firestore/firestore.rules. It also
     * folds in what the onOrderCreate trigger would have done (auto-assign the first on-duty
     * picker) as part of the same write, since that trigger is undeployed too.
     */
    suspend fun checkoutOrder(
        storeId: String,
        cartItems: List<CartItem>,
        deliverySlotType: DeliverySlotType,
        deliverySlotLabel: String,
        hasLoyaltyCard: Boolean,
        driverTip: Double,
        deliveryAddress: DeliveryAddress
    ): String {
        val customerId = FirebaseAuthManager.getCurrentUserId() ?: error("You're not signed in.")

        // Firestore transactions can only re-read documents by reference, not run queries, so
        // the on-duty picker lookup happens just before the transaction starts.
        val pickerId = db.collection(COLLECTION_PICKERS)
            .whereEqualTo("assignedStoreId", storeId)
            .whereEqualTo("dutyStatus", "ON_DUTY")
            .limit(1)
            .get().await()
            .documents.firstOrNull()?.id

        val orderRef = db.collection(COLLECTION_ORDERS).document()
        val productRefs = cartItems.map {
            db.collection(COLLECTION_STORES).document(storeId).collection(COLLECTION_PRODUCTS).document(it.productId)
        }

        val newOrder = db.runTransaction { transaction ->
            val products = productRefs.mapIndexed { index, ref ->
                val snap = transaction.get(ref)
                if (!snap.exists()) error("${cartItems[index].name} is no longer available.")
                snap.toObject(Product::class.java) ?: error("${cartItems[index].name} is no longer available.")
            }
            products.forEachIndexed { index, product ->
                if (product.currentStockLevel < cartItems[index].quantity) {
                    error("Only ${product.currentStockLevel} of ${product.name} left in stock.")
                }
            }
            productRefs.forEachIndexed { index, ref ->
                transaction.update(ref, "currentStockLevel", products[index].currentStockLevel - cartItems[index].quantity)
            }

            val breakdown = CheckoutCalculator.calculate(
                cart = ShoppingCart(storeId = storeId, items = cartItems),
                hasLinkedLoyaltyCard = hasLoyaltyCard,
                driverTip = driverTip
            )
            val orderItems = products.mapIndexed { index, product ->
                OrderItem(
                    productId = product.productId,
                    name = product.name,
                    quantity = cartItems[index].quantity,
                    unitPrice = product.unitPrice,
                    aisleNumber = product.aisleNumber,
                    barcode = product.barcode,
                    imageUrl = product.imageUrl,
                    isSubstituted = false,
                    substitutionApproved = null
                )
            }

            val order = Order(
                orderId = orderRef.id,
                customerId = customerId,
                storeId = storeId,
                shopperId = pickerId,
                driverId = null,
                orderStatus = if (pickerId != null) OrderStatus.PICKING else OrderStatus.RECEIVED,
                items = orderItems,
                subtotal = breakdown.subtotal,
                loyaltyDiscount = breakdown.loyaltyDiscount,
                driverTip = breakdown.driverTip,
                serviceFee = breakdown.serviceFee,
                totalAmount = breakdown.total,
                deliverySlotType = deliverySlotType,
                deliverySlotLabel = deliverySlotLabel,
                deliveryAddress = deliveryAddress,
                paymentReferenceToken = "TOKENISED-${orderRef.id}",
                placementTime = System.currentTimeMillis(),
                scheduledTime = null,
                deliveredTime = null,
                deliveryOtp = (100000..999999).random().toString(),
                deliveryQrToken = "${orderRef.id}-${(100000..999999).random()}",
                dispatchConfirmedByAdmin = false
            )
            transaction.set(orderRef, order)
            order
        }.await()

        // Lets the Realtime Database rules authorise the substitution chat for this order
        // (chatParticipants/{orderId}) without ever having to trust a client-supplied
        // customerId on read — mirrors what checkoutOrder + onOrderCreate did server-side.
        FirebaseRealtimeService.setChatParticipants(newOrder.orderId, customerId, pickerId)
        return newOrder.orderId
    }

    /**
     * Client-side stand-in for the acceptDeliveryRun Cloud Function (functions/src/delivery.ts)
     * — undeployed for the same Blaze/Spark reason as checkoutOrder. Still wrapped in a
     * transaction so two drivers tapping "Accept" on the same run at the same instant can't
     * both win it.
     */
    suspend fun acceptDeliveryRun(orderId: String, driverId: String) {
        val orderRef = db.collection(COLLECTION_ORDERS).document(orderId)
        val customerId = db.runTransaction { transaction ->
            val snap = transaction.get(orderRef)
            val current = snap.toObject(Order::class.java) ?: error("Order not found.")
            if (current.orderStatus != OrderStatus.READY_FOR_COLLECTION || current.driverId != null) {
                error("This run has already been accepted by another driver.")
            }
            transaction.update(orderRef, "driverId", driverId)
            current.customerId
        }.await()

        FirebaseRealtimeService.setRunAssignment(driverId, orderId, customerId)
    }

    /**
     * Client-side stand-in for confirmProofOfDelivery (functions/src/delivery.ts). The OTP/QR
     * comparison that Function did server-side now runs here instead — a real production
     * deployment would keep this check server-side (a modified client could in theory always
     * report success), but there is no server compute available on the free Spark plan.
     */
    suspend fun confirmProofOfDelivery(orderId: String, method: String, code: String) {
        val driverId = FirebaseAuthManager.getCurrentUserId() ?: error("You're not signed in.")
        val orderRef = db.collection(COLLECTION_ORDERS).document(orderId)
        val order = orderRef.get().await().toObject(Order::class.java) ?: error("Order not found.")

        if (order.driverId != driverId) error("You are not the driver for this order.")

        val isValid = (method == "OTP" && code == order.deliveryOtp) ||
            (method == "QR_CODE" && code == order.deliveryQrToken) ||
            method == "PHOTO"
        if (!isValid) error("That code doesn't match this order.")

        orderRef.update(
            mapOf(
                "orderStatus" to OrderStatus.DELIVERED.name,
                "deliveredTime" to System.currentTimeMillis()
            )
        ).await()
    }

    /**
     * Client-side stand-in for proposeSubstitution (functions/src/substitution.ts). Returns the
     * replacement product so the caller can push the chat message via FirebaseRealtimeService
     * (RTDB writes stay in that service, matching the existing split of responsibilities).
     */
    suspend fun proposeSubstitution(orderId: String, productId: String, replacementProductId: String): Product {
        val orderRef = db.collection(COLLECTION_ORDERS).document(orderId)
        val order = orderRef.get().await().toObject(Order::class.java) ?: error("Order not found.")
        val replacement = db.collection(COLLECTION_STORES).document(order.storeId)
            .collection(COLLECTION_PRODUCTS).document(replacementProductId)
            .get().await().toObject(Product::class.java) ?: error("Replacement product not found.")

        val updatedItems = order.items.map {
            if (it.productId == productId) it.copy(isSubstituted = true, substitutionApproved = null) else it
        }
        orderRef.update("items", updatedItems).await()
        return replacement
    }

    /** Client-side stand-in for respondToSubstitution (functions/src/substitution.ts). */
    suspend fun respondToSubstitution(orderId: String, proposedProductId: String, approved: Boolean) {
        val orderRef = db.collection(COLLECTION_ORDERS).document(orderId)
        val order = orderRef.get().await().toObject(Order::class.java) ?: error("Order not found.")

        val replacement = if (approved) {
            db.collection(COLLECTION_STORES).document(order.storeId)
                .collection(COLLECTION_PRODUCTS).document(proposedProductId)
                .get().await().toObject(Product::class.java)
        } else null

        val updatedItems = order.items.map { item ->
            if (item.productId != proposedProductId && !(item.isSubstituted && item.substitutionApproved == null)) {
                return@map item
            }
            if (!approved) {
                item.copy(isSubstituted = false, substitutionApproved = null)
            } else {
                item.copy(
                    name = replacement?.name ?: item.name,
                    unitPrice = replacement?.unitPrice ?: item.unitPrice,
                    imageUrl = replacement?.imageUrl ?: item.imageUrl,
                    isSubstituted = true,
                    substitutionApproved = true
                )
            }
        }
        orderRef.update("items", updatedItems).await()
    }

    /** Store staff/admin sign-off that a driver's dispatch code matches the physical order at
     * the counter — see DriverActiveRunScreen. Written from admin-web's Orders page. */
    suspend fun confirmDispatch(orderId: String) {
        db.collection(COLLECTION_ORDERS).document(orderId).update("dispatchConfirmedByAdmin", true).await()
    }

    /**
     * Live order tracking stream. Implements the Observer pattern (Part 1, section 9.3.12,
     * "Pattern of Observation"): Firestore's SnapshotListener notifies every collector as soon
     * as the order document changes, so the UI redraws without a manual refresh or page reload.
     */
    fun listenToOrderUpdates(orderId: String): Flow<Order?> = callbackFlow {
        val registration: ListenerRegistration = db.collection(COLLECTION_ORDERS)
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Order::class.java))
            }
        awaitClose { registration.remove() }
    }
}
