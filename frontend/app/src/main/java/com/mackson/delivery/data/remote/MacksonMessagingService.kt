package com.mackson.delivery.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mackson.delivery.R

/**
 * Receives status-change and driver-arrival push notifications sent by
 * functions/src/notifications.ts via Firebase Cloud Messaging (Part 1, section 9.3.9:
 * "Asynchronous push messages about status changes and driver arrivals").
 */
class MacksonMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuthManager.getCurrentUserId() ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "Mackson's Delivery"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "order_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Order updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
