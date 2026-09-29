package com.mackson.delivery.data.model

/**
 * Line item snapshotted into the order at checkout time (denormalised, per Part 1
 * section 9.3.11 — avoids a second read against the live product catalogue when
 * rendering receipts or order history).
 */
data class OrderItem(
    val productId: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val aisleNumber: Int = 1,
    val barcode: String = "",
    val imageUrl: String = "",
    val isSubstituted: Boolean = false,
    val substitutionApproved: Boolean? = null
)

/**
 * The primary transactional record of a purchase.
 * Firestore collection: orders/{orderId}. Field names match the JSON schema
 * documented in Part 1, section 9.3.11 (orders Document Schema).
 */
data class Order(
    val orderId: String = "",
    val customerId: String = "",
    val storeId: String = "",
    val shopperId: String? = null,
    val driverId: String? = null,
    val orderStatus: OrderStatus = OrderStatus.RECEIVED,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val loyaltyDiscount: Double = 0.0,
    val driverTip: Double = 0.0,
    val serviceFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val deliverySlotType: DeliverySlotType = DeliverySlotType.IMMEDIATE_ROLLING,
    val deliverySlotLabel: String = "",
    val deliveryAddress: DeliveryAddress? = null,
    val paymentReferenceToken: String? = null,
    val placementTime: Long = 0L,
    val scheduledTime: Long? = null,
    val deliveredTime: Long? = null,
    /** Compared against the driver's proof-of-delivery submission — see confirmProofOfDelivery
     * in FirestoreRepository. Generated at checkout, never shown to the driver ahead of time. */
    val deliveryOtp: String? = null,
    val deliveryQrToken: String? = null,
    /** True once store staff/admin visually match the driver's dispatch code against the
     * physical order at the counter — gates "Mark as collected" in DriverActiveRunScreen. */
    val dispatchConfirmedByAdmin: Boolean = false
)

/** Realtime Database node: driverTelemetry/{driverId} — high-frequency GPS stream. */
data class DriverTelemetry(
    val driverId: String = "",
    val orderId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAtEpochMs: Long = 0L
)

/** Realtime Database node: substitutionChats/{orderId}/messages/{messageId}. */
data class SubstitutionMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderRole: SubstitutionSenderRole = SubstitutionSenderRole.SYSTEM,
    val timestamp: Long = 0L,
    val payloadText: String = "",
    val proposedProductId: String? = null,
    val isApproved: Boolean? = null
)
