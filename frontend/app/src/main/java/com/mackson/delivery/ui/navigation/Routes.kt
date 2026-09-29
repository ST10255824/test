package com.mackson.delivery.ui.navigation

/** Central route table. Kept as plain string constants (with a couple of arg helpers) so the
 * NavHost stays simple to read across three role-based navigation graphs (Customer, Picker,
 * Driver) — Store Manager/Admin is served by the separate admin-web/ console instead. */
object Routes {
    // Auth
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    // A query-style optional argument, not a path segment — the login screen always links here
    // with an empty phone (before the user has typed one), and an empty *path* segment isn't a
    // valid match against "{phone}" in Navigation Compose (it throws instead of falling back to
    // the argument's defaultValue), which crashed the app on every tap of "Sign in with phone".
    const val OTP = "auth/otp?phone={phone}"
    fun otp(phone: String) = "auth/otp?phone=$phone"

    // Customer
    const val CUSTOMER_HOME = "customer/home"
    const val CUSTOMER_BROWSE = "customer/browse/{storeId}"
    fun customerBrowse(storeId: String) = "customer/browse/$storeId"
    const val CUSTOMER_SEARCH = "customer/search/{storeId}"
    fun customerSearch(storeId: String) = "customer/search/$storeId"
    const val CUSTOMER_PRODUCT = "customer/product/{storeId}/{productId}"
    fun customerProduct(storeId: String, productId: String) = "customer/product/$storeId/$productId"
    const val CUSTOMER_CART = "customer/cart"
    const val CUSTOMER_CHECKOUT = "customer/checkout"
    const val CUSTOMER_TRACKING = "customer/tracking/{orderId}"
    fun customerTracking(orderId: String) = "customer/tracking/$orderId"
    const val CUSTOMER_CHAT = "customer/chat/{orderId}"
    fun customerChat(orderId: String) = "customer/chat/$orderId"
    const val CUSTOMER_HISTORY = "customer/history"
    const val CUSTOMER_PROFILE = "customer/profile"

    // Picker
    const val PICKER_MANIFEST = "picker/manifest"
    const val PICKER_ORDER_DETAIL = "picker/order/{orderId}"
    fun pickerOrderDetail(orderId: String) = "picker/order/$orderId"
    const val PICKER_SCAN = "picker/scan/{orderId}/{mode}/{targetProductId}"
    fun pickerScanForPicking(orderId: String, productId: String) = "picker/scan/$orderId/PICK/$productId"
    fun pickerScanForSubstitution(orderId: String, productId: String) = "picker/scan/$orderId/SUB/$productId"

    // Driver
    const val DRIVER_JOBS = "driver/jobs"
    const val DRIVER_ACTIVE_RUN = "driver/run/{orderId}"
    fun driverActiveRun(orderId: String) = "driver/run/$orderId"
    const val DRIVER_PROOF = "driver/proof/{orderId}"
    fun driverProof(orderId: String) = "driver/proof/$orderId"
}
