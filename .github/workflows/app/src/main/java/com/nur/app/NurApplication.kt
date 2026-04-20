package com.nur.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp

class NurApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_PRAYER,     "Daily Prayers",   NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_REFLECTION, "Reflections",     NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_GUIDANCE,   "Guidance",        NotificationManager.IMPORTANCE_DEFAULT)
            ))
        }
    }

    companion object {
        const val CHANNEL_PRAYER     = "nur_prayer"
        const val CHANNEL_REFLECTION = "nur_reflection"
        const val CHANNEL_GUIDANCE   = "nur_guidance"
    }
}
