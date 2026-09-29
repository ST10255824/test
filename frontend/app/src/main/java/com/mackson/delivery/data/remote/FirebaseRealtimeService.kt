package com.mackson.delivery.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.mackson.delivery.data.model.DriverTelemetry
import com.mackson.delivery.data.model.SubstitutionMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages high-velocity WebSocket streaming jobs via the Firebase Realtime Database
 * (Part 1, section 9.3.9: "FirebaseRealtimeService"). Firestore handles durable records;
 * this service handles the two low-latency, high-frequency streams called out in Part 1
 * section 2.3: live driver GPS telemetry and the picker/customer substitution chat.
 *
 * Implemented as a Kotlin `object` (Singleton pattern) so the app holds exactly one
 * Realtime Database connection for its whole lifetime.
 */
object FirebaseRealtimeService {

    private val realtimeDb: FirebaseDatabase get() = FirebaseDatabase.getInstance()

    private const val NODE_TELEMETRY = "driverTelemetry"
    private const val NODE_CHATS = "substitutionChats"
    private const val NODE_CHAT_PARTICIPANTS = "chatParticipants"
    private const val NODE_RUN_ASSIGNMENTS = "runAssignments"

    /** Client-side stand-in for what checkoutOrder / onOrderCreate wrote server-side — see
     * FirestoreRepository.checkoutOrder. Grants the customer and (if one was assigned) the
     * picker read access to this order's substitution chat under database.rules.json. */
    suspend fun setChatParticipants(orderId: String, customerId: String, shopperId: String?) {
        val payload = mutableMapOf<String, Any?>("customerId" to customerId)
        if (shopperId != null) payload["shopperId"] = shopperId
        realtimeDb.getReference(NODE_CHAT_PARTICIPANTS).child(orderId).setValue(payload).await()
    }

    /** Client-side stand-in for what acceptDeliveryRun wrote server-side — see
     * FirestoreRepository.acceptDeliveryRun. Grants the customer read access to this driver's
     * live GPS telemetry stream for the duration of the run. */
    suspend fun setRunAssignment(driverId: String, orderId: String, customerId: String) {
        realtimeDb.getReference(NODE_RUN_ASSIGNMENTS).child(driverId)
            .setValue(mapOf("customerId" to customerId, "orderId" to orderId))
            .await()
    }

    /** Driver app pushes a GPS ping roughly every 5 seconds — Part 1 non-functional requirement. */
    suspend fun streamDriverLocation(driverId: String, orderId: String, lat: Double, lng: Double) {
        val payload = DriverTelemetry(
            driverId = driverId,
            orderId = orderId,
            latitude = lat,
            longitude = lng,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        realtimeDb.getReference(NODE_TELEMETRY).child(driverId).setValue(payload).await()
    }

    /** Customer tracking screen subscribes to this to redraw the map marker — Observer pattern. */
    fun observeDriverLocation(driverId: String): Flow<DriverTelemetry?> = callbackFlow {
        val ref = realtimeDb.getReference(NODE_TELEMETRY).child(driverId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(DriverTelemetry::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Picker proposes a substitute, or the customer approves/declines it — Part 1 US-14/US-17. */
    suspend fun sendLiveChatMessage(orderId: String, message: SubstitutionMessage) {
        val ref = realtimeDb.getReference(NODE_CHATS).child(orderId).child("messages").push()
        realtimeDb.getReference(NODE_CHATS).child(orderId).child("messages")
            .child(ref.key ?: return)
            .setValue(message.copy(messageId = ref.key.orEmpty()))
            .await()
    }

    /** Customer approves/declines a picker's proposed substitute — Part 1 US-14. Previously the
     * respondToSubstitution Cloud Function's job; see FirestoreRepository.respondToSubstitution
     * for the companion Firestore-side item update (also now client-side). */
    suspend fun setSubstitutionApproval(orderId: String, messageId: String, approved: Boolean) {
        realtimeDb.getReference(NODE_CHATS).child(orderId).child("messages").child(messageId)
            .child("isApproved").setValue(approved).await()
    }

    fun observeSubstitutionChat(orderId: String): Flow<List<SubstitutionMessage>> = callbackFlow {
        val ref = realtimeDb.getReference(NODE_CHATS).child(orderId).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(SubstitutionMessage::class.java) }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
