package com.mackson.delivery.data.model

/** A physical supermarket branch / fulfilment centre — Firestore collection: storeNodes/{storeId}. */
data class StoreNode(
    val storeId: String = "",
    val branchName: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val serviceRadiusKm: Double = 5.0,
    val isAcceptingOrders: Boolean = true
)

/** A catalogue item scoped to a store node — Firestore collection: storeNodes/{storeId}/products/{productId}. */
data class Product(
    val productId: String = "",
    val storeId: String = "",
    val name: String = "",
    val description: String = "",
    val unitPrice: Double = 0.0,
    val weightGrams: Int = 0,
    val category: String = "",
    val aisleNumber: Int = 1,
    val imageUrl: String = "",
    val barcode: String = "",
    val currentStockLevel: Int = 0,
    val isAgeRestricted: Boolean = false
) {
    val inStock: Boolean get() = currentStockLevel > 0
}

/** A saved reorder list — Firestore collection: users/{uid}/shoppingLists/{listId}. */
data class ShoppingList(
    val listId: String = "",
    val name: String = "",
    val productIds: List<String> = emptyList()
)
