package io.ipoli.android.common.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.app.NotificationCompat
import io.ipoli.android.R

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 05/18/2018.
 */
object NotificationUtil {

    @Suppress("DEPRECATION")
    fun createDefaultNotification(
        context: Context,
        title: String,
        icon: Bitmap,
        message: String,
        sound: Uri,
        channelId: String,
        contentIntent: PendingIntent
    ): Notification =
        NotificationCompat.Builder(
            context,
            channelId
        ).setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(icon)
            .setSound(sound)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

}