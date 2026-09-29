package com.mackson.delivery.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Validates a delivery address against a store's service radius — Part 1 US-02. */
object GeofenceValidator {

    private const val EARTH_RADIUS_KM = 6371.0

    fun isWithinServiceRadius(
        addressLat: Double,
        addressLng: Double,
        storeLat: Double,
        storeLng: Double,
        serviceRadiusKm: Double
    ): Boolean = haversineKm(addressLat, addressLng, storeLat, storeLng) <= serviceRadiusKm

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}
