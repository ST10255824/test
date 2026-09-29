package com.mackson.delivery.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Free address search/geocoding via OpenStreetMap's Nominatim API — used instead of Google
 * Places Autocomplete, which needs a Google Cloud billing account (a card on file) the same way
 * Cloud Functions need Blaze. Nominatim is free but rate-limited to ~1 request/second and
 * requires a real User-Agent identifying the app (see their usage policy), which is why
 * AddressSearchScreen debounces keystrokes before calling this rather than searching on every
 * character typed.
 */
object AddressSearchService {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/search"
    private const val USER_AGENT = "MacksonsDeliveryApp/1.0 (INSY7315 WIL student project)"

    // Estcourt, KwaZulu-Natal — soft-biases results toward the store's own area without
    // excluding matches elsewhere in South Africa (bounded=0 below).
    private const val STORE_LAT = -29.0050
    private const val STORE_LNG = 29.8680
    private const val BIAS_DEGREES = 0.6

    private val client = OkHttpClient()

    suspend fun search(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("addressdetails", "0")
            .addQueryParameter("limit", "6")
            .addQueryParameter("countrycodes", "za")
            .addQueryParameter(
                "viewbox",
                "${STORE_LNG - BIAS_DEGREES},${STORE_LAT + BIAS_DEGREES},${STORE_LNG + BIAS_DEGREES},${STORE_LAT - BIAS_DEGREES}"
            )
            .addQueryParameter("bounded", "0")
            .build()

        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(body)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AddressSuggestion(
                    displayName = obj.getString("display_name"),
                    latitude = obj.getString("lat").toDouble(),
                    longitude = obj.getString("lon").toDouble()
                )
            }
        }
    }
}
