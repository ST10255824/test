package com.mackson.delivery.domain

/** Represents one hourly logistics window and how much capacity remains — Part 1 US-09. */
data class DeliverySlot(
    val label: String,
    val capacity: Int,
    val bookedCount: Int
) {
    val isFull: Boolean get() = bookedCount >= capacity
    val remaining: Int get() = (capacity - bookedCount).coerceAtLeast(0)
}

object DeliverySlotPlanner {
    fun availableSlots(slots: List<DeliverySlot>): List<DeliverySlot> = slots.filterNot { it.isFull }

    fun canBook(slot: DeliverySlot): Boolean = !slot.isFull
}
