package com.mackson.delivery.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeliverySlotPlannerTest {

    @Test
    fun `full slot cannot be booked`() {
        val slot = DeliverySlot("17:00-18:00", capacity = 20, bookedCount = 20)
        assertThat(DeliverySlotPlanner.canBook(slot)).isFalse()
    }

    @Test
    fun `slot with remaining capacity can be booked`() {
        val slot = DeliverySlot("17:00-18:00", capacity = 20, bookedCount = 19)
        assertThat(DeliverySlotPlanner.canBook(slot)).isTrue()
        assertThat(slot.remaining).isEqualTo(1)
    }

    @Test
    fun `availableSlots filters out full slots`() {
        val slots = listOf(
            DeliverySlot("A", capacity = 5, bookedCount = 5),
            DeliverySlot("B", capacity = 5, bookedCount = 2)
        )
        val available = DeliverySlotPlanner.availableSlots(slots)
        assertThat(available).hasSize(1)
        assertThat(available.first().label).isEqualTo("B")
    }
}
