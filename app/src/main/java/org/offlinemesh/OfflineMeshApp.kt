package org.offlinemesh

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class OfflineMeshApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                getString(R.string.call_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.call_notification_channel_desc)
                setShowBadge(true)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(callChannel)
        }
    }

    companion object {
        const val CHANNEL_CALLS = "offlinemesh_calls_channel"
    }
}
