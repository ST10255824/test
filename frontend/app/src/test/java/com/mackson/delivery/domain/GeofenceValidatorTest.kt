package com.mackson.delivery.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeofenceValidatorTest {

    // Sandton (store) vs Rosebank (customer) — roughly 6.5km apart in reality.
    private val storeLat = -26.1076
    private val storeLng = 28.0567
    private val nearbyLat = -26.1467 // Rosebank, ~5km away
    private val nearbyLng = 28.0436
    private val farLat = -25.7461 // Pretoria CBD, ~40km away
    private val farLng = 28.1881

    @Test
    fun `address within radius is accepted`() {
        val result = GeofenceValidator.isWithinServiceRadius(
            nearbyLat, nearbyLng, storeLat, storeLng, serviceRadiusKm = 10.0
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `address outside radius is rejected`() {
        val result = GeofenceValidator.isWithinServiceRadius(
            farLat, farLng, storeLat, storeLng, serviceRadiusKm = 5.0
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `distance to self is zero`() {
        val distance = GeofenceValidator.haversineKm(storeLat, storeLng, storeLat, storeLng)
        assertThat(distance).isWithin(0.0001).of(0.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeofenceValidator.haversineKm(storeLat, storeLng, nearbyLat, nearbyLng)
        val b = GeofenceValidator.haversineKm(nearbyLat, nearbyLng, storeLat, storeLng)
        assertThat(a).isWithin(0.0001).of(b)
    }
}
