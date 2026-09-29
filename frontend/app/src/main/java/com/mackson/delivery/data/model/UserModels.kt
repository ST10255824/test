package com.mackson.delivery.data.model

/**
 * Customer — end user buying groceries. See Part 1 domain class diagram (9.3.9).
 * Firestore collection: users/{uid} where role == CUSTOMER.
 */
data class Customer(
    val customerId: String = "",
    val name: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val role: String = "CUSTOMER",
    val savedAddresses: List<DeliveryAddress> = emptyList(),
    val defaultAddressId: String? = null,
    val biometricEnabled: Boolean = false,
    val loyaltyCardNumber: String? = null,
    val loyaltyPoints: Int = 0,
    val fcmToken: String? = null
)

data class DeliveryAddress(
    val addressId: String = "",
    val label: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val formattedAddress: String = "",
    val withinServiceRadius: Boolean = true
)

/** Store branch manager — Part 1: "Store Manager" domain class. */
data class StoreManagerProfile(
    val managerId: String = "",
    val managerName: String = "",
    val assignedStoreId: String = "",
    val shiftSchedule: String = "",
    val branchAccessLevel: String = "STANDARD"
)

/** Platform-wide administrator — Part 1: "System Manager" domain class. */
data class SystemAdminProfile(
    val adminId: String = "",
    val adminName: String = "",
    val accessRole: String = "ADMIN",
    val auditLogPermissions: Boolean = true
)

/** In-store picker / personal shopper — Part 1: "Personal Shopper" domain class. */
data class PickerProfile(
    val shopperId: String = "",
    val staffName: String = "",
    val assignedStoreId: String = "",
    val dutyStatus: String = "OFF_DUTY" // ON_DUTY | OFF_DUTY | ON_BREAK
)

/** Last-mile courier — Part 1: "Delivery Driver" domain class. */
data class DriverProfile(
    val driverId: String = "",
    val driverName: String = "",
    val vehicleType: String = "",
    /** Shown on the customer's tracking screen for verification at the door — WIL group
     * requirement: "gets the license plate of the vehicle...and as well as the name and
     * picture for verification". */
    val licensePlate: String = "",
    val photoUrl: String = "",
    val currentPayoutBalance: Double = 0.0,
    val liveGpsLat: Double? = null,
    val liveGpsLng: Double? = null,
    val isAvailable: Boolean = false
)
