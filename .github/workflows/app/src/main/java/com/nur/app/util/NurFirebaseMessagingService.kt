package com.nur.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nur.app.NurApplication
import com.nur.app.R
import com.nur.app.data.repository.FirebaseRepository
import com.nur.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NurFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch { FirebaseRepository.updateFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title    = message.notification?.title ?: message.data["title"] ?: return
        val body     = message.notification?.body  ?: message.data["body"]  ?: return
        val postId   = message.data["postId"]
        val postType = message.data["postType"]

        val channel = when (postType) {
            "PRAYER"     -> NurApplication.CHANNEL_PRAYER
            "REFLECTION" -> NurApplication.CHANNEL_REFLECTION
            else         -> NurApplication.CHANNEL_GUIDANCE
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            postId?.let { putExtra("postId", it) }
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notif)
    }
}
