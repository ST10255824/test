package com.mackson.delivery.data.model

/** Matches the RBAC role claims issued by Firebase Auth custom claims. */
enum class UserRole {
    CUSTOMER, PICKER, DRIVER, MANAGER, ADMIN;

    companion object {
        fun fromString(value: String?): UserRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOMER
    }
}

/** Order lifecycle states — mirrors the Firestore state diagram in Part 1, section 9.3.10. */
enum class OrderStatus {
    RECEIVED,
    PICKING,
    READY_FOR_COLLECTION,
    EN_ROUTE,
    ARRIVED_AT_NODE,
    DELIVERED,
    CANCELLED;

    fun nextForPicker(): OrderStatus? = when (this) {
        RECEIVED -> PICKING
        PICKING -> READY_FOR_COLLECTION
        else -> null
    }

    fun nextForDriver(): OrderStatus? = when (this) {
        READY_FOR_COLLECTION -> EN_ROUTE
        EN_ROUTE -> ARRIVED_AT_NODE
        ARRIVED_AT_NODE -> DELIVERED
        else -> null
    }
}

enum class DeliverySlotType { IMMEDIATE_ROLLING, SCHEDULED_HOURLY }

enum class SubstitutionSenderRole { PICKER, CUSTOMER, SYSTEM }

enum class ProofOfDeliveryMethod { OTP, QR_CODE, PHOTO }
