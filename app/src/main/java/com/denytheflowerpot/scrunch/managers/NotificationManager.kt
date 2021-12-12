package com.denytheflowerpot.scrunch.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.denytheflowerpot.scrunch.R
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.activities.MainActivity
import com.denytheflowerpot.scrunch.services.FoldActionSignalingService

class NotificationManager(private val context: Context) {
    private val notificationChannelId = "scrunch_notifications"
    private val channel: NotificationChannel by lazy {
        val c = NotificationChannel(notificationChannelId, context.getString(R.string.notif_channel_label), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(c)
        return@lazy c
    }

    fun generateNotification(stopAction: String): Notification? {
        (context as? ScrunchApplication)?.getServiceIntent(false)?.let {
            val informMainActivityIntent = Intent(context, MainActivity::class.java).putExtra(FoldActionSignalingService.stopServiceAction, true)
            val stopServicePendingIntent = PendingIntent.getActivity(context, 0, informMainActivityIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
            return NotificationCompat.Builder(context, channel.id)
                .setContentTitle(context.getText(R.string.notif_title))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getText(R.string.notif_content)))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(NotificationCompat.Action.Builder(R.drawable.stop, context.getText(R.string.btn_stop), stopServicePendingIntent).build())
                .build()
        } ?: return null
    }
}